import java.io.*;
import java.util.*;
import java.net.*;


//will be run on the primary mutex server

public class HeartBeatSender extends TimerTask {

    private static Socket hblistener = null;
    private static PrintStream os1;


    public void run() {
        try {
            os1.println("heartbeat");

        } catch (Exception e) {
            System.out.println("\nFailed to send heartbeat message!")
        }
    }

    public static void main(String[] args) throws IOException {

        hblistener = new Socket("10.0.0.6", 4447);
        os1 = new PrintStream(hblistener.getOutputStream()); //to write to backup server

        Timer timer = new Timer();
        timer.schedule(new HeartBeatSender(), 0, 5000); //keep writing the heartbeat message every 30 seconds until dead

    }
}
