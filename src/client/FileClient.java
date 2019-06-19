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
            String masterIP = readIPFile("/ip");
            sock = new Socket(masterIP, 4444);
            stdin = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception e) {
            System.err.println("Cannot connect to the server, try again later.");
            System.exit(1);
        }
        
        os = new PrintStream(sock.getOutputStream());
//        System.err.println("**********************************************");
//        System.out.print("\n\n");
//        System.out.print("This is a Multi-Cloud Distributed File System.");
//        System.out.print("\n\n");
//        System.out.print("You are currently logged in to Client 1 on Cluster 1.");
//        System.out.print("\n\n");
//        System.err.println("\n**********************************************\n");

        try {
            switch (args[0]) {
                case "snd":
                    os.println("1");
                    String fileName;
                    //System.err.println("Enter file name you wish to upload: ");
                    fileName = args[1];
                    sendFile(fileName);
                    break;
                case "rcv":
                    os.println("2");
                    //System.err.println("Enter the Mode: Read or Write ?   ");
                    mode = args[1];
                    os.println(mode);
                    switch(mode){
                        case "r":
                        // How to display all the available files to read/write ?????
                            //System.err.print("Enter the file name you wish to read: ");
                            fileName = args[2];
                            os.println(fileName);
                            receiveFile(fileName);
                            break;
                        case "w":
                        // How to display all the available files to read/write ?????
                            //System.err.print("Enter the file name you need to write in: ");
                            fileName = args[2];
                            os.println(fileName);

                            //check if any exceptions were written
                            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                            if (in.readLine().equals("locked")) {
                                System.out.println("File locked, try again later!");
                                break;
                            }

                            long file_size = receiveFile(fileName);
                            System.out.print("\n\n");
                            
                            try
                            {
                                FileWriter fw = new FileWriter(fileName,true); //the true will append the new data
                                fw.write("changed the file...\n");//appends the string to the file
                                fw.close();
                            }
                            catch(IOException ioe)
                            {
                                System.err.println("IOException: " + ioe.getMessage());
                            }
                            
                            System.out.println("\nFile has been changed in the backend...");
                            System.out.println("\nFile size has increased. Current file size: "+file_size);
                            System.err.print("\n\nType 'Yes' to commit your changes -->>>  ");
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

    // public static String selectAction() throws IOException {
    //     System.out.println("1. Send file.");
    //     System.out.println("2. Receive file.");
    //     System.out.print("\nMake selection: ");

    //     return stdin.readLine();
    // }

    public static void sendFile(String fileName) {
        
         try {
            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];
            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);
            OutputStream os = sock.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            String dos_input = fileName + "," + mybytearray.length + "," + new String(mybytearray);
            dos.writeChars(dos_input);
       
            dos.flush();
            sock.shutdownOutput();
            System.out.println("File "+fileName+" sent to Server.");
        } catch (Exception e) {
            System.err.println("File does not exist!");
        }
    }

    public static long receiveFile(String fileName) {
        try {
            int bytesRead;
            InputStream in = sock.getInputStream();

            DataInputStream clientData = new DataInputStream(in);

            fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream((fileName));
            long size = clientData.readLong();
            long fileSize = size;
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            output.close();

            System.out.println("File "+fileName+" received from Server.");
            return fileSize;
        } catch (IOException ex) {
            return 0;
        }
    }
}
