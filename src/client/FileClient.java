import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class FileClient {

    private static Socket sock;
    private static String fileName;
    private static String mode;
    private static String inputAfterWriting;
    // private static String inputAfterWriting;
    private static BufferedReader stdin;
    private static PrintStream os;

    public static void main(String[] args) throws IOException {
        try {
            sock = new Socket("localhost", 4444);
            stdin = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception e) {
            System.err.println("Cannot connect to the server, try again later.");
            System.exit(1);
        }

        os = new PrintStream(sock.getOutputStream());
        System.err.println("**********************************************");
        System.out.print("\n\n");
        System.out.print("This is a Multi-Cloud Distributed File System.");
        System.out.print("\n\n");
        System.out.print("You are currently logged in to Client 1 on Cluster 1.");
        System.out.print("\n\n");
        System.err.println("**********************************************");

        try {
            switch (Integer.parseInt(selectAction())) {
                case 1:
                    os.println("1");
                    String fileName;
                    System.err.println("Enter file name you wish to upload: ");
                    fileName = stdin.readLine();
                    sendFile(fileName);
                    break;
                case 2:
                    os.println("2");
                    System.err.println("Enter the Mode: Read or Write ?   ");
                    mode = stdin.readLine();
                    os.println(mode);
                    switch(mode){
                        case "Read":
                        // How to display all the available files to read/write ?????
                            System.err.print("Enter the file name you wish to read: ");
                            fileName = stdin.readLine();
                            os.println(fileName);
                            receiveFile(fileName);
                            break;
                        case "Write":
                        // How to display all the available files to read/write ?????
                            System.err.print("Enter the file name you need to write in: ");
                            fileName = stdin.readLine();
                            os.println(fileName);
                            receiveFile(fileName);
                            System.out.print("\n\n");
                            System.err.print("Type 'Yes' to commit your changes -->>>  ");
                            inputAfterWriting = stdin.readLine();
                            os.println(inputAfterWriting);
                            // System.err.print(inputAfterWriting);
                            switch(inputAfterWriting){
                                case "Yes":
                                    // System.err.print("here");
                                    sendFile((fileName));
                                    break;
                            }
                    }
                    break;
            }
        } catch (Exception e) {
            System.out.print("\n\n");
            System.err.println("Oops! Something went wrong :( Please try again. ");
        }
        sock.close();
    }

    public static String selectAction() throws IOException {
        System.out.println("1. Send file.");
        System.out.println("2. Receive file.");
        System.out.print("\nMake selection: ");

        return stdin.readLine();
    }

    public static void sendFile(String fileName) {
        /*try {
            sock = new Socket("192.168.1.5", 4444);
            } catch (Exception e) {
            System.err.println("Cannot connect to the server, try again later.");
            System.exit(1);
            }*/
         try {
            // fileName = stdin.readLine();
            System.err.print(fileName);
            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];
            //byte[] mybytearray = new byte[21];
            System.out.print("\nmyFile.length = "+ myFile.length());
            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);
            System.out.print("\nmyByteArray contents:   ");
            System.out.println(Arrays.toString(mybytearray));
            OutputStream os = sock.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
          //System.out.print("\nMyfile.getname....\n");
          //System.out.print(myFile.getName());
        //    dos.writeUTF(fileName);
            String dos_input = fileName + "," + mybytearray.length + "," + new String(mybytearray);
            System.out.println("DOS input : " + dos_input);
            dos.writeChars(dos_input);
        //dos.writeLong(mybytearray.length);
            //dos.write();
           // System.out.print("\nmybytearray length ");
           // System.out.print(mybytearray.length);
            //dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
           // fis.close();
           // bis.close();
           // dis.close();
           // dos.close();
            sock.shutdownOutput();
            System.out.println("File "+fileName+" sent to Server.");
        } catch (Exception e) {
            System.err.println("File does not exist!");
        }
    }

    public static void receiveFile(String fileName) {
        try {
            int bytesRead;
            InputStream in = sock.getInputStream();

            DataInputStream clientData = new DataInputStream(in);

            fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream((fileName));
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            output.close();
            // in.close();

            System.out.println("File "+fileName+" received from Server.");
        } catch (IOException ex) {
            Logger.getLogger(CLIENTConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}