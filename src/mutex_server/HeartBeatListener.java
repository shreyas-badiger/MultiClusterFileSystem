import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.logging.Logger;
import java.util.logging.Level;


//will be run on the backup mutex server

public class HeartBeatListener {

    private static ServerSocket hblistener;
    private static Socket hbsender = null;
    private static BufferedReader in = null;

    public static void main(String[] args) throws IOException {

        try {
            hblistener = new ServerSocket(4447);
            System.out.println("Hb listener started.");
        } catch (Exception e) {
            System.err.println("Port already in use.");
            System.exit(1);
        }


            try {
                hbsender = hblistener.accept();
                System.out.println("Accepted connection : " + hbsender);

                in = new BufferedReader(new InputStreamReader(hbsender.getInputStream()));
                hbsender.setSoTimeout(30000); //expect to read every 30s

                while(true) {
                    System.out.println(in.readLine()); //will go on until socket times out
                }

            } catch (SocketTimeoutException e) {
                System.err.println("Mutex Server is down.. Taking charge..");
                //send a broadcast to the master network
                broadcastIP("localhost");
            }


    }

    //on receiving this broadcast, all the masters should change their ip files to this IP
    public static void broadcastIP(String thisIP) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            String IPstr = "IP-" + thisIP;
            byte[] msg = IPstr.getBytes();
            DatagramPacket packet
                    = new DatagramPacket(msg, msg.length, InetAddress.getByName("255.255.255.255"), 4446);
            socket.send(packet);
            socket.close();
        } catch (Exception e) {
            System.err.println("Broadcast IP failed");
        }
    }

}
