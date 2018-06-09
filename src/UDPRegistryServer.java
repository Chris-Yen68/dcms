import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class UDPRegistryServer implements Runnable {
    private int portNumber;
    private boolean stop = true;
    private DatagramSocket datagramSocket = null;

    public UDPRegistryServer(int portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(portNumber);
            byte[] buffer = new byte[1024];
            byte[] sendBuffer = new byte[1024];
            while (stop){
                DatagramPacket request = new DatagramPacket(buffer,buffer.length);
                try {
                    datagramSocket.receive(request);
                    String receiveData = new String(request.getData(),0,request.getLength());
                    System.out.printf("received data: "+receiveData+"\n");
                    String reply = "something went wrong";
                    String[] requestData = receiveData.split(":");
                    System.out.printf("request ops: " + requestData[0] + "\n");
                    if (requestData[0].equals("register")) {
                        reply = CenterRegistry.register(requestData[1] + ":" + requestData[2] + ":" + requestData[3]);
                    } else if (requestData[0].equals("getservers")) {
                        reply = CenterRegistry.getServers();
                    } else if (requestData[0].equals("unregister")){
                        reply = CenterRegistry.unRegister(requestData[1]);
                    }

                      else {
                        reply = "wrong request";
                    }
                        sendBuffer = reply.getBytes();

                        DatagramPacket send = new DatagramPacket(sendBuffer, sendBuffer.length, request.getAddress(), request.getPort());
                        datagramSocket.send(send);

                } catch (IOException e) {
                    System.out.println("UDP Registry Server socket is closed!");
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("UDP Registry Server is closed!");
    }
    public void stopServer(){
        datagramSocket.close();
        stop = false;
    }
}
