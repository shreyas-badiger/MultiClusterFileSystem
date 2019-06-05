import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class MutexServer {

    private static ServerSocket serverSocket;
    private static Socket masterSocket = null;

    private static Map<String, Integer> mutex  = new HashMap<>();
    //key : String -> filename
    //value : 0 -> available for write; 1 -> not available

    //whenever new files are added on any cluster, info about it should be populated here
    //add list of entries to the map
    static void addEntry(List<String> files) {
        for (String file : files) {
            mutex.put(file, 0);
        }
    }

    //remove list of entries from the map
    static void deleteEntry(List<String> files) {
        for (String file: files) {
            mutex.remove(file);
        }
    }

    static void resetEntry(String file) {
        mutex.put(file, 0);
    }

    //returns 1 if file is already locked, 0 otherwise
    static int checkMutex(String file) {
        if (mutex.get(file) == 1) {
            return 1;
        } else {
            mutex.put(file, 1); //TODO
            return 0;
        }
    }

    public static void main(String[] args) throws IOException {

        try {
            serverSocket = new ServerSocket(4445);
            System.out.println("Server started.");
        } catch (Exception e) {
            System.err.println("Port already in use.");
            System.exit(1);
        }

        while (true) {
            try {
                masterSocket = serverSocket.accept();
                System.out.println("Accepted connection : " + masterSocket);

                Thread t = new Thread(new MasterConnection(masterSocket));

                t.start();

            } catch (Exception e) {
                System.err.println("Error in connection attempt.");
            }
        }
    }

}
