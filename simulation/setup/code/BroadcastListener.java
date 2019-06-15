import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

public class BroadcastListener {

    private static DatagramSocket socket;

    public static void main(String[] args) throws IOException {

        try {
            socket = new DatagramSocket(4446);
            System.out.println("Listening to Broadcast messages on port 4446...");
        } catch (Exception e) {
            System.err.println("Port already in use.");
            System.exit(1);
        }


        while (true) {
            try {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, 1024);
                socket.receive(packet);
                System.out.println("Received packet...");

                Thread t = new Thread(new BroadcastConnection(packet));
                t.start();
                //socket.close();

            } catch (Exception e) {
                System.err.println("Error in connection attempt.");
            }
        }
    }
}
