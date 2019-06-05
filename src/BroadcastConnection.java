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
            //FileOutputStream output = new FileOutputStream((temp)); // filename
            //int size = Integer.parseInt(bfReader.readLine());

            BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
            while((temp = bfReader.readLine()) != null){
                writer.append(temp);
            }
            writer.close();

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
