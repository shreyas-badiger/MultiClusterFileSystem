import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class BroadcastConnection implements Runnable {

    DatagramPacket pkt = null;

    public BroadcastConnection(DatagramPacket pkt) {
        this.pkt = pkt;
    }

    @Override
    public void run() {
        byte[] content =  pkt.getData();
        InputStream is = null;
        BufferedReader bfReader = null;
        try {
            is = new ByteArrayInputStream(content);
            bfReader = new BufferedReader(new InputStreamReader(is));

            String temp = null;
            temp = bfReader.readLine(); // read the filename
            System.out.println("hereeee...  "+ temp);

            //temp could be a filename or an IP in the format IP-<ip address>

            if(temp.startsWith("IP")) {
                String[] arr = temp.split("-");
                String newMutexServerIP = arr[1];

                //update the IP file with this IP, so that after this the master will call this new server

            } else {

                //FileOutputStream output = new FileOutputStream((temp)); // filename
                //int size = Integer.parseInt(bfReader.readLine());

                BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
                while((temp = bfReader.readLine()) != null){
                    writer.append(temp);
                }
                writer.close();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        finally {
            try{
                if(is != null) is.close();
            } catch (Exception ex){

            }
        }
    }
}
