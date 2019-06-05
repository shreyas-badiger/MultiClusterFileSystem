import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.logging.Logger;
import java.util.logging.Level;


public class MasterConnection implements Runnable {

    private Socket masterSocket;
    private BufferedReader in = null;
    int flag;

    public MasterConnection(Socket client) {
        this.masterSocket = client;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()));

            //get the filename,oper from a master
            String fname_oper = in.readLine();
            String[] pair = fname_oper.trim().split(",");

            //pair[0] has the file name ; pair[1] has the operation requested
            //operation could be check, reset, add, delete an entry in the map in Mutex server

            if (pair[1].equals("check")) {
                flag = MutexServer.checkMutex(pair[0]);
                System.out.println("Got Flag.." + flag);

                //send flag to the master
                PrintStream os = new PrintStream(masterSocket.getOutputStream());
                os.println(flag);
                System.out.println("Flag sent to Master server.");
            }

            else if (pair[1].equals("reset")) {
                MutexServer.resetEntry(pair[0]);
                System.out.println("Resetting.. ");
            }

            else if (pair[1].equals("add")) {
                MutexServer.addEntry(Arrays.asList(pair[0]));
                System.out.println("Adding this : " + pair[0] + " " + pair[1]);
            }

            else if (pair[1].equals("delete")) {
                MutexServer.deleteEntry(Arrays.asList(pair[0]));
            }

        } catch (Exception e) {
            System.err.println(" error in master connection.. ");
        }
    }
}
