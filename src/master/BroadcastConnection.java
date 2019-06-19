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
            temp = bfReader.readLine(); // read the filename or IP string
            System.out.println("hereeee...  "+ temp);

            //temp could be a filename or an IP in the format IP-<ip address>
            //filename when another master broadcasts a file
            //IP-<ip address> when backup mutex server takes charge and informs all to call it for mutex
            if(temp.startsWith("IP")) {
                String[] arr = temp.split("-");
                String newMutexServerIP = arr[1];

                //***********TODO***************************************
                //update the IP file with this IP, so that after this the master will call this new server

            } else {

                //FileOutputStream output = new FileOutputStream((temp)); // filename
                //int size = Integer.parseInt(bfReader.readLine());

                BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
                String line;
                while((line = bfReader.readLine()) != null){
                    writer.append(line);
                }
                writer.close();

                //************TODO*****************
                //This master received a file, should put it in the local HDFS.  //temp holds the filename
                //hdfs put (temp)


                //***************************************
                //clear the file off from master's memory
                File file = new File(temp);
                file.delete();

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
