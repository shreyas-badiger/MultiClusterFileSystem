import os
import sys
import time
import json
from datetime import datetime
from threading import Thread

imageName = "shrey67/node_image"
deviceCode = {"masters": 0 , "slaves": 1, "clients": 2}
envVariable = {"public":"PUB", "private":"PVT", "global":"GLOBAL"}
tmpDir = "../../tmp"

def tarMyCode():
    os.system("rm -rf code")
    os.system("mkdir code")
    os.system("cp ../../src/client/* code/")
    os.system("cp ../../src/master/* code/")
    os.system("cp ../../src/mutex_server/* code/")
    os.system("tar -zcf code.tar.gz -C code .")

class Device:
    def __init__(self, devicesList, deviceType):
        self.devicesList = devicesList
        self.numberOfDevices = len(devicesList)
        self.deviceType = deviceCode[deviceType]
    
    def createDevices(self):
        for d in self.devicesList:
            print("Creating device {}".format(d))
            command = "docker run -d --hostname {0} --name {0} {1}".format(d, imageName)
            os.system(command)
            self.defaultSetting(d)
    
    def defaultSetting(self, d):
        commands = [
            "docker cp code.tar.gz {}:/".format(d),
            "docker exec -i {} tar -zxvf code.tar.gz".format(d),
        ]
        for command in commands:
            os.popen(command).read()
        return


class Network:
    def __init__(self, network, networkDict):
        self.networkDict = networkDict
        self.network = network
        self.networkType = networkDict["type"]
        self.master = networkDict["master"]
        self.master_ip = ""
    
    def createNetwork(self):
        network_address = self.networkDict["network_ip"]
        print("Creating network {} with network address {}".format(self.network, network_address))
        command = "docker network create -d bridge --ip-range={0} --subnet={0} {1}".format(network_address, self.network)
        os.system(command)
        print("\n")


    def getIp(self, networkPrefix, d):
        command = "docker exec -i {0} ip a | grep {1} | awk '{{print $2}} {{print $7}}'".format(d, networkPrefix)
        result = os.popen(command).read().split("\n")[:-1]
        result[0] = result[0].split("/")[0]
        print ("ip address:{}, eth:{}".format(result[0], result[1]))
        return result

    def makeConnections(self):
        netJSON = {}
        devices = self.networkDict["devices"]
        network_address = self.networkDict["network_ip"]
        networkPrefix = ".".join(network_address.split(".")[0:-1])
        clientIPs = {}

        #Connect the master
        print("Connecting {} to {}".format(self.master, self.network))
        command = "docker network connect {0} {1}".format(self.network, self.master)
        os.system(command)
        result = self.getIp(networkPrefix, self.master)
        self.master_ip = result[0]
        netJSON[self.master] = {"ip":result[0], "eth":result[1]}
        self.defaultSetting(self.master)

        #Connect others
        for d in devices:
            print("Connecting {} to {}".format(d, self.network))
            command = "docker network connect {0} {1}".format(self.network, d)
            os.system(command)

            #Extract the ip and eth number for the device d
            result = self.getIp(networkPrefix, d)
            netJSON[d] = {"ip":result[0], "eth":result[1]}
            clientIPs[d] = result[0]
            self.defaultSetting(d)

            #Put ip (IP address of it's master) file to the container
            os.system("echo {} > {}/ip".format(self.master_ip, tmpDir))
            os.system("docker cp {}/ip {}:/".format(tmpDir, d))


        with open('{}/clientIPs.json'.format(tmpDir),'w') as file:
            file.write(json.dumps(clientIPs))
        os.system("docker cp {}/clientIPs.json {}:/".format(tmpDir, self.master))

        return netJSON
    
    def defaultSetting(self, d):
        bandwidth_mbps = self.networkDict["bandwidth_mbps"]
        latency_ms = self.networkDict["latency_ms"]
        print("Setting bw to {} & latency to {}".format(bandwidth_mbps, latency_ms))
        # commands = [
        #     "sudo docker exec -i {0} tc qdisc add dev {1} handle 1: root htb default 11".format(d, eth_ip_dict[gw][private_network[i]]["eth"]),
        #     "sudo docker exec -i {0} tc class add dev {1} parent 1: classid 1:1 htb rate {2}Mbps".format(gw, eth_ip_dict[gw][private_network[i]]["eth"], private_networks_dict[private_network[i]]["bandwidth_mbps"]),
        #     "sudo docker exec -i {0} tc class add dev {1} parent 1:1 classid 1:11 htb rate {2}Mbit".format(gw, eth_ip_dict[gw][private_network[i]]["eth"], private_networks_dict[private_network[i]]["bandwidth_mbps"]),
        #     "sudo docker exec -i {0} tc qdisc add dev {1} parent 1:11 handle 10: netem delay {2}ms".format(gw, eth_ip_dict[gw][private_network[i]]["eth"], float( private_networks_dict[private_network[i]]["latency_ms"]))
        # ]
            

class HDFS:
    def __init__(self, master, slaves):
        self.master = master
        self.slaves = slaves

    def setConfigFiles(self):
        return

    def startHDFS(self):
        return


startTime = datetime.now()
config = json.load(open("../config/config.json"))
devices = config["devices"]
networks = config["networks"]
ipJSON = {}

if len(sys.argv) == 2 and sys.argv[1] == "-d":
    #Delete the setup
    print ("\nRemoving all the containers")
    command = "docker rm --force $(docker ps -a -q)"
    os.system(command)
    print ("\n\nRemoving the networks")
    for n in networks:
        command = "docker network rm {}".format(n)
        os.system(command)
else:
    tarMyCode()
    # if(input("\nPlease tar the source code (tar -zcvf tmp/src.tar.gz src)\n  Press 0 to exit.\n  Press 1 to continue\n\n") == 0):
    #     sys.exit()
    #Create Devices
    print("\n\n\t\tCREATING DEVICES")
    print("\t\t----------------\n")
    for deviceType in devices:
        print("\n*******************************************************")
        print("\t\tCreating {}".format(deviceType))
        print("*******************************************************")
        obj = Device(devices[deviceType], deviceType)
        obj.createDevices()

    #Create Networks
    print("\n\n\n\t\tCREATING NETWORKS")
    print("\t\t-----------------\n")
    for n in networks:
        print("\n*******************************************************")
        print("\tCreating network {}".format(n))
        print("*******************************************************")
        obj = Network(n, networks[n])
        obj.createNetwork()
        ipJSON[n] = obj.makeConnections()
        network_address = networks[n]["network_ip"]
        ipJSON["broadcast_ip"] = networkPrefix = ".".join(network_address.split(".")[0:-1])+".255"
    
    with open('../output/ip.json','w') as file:
        file.write(json.dumps(ipJSON))