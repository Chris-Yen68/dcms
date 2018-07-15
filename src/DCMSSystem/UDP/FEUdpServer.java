package DCMSSystem.UDP;

import DCMSSystem.FrontEndImpl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Date;

public class FEUdpServer implements Runnable{
    private int portNumber;
    FrontEndImpl frontEnd;
    private boolean stop = true;
    private DatagramSocket datagramSocket = null;

    public FEUdpServer(int portNumber, FrontEndImpl frontEnd) {
        this.portNumber = portNumber;
        this.frontEnd=frontEnd;
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
                    Object object = null;
                    datagramSocket.receive(request);
                    System.out.printf("some data was recevied via udp");
                    String heartbeat=request.getData().toString();
                    frontEnd.servers.get(heartbeat).lastHB=new Date();
                } catch (IOException e) {
                    System.out.println("UDP Server socket is closed!");
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("UDP Server is closed!");

    }

    public void stopServer(){
        datagramSocket.close();
        stop = false;

    }
}
