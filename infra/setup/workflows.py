
import os
import sys
import time
import json
from datetime import datetime
from threading import Thread

class SimpleTests:
    def __init__(self):
        config = json.load(open("../config/config.json"))
        self.devices = config["devices"]
        self.networks = config["networks"]
        for d in self.devices:
            os.system("docker exec -i {} javac src/client/FileClient.java".format(d))

        self.workflow1()
        self.workflow2()

    """
    File created by a client in cluster1 is accessible by a client in cluster2
    """
    def workflow1(self):

        commands = [
            "docker exec -i C2.1 ls src/client/*.txt"
            
            # C1.1 write 5 files to it's master1
            "docker exec -i C1.1 touch src/client/c1.1_file1.txt",
            "docker exec -i C1.1 touch src/client/c1.1_file2.txt",
            "docker exec -i C1.1 touch src/client/c1.1_file3.txt",
            "docker exec -i C1.1 touch src/client/c1.1_file4.txt",
            "docker exec -i C1.1 touch src/client/c1.1_file5.txt",

            #Send the 5 files to it's cluster master
            "docker exec -i C1.1 java src/client/FileClient snd c1.1_file1.txt",
            "docker exec -i C1.1 java src/client/FileClient snd c1.1_file2.txt",
            "docker exec -i C1.1 java src/client/FileClient snd c1.1_file3.txt",
            "docker exec -i C1.1 java src/client/FileClient snd c1.1_file4.txt",
            "docker exec -i C1.1 java src/client/FileClient snd c1.1_file5.txt",

            #Another cluster client request and recieves files
            "docker exec -i C2.1 java src/client/FileClient rcv c1.1_file1.txt",
            "docker exec -i C2.1 java src/client/FileClient rcv c1.1_file2.txt",
            "docker exec -i C2.1 java src/client/FileClient rcv c1.1_file3.txt",
            "docker exec -i C2.1 java src/client/FileClient rcv c1.1_file4.txt",
            "docker exec -i C2.1 java src/client/FileClient rcv c1.1_file5.txt",


            #Verify if the file rcvd successfully.
            "docker exec -i C2.1 ls src/client/*.txt"
        ]

        for command in commands:
            os.system(command)

    """
    All clusters are interconnected. All create one file each. All can access each other's files.
    """
    def workflow2(self):
        # All clients create and send a file to the master
        for d in self.devices:
            print("{0} is writing file {0}_file.txt".format(d))
            os.system("docker exec -i {} touch src/client/{}_file.txt".format(d))
            os.system("docker exec -i {} java src/client/FileClient snd {}_file.txt".format(d))

        # All clients request each other's files
        for d in self.devices:
            os.system("docker exec -i {} ls src/client/*.txt")
            print("{} is requesting files".format(d))
            for fileName in self.devices:
                print("\t requesting {}_file.txt".format(fileName))
                os.system("docker exec -i {} java src/client/FileClient rcv {}_file.txt".format(d, fileName))
            print("\t ** LS OUTPUT **")
            os.system("docker exec -i {} ls src/client/*.txt")



SimpleTests()











