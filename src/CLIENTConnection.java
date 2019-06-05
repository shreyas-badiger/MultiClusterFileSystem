import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.logging.Logger;
import java.util.logging.Level;


public class CLIENTConnection implements Runnable {

    private Socket clientSocket; // to connect to the client
    private static Socket sock1; //to connect to Mutex server
    private BufferedReader in = null;
    private static BufferedReader in2;
    private static PrintStream os;
    private static PrintStream os1;

    public CLIENTConnection(Socket client) {
        this.clientSocket = client;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String clientSelection;
            while ((clientSelection = in.readLine()) != null) {
                switch (clientSelection) {
                    case "1":
                        receiveFile();
                        break;
                    case "2":
                        String outGoingFileName;
                        String accessMode;
                        while ((accessMode = in.readLine()) != null){
                            // System.err.print(accessMode);
                            switch(accessMode){
                                case "Read":
                                    while((outGoingFileName = in.readLine()) != null){
                                        System.err.print("Preparing to send file...");
                                        sendFile(outGoingFileName);
                                        break;
                                    }
                                case "Write":
                                    os = new PrintStream(clientSocket.getOutputStream()); // to write to client

                                    while((outGoingFileName = in.readLine()) != null){
                                        // check mutex and allow access

                                        //if the file is there in the master's blocked list, return there only
                                        if(FileServer.files_blocked.contains(outGoingFileName)) {
                                            // send exception to client
                                            os.println("File is locked, try again later !!");
                                            break;
                                        }

                                        int flag = 0;

                                        try {
                                            //opening socket to mutex server
                                            sock1 = new Socket("localhost", 4445);
                                            in2 = new BufferedReader(new InputStreamReader(sock1.getInputStream()));
                                            os1 = new PrintStream(sock1.getOutputStream()); //to write to mutex server
                                        } catch (Exception e) {
                                            System.err.println("Cannot connect to the mutex server, try again later.");
                                            System.exit(1);
                                        }

                                        try {
                                            os1.println(outGoingFileName + ",check"); // send this to mutex server
                                            flag = Integer.parseInt(in2.readLine());
                                            System.out.println("Got this flag from Mutex Sever.. " + flag);
                                        } catch (Exception e) {
                                            System.err.println("Could not read flag from mutex server");
                                        }

                                        if (flag == 0) { // okay to send

                                            FileServer.files_blocked.add(outGoingFileName);

                                            System.err.print("Preparing to send file...");
                                            sendFile(outGoingFileName);
                                            // can add a waiting statement here.... stating that Client # is writing now.
                                            String doneWriting;
                                            while ((doneWriting = in.readLine()) != null) {
                                                // System.err.print(doneWriting);
                                                // System.err.print("here....");
                                                switch (doneWriting) {
                                                    case "Yes":
                                                        receiveFile(); //receive the updated file
                                                        //reset the entry in the central server
                                                        os1.println(outGoingFileName + ",reset"); // send this to mutex server

                                                        //also, remove from the local files_blocked
                                                        FileServer.files_blocked.remove(outGoingFileName);
                                                        break;
                                                }
                                            }
                                        } else { //not okay to send
                                            // send exception to client
                                            os.println("File is locked, try again later !!");
                                        }

                                        sock1.close();
                                    }
                            }
                        }
                        break;
                    default:
                        System.out.println("Incorrect command received.");
                        break;
                }
                in.close();
                break;
            }

        } catch (IOException ex) {
            Logger.getLogger(CLIENTConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveFile() {
        try {
            int bytesRead;

            DataInputStream clientData = new DataInputStream(clientSocket.getInputStream());
            System.out.println("In receive file....");

            String fileName = clientData.readUTF();
            System.out.println("Got a file..." + fileName);

            OutputStream output = new FileOutputStream((fileName));
            long size = clientData.readLong();
            System.out.println("Got a file of size:..." + size);

            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            output.close();
            clientData.close();

            System.out.println("File "+fileName+" received from client.");

            //whenever the receives a file, it should be put in HDFS and cleared off from master's memory


            //the mutex server is to be updated
            try {
                //opening socket to mutex server
                sock1 = new Socket("localhost", 4445);
                in2 = new BufferedReader(new InputStreamReader(sock1.getInputStream()));

            } catch (Exception e) {
                System.err.println("Cannot connect to the mutex server, try again later.");
                System.exit(1);
            }

            try {
                os1 = new PrintStream(sock1.getOutputStream());
                os1.println(fileName + ",add"); // send this to mutex server
                sock1.close();
            } catch (Exception e) {
                System.err.println("not valid input");
            }

            //broadcast this file to other masters
            broadcastFile(fileName);

        } catch (IOException ex) {
            System.err.println("Client error. Connection closed.");
        }
    }
    public void sendFile(String fileName) {
        try {
            //handle file read
            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //bis.read(mybytearray, 0, mybytearray.length);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            //handle file send over socket
            OutputStream os = clientSocket.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            System.out.println("File "+fileName+" sent to client.");
        } catch (Exception e) {
            System.err.println("File does not exist!");
        }
    }

    public void broadcastFile(String fileName) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);

            File myFile = new File(fileName);
            String temp = fileName + "\n";
            byte[] fname = temp.getBytes();
            byte[] mybytearray = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //bis.read(mybytearray, 0, mybytearray.length);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            byte[] final_array = new byte[fname.length + mybytearray.length];
            System.arraycopy(fname, 0, final_array, 0, fname.length);
            System.arraycopy(mybytearray, 0, final_array, fname.length, mybytearray.length);

            DatagramPacket packet
                    = new DatagramPacket(final_array, final_array.length, InetAddress.getByName("255.255.255.255"), 4446);
            socket.send(packet);
            socket.close();


        } catch (Exception e) {
            System.err.println("Broadcast failed");
        }
    }

}