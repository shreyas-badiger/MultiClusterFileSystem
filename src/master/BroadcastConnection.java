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
        // System.out.println("\nPacket size (broadcast connection): " + pkt.getLength());
        // System.out.println("\nContent byte array (braodcast connection) : ");
        // for(int ind=0; ind<content.length; ind++){
        //     System.out.println(content[ind]);
        // }
        InputStream is = null;
        BufferedReader bfReader = null;
        try {
            is = new ByteArrayInputStream(content);
            bfReader = new BufferedReader(new InputStreamReader(is));

            String temp = null;
            temp = bfReader.readLine(); // read the filename
            System.out.println("\nMaster node is now listening to broadcast messages...\n"+ temp);

            //temp could be a filename or an IP in the format IP-<ip address>

            if(temp.startsWith("IP")) {
                String[] arr = temp.split("-");
                String newMutexServerIP = arr[1];
                File f = new File("/ip");
                boolean success = f.delete();
                PrintWriter writer = new PrintWriter("/ip", "UTF-8");
                writer.println(newMutexServerIP);
                writer.close();

                //update the IP file with this IP, so that after this the master will call this new server

            } else {

                //FileOutputStream output = new FileOutputStream((temp)); // filename
                //int size = Integer.parseInt(bfReader.readLine());

                // BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
                FileWriter file_writer = new FileWriter(temp);
                byte[] contents_array = new byte[pkt.getLength() - temp.length()];
                System.arraycopy(content, temp.length()+1, contents_array, 0, contents_array.length);
                // System.out.println("\nContents_array : ");
                // for(int ind=0; ind<contents_array.length-1; ind++){
                //     System.out.println(contents_array[ind]);
                // }


                InputStream new_is = new ByteArrayInputStream(contents_array);
                BufferedReader new_bfReader = new BufferedReader(new InputStreamReader(new_is));
                System.out.println("\nFile Contents : \n");
                while((temp = new_bfReader.readLine()) != null){
                    System.out.println("\n" + temp);
                    file_writer.write(temp);
                }
                file_writer.close();
                

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
