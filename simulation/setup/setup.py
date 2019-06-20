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
            command = "docker run -d --hostname {0} --name {0} --cap-add=NET_ADMIN {1}".format(d, imageName)
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
        self.netJSON = {}
    
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
        self.netJSON[self.master] = {"ip":result[0], "eth":result[1]}
        self.defaultSetting(self.master)

        #Connect others
        for d in devices:
            print("Connecting {} to {}".format(d, self.network))
            command = "docker network connect {0} {1}".format(self.network, d)
            os.system(command)

            #Extract the ip and eth number for the device d
            result = self.getIp(networkPrefix, d)
            self.netJSON[d] = {"ip":result[0], "eth":result[1]}
            clientIPs[d] = result[0]
            self.defaultSetting(d)

            #Put ip (IP address of it's master) file to the container
            os.system("echo {} > {}/ip".format(self.master_ip, tmpDir))
            os.system("docker cp {}/ip {}:/".format(tmpDir, d))


        #This block is responsible to setup HDFS
        if(self.networkDict["type"] == "private" and len(sys.argv) == 2 and sys.argv[1]=="-hdfs"):
            print("\n\n\t***Setting up HDFS cluster***\n")
            commands = []
            #On master, gen ssh keys and add it to all the slaves for password-less access.
            print("\nsetup SSH keys")
            commands.append("docker exec -i {} ssh-keygen -b 4096 -f /root/.ssh/id_rsa -t rsa -N ''".format(self.master))
            commands.append("echo root > password")
            commands.append("docker cp password {}:/".format(self.master))
            commands.append("docker exec -i {} apt-get install sshpass".format(self.master))
            commands.append("docker cp ../resources/config {0}:/root/.ssh/config".format(self.master))
            commands.append("docker exec -i {} chmod 600 /root/.ssh/config".format(self.master))
            commands.append("docker exec -i {} chown root /root/.ssh/config".format(self.master))
            commands.append("docker exec -i {} sshpass -f password ssh-copy-id -o StrictHostKeychecking=no -i /root/.ssh/id_rsa.pub root@{}".format(self.master, self.master_ip))
            for c in clientIPs.keys():
                commands.append("docker exec -i {} sshpass -f password ssh-copy-id -o StrictHostKeychecking=no -i /root/.ssh/id_rsa.pub root@{}".format(self.master, clientIPs[c]))
            for command in commands:
                os.popen(command).read()

            #Copy respective core-site.xml to the cluster
            commands = []
            print("\nadd core-site.xml")
            commands.append("docker cp ../resources/{0} {0}:/root/hadoop/etc/hadoop/core-site.xml".format(self.master))
            commands.append("echo \"{} {}\" > hostsEntry".format(self.master_ip, self.master))
            commands.append("docker cp ../resources/hostsEntry.sh {}:/".format(self.master))
            commands.append("docker cp hostsEntry {}:/".format(self.master))
            commands.append("docker exec -i {} ./hostsEntry.sh".format(self.master))
            for d in devices:
                commands.append("docker cp ../resources/{} {}:/root/hadoop/etc/hadoop/core-site.xml".format(self.master, d))
                commands.append("docker cp ../resources/hostsEntry.sh {}:/".format(d))
                commands.append("docker cp hostsEntry {}:/".format(d))
                commands.append("docker exec -i {} ./hostsEntry.sh".format(d))
            for command in commands:
                os.popen(command).read()

            #Replace HDFS slaves file
            commands = []
            print("\nadd slaves file on master")
            commands.append("touch slaves")
            for c in clientIPs.keys():
                commands.append("echo {} >> slaves".format(clientIPs[c]))
            commands.append("docker cp slaves {}:/root/hadoop/etc/hadoop/slaves".format(self.master))

            ##Format HDFS
            print("\nreformat HDFS")
            commands.append("docker exec -i {} /root/hadoop/sbin/start-dfs.sh".format(self.master))

            print("\nstart HDFS")
            commands.append("docker exec -i {} /root/hadoop/bin/hdfs namenode -format".format(self.master))

            for command in commands:
                os.popen(command).read()


            #Delete all temp files
            os.system("rm -rf hostsEntry")
            os.system("rm -rf slaves")
            os.system("rm -rf password")

        return self.netJSON
    
    def defaultSetting(self, d):
        bandwidth_mbps = self.networkDict["bandwidth_mbps"]
        latency_ms = self.networkDict["latency_ms"]
        print("Setting bw to {} & latency to {}".format(bandwidth_mbps, latency_ms))
        commands = [
            "sudo docker exec -i {0} tc qdisc add dev {1} handle 1: root htb default 11".format(d, self.netJSON[d]["eth"]),
            "sudo docker exec -i {0} tc class add dev {1} parent 1: classid 1:1 htb rate {2}Mbps".format(d, self.netJSON[d]["eth"], bandwidth_mbps),
            "sudo docker exec -i {0} tc class add dev {1} parent 1:1 classid 1:11 htb rate {2}Mbit".format(d, self.netJSON[d]["eth"], bandwidth_mbps),
            "sudo docker exec -i {0} tc qdisc add dev {1} parent 1:11 handle 10: netem delay {2}ms".format(d, self.netJSON[d]["eth"], latency_ms)
        ]
        for command in commands:
            os.system(command)


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

    os.system("rm -rf code*")
    
    with open('../output/ip.json','w') as file:
        file.write(json.dumps(ipJSON))
endTime = datetime.now()
print("Time taken: {}\n\n\n".format(endTime - startTime))