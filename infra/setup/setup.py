import os
import sys
import time
import json
from datetime import datetime
from threading import Thread

imageName = "shrey67/node_image"
deviceCode = {"masters": 0 , "slaves": 1, "clients": 2}
envVariable = {"public":"PUB", "private":"PVT", "global":"GLOBAL"}

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
        ]
        for command in commands:
            os.system(command)
        return


class Network:
    def __init__(self, network, networkDict):
        self.networkDict = networkDict
        self.network = network
        self.networkType = networkDict["type"]
    
    def createNetwork(self):
        network_address = self.networkDict["network_ip"]
        print("Creating network {} with network address {}".format(self.network, network_address))
        command = "docker network create -d bridge --ip-range={0} --subnet={0} {1}".format(network_address, self.network)
        os.system(command)
        print("\n")

    def makeConnections(self):
        netJSON = {}
        devices = self.networkDict["devices"]
        network_address = self.networkDict["network_ip"]
        for d in devices:
            print("Connecting {} to {}".format(d, self.network))
            command = "docker network connect {0} {1}".format(self.network,d)
            os.system(command)

            #Extract the ip and eth number for the device d
            networkPrefix = ".".join(network_address.split(".")[0:-1])
            command = "docker exec -i {0} ip a | grep {1} | awk '{{print $2}} {{print $7}}'".format(d, networkPrefix)
            result = os.popen(command).read().split("\n")[:-1]
            print ("ip address:{}, eth:{}".format(result[0], result[1]))
            netJSON[d] = {"ip":result[0], "eth":result[1]}
            self.defaultSetting(d)
        return netJSON
    
    def defaultSetting(self, d):
        bandwidth_mbps = self.networkDict["bandwidth_mbps"]
        latency_ms = self.networkDict["latency_ms"]
        print("Setting bw to {} & latency to {}".format(bandwidth_mbps, latency_ms))
            

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