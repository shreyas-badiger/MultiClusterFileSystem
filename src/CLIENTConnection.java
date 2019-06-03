import java.io.*;   
import java.text.*;   
import java.util.*;   
import java.net.*;  
import java.util.logging.Logger; 
import java.util.logging.Level;


public class CLIENTConnection implements Runnable {

    private Socket clientSocket;
    private BufferedReader in = null;

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
                                    while((outGoingFileName = in.readLine()) != null){
                                        System.err.print("Preparing to send file...");
                                        sendFile(outGoingFileName);
                                        // can add a waiting statement here.... stating that Client # is writing now.
                                        String doneWriting;
                                        while((doneWriting = in.readLine()) != null){
                                            // System.err.print(doneWriting);
                                            // System.err.print("here....");
                                            switch(doneWriting){
                                                case "Yes":
                                                    receiveFile();
                                                    break;
                                            }
                                        }
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

            String fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream(("./Server_folder/received_from_client_" + fileName));
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            output.close();
            clientData.close();

            System.out.println("File "+fileName+" received from client.");
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
}