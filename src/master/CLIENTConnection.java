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
                                case "r":
                                    while((outGoingFileName = in.readLine()) != null){
                                        System.err.print("\nPreparing to send file...");
                                        sendFile(outGoingFileName);
                                        break;
                                    }
                                case "w":
                                    os = new PrintStream(clientSocket.getOutputStream()); // to write to client

                                    while((outGoingFileName = in.readLine()) != null){
                                        // check mutex and allow access

                                        //if the file is there in the master's blocked list, return there only
                                        if(FileServer.files_blocked.contains(outGoingFileName)) {
                                            // send exception to client
                                            os.println("locked");
                                            break;
                                        }

                                        int flag = 0;

                                        try {
                                            //opening socket to mutex server
                                            String mutex_server_ip = readIPFile("/ip");
                                            sock1 = new Socket(mutex_server_ip, 4445);
                                            in2 = new BufferedReader(new InputStreamReader(sock1.getInputStream()));
                                            os1 = new PrintStream(sock1.getOutputStream()); //to write to mutex server
                                        } catch (Exception e) {
                                            System.err.println("\nCannot connect to the mutex server, try again later.");
                                            System.exit(1);
                                        }

                                        try {
                                            os1.println(outGoingFileName + ",check"); // send this to mutex server
                                            flag = Integer.parseInt(in2.readLine());
                                            System.out.println("\nGot this flag from Mutex Sever.. " + flag);
                                        } catch (Exception e) {
                                            System.err.println("\nCould not read flag from mutex server");
                                        }

                                        if (flag == 0) { // okay to send

                                            FileServer.files_blocked.add(outGoingFileName);

                                            System.err.print("\nPreparing to send file...");
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
                                            os.println("locked");
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

    public static String readIPFile(String IPfileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(IPfileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
    
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }

    public void receiveFile() {
        try {
            int bytesRead;
            DataInputStream clientData = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            System.out.println("In receive file....");
            StringBuffer inputLine = new StringBuffer();
/*          String tmp;
            while ((tmp = clientData.readLine()) != null) {
                    System.out.print("\n here...");
                    inputLine.append(tmp);
                    System.out.println(tmp);
            }
*/          System.out.print("\n");
            System.out.print("clientData available : " + clientData.available());
            byte c = 0;
            String fileName = "";
            int i = 0;
            while(c != -1){
                try{
                        c = clientData.readByte();
                        i++;
                        if(c==',')
                                break;
                        char character_1 = (char)c;
                        System.out.println("Character in fileName : " + c);
                        if(c != 0){
                                System.out.println("Inside while Character in fileName : " + character_1);
                                fileName += character_1;
                        }
        //              System.out.println("Filename: "+ fileName);
                }
                catch(EOFException e){
                        System.out.println("\nNothing in clientData...");
                        break;
                }
            }
            String fileSize = new String();
            while(c != -1){
                try{
                        c = clientData.readByte();
                        i++;
                        if(c == ',')
                                break;
                        //int a = Character.getNumericValue((char)c);
                        //fileSize += a;
                        //System.out.println(new String(c + ""));
                        char character = (char)c;
                        if(character >= 48 && character <= 57){
                                fileSize += character;
                        }
                }
                catch(EOFException e){
                        break;
                }

            }
            for(int j = 0; j<fileSize.length(); j++){
                System.out.println("Character in fileSize string : " + fileSize.charAt(j));
            }
            System.out.println("Got a file... FileName = " + fileName + " File Size = " + fileSize);
            System.out.print("\n\n");
            //fileSize = fileSize.trim();
            Integer file_size = Integer.valueOf(fileSize);
            System.out.println("Number_filesize : " + file_size);
            OutputStream output = new FileOutputStream(fileName);
            //long size = clientData.readLong();
            //System.out.println("Got a file of size:..." + size);
            byte[] buffer = new byte[1024];
            while (file_size > 0 && (bytesRead = clientData.read(buffer, i, (int) Math.min(1024, file_size))) != -1) {
                System.out.println("Inside the while...");
                // this needs to be fixed....
                System.out.println("Buffer.." + buffer);
                output.write(buffer, 0, bytesRead);
                file_size -= bytesRead;
            }
            System.out.println("File "+fileName+" received from client.");

            //*****************TODO********************************
            //whenever the master receives a file, it should be put in HDFS
            //hdfs put (fileName);

            //the mutex server is to be updated
            try {
                //opening socket to mutex server
                String mutex_server_ip = readIPFile("/ip");
                sock1 = new Socket(mutex_server_ip, 4445);
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

            output.close();
            clientData.close();

            //***************************************
            //clear the file off from master's memory
            File file = new File(fileName);
            file.delete();

        } catch (IOException ex) {
            System.out.print("Exception: \n");
            ex.printStackTrace();
            System.err.println("Client error. Connection closed.");
        }
    }


    public void sendFile(String fileName) {
        try {

            //************TODO*****************************
            // get the file from HDFS.. will be stored in the local system, and send it across to the client
            //hdfs get (fileName);

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

            //delete the file from master's memory****************
            myFile.delete();


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
            System.out.println("The updated file has been broadcast to all the Master servers...");
            socket.close();


        } catch (Exception e) {
            System.err.println("\nBroadcast of the file failed.");
        }
    }

}
