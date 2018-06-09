import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPServer implements Runnable {
    private int portNumber;
    private CenterSystem centerSystem;
    private boolean stop = true;
    private DatagramSocket datagramSocket = null;

    public UDPServer(int portNumber, CenterSystem centerSystem) {
        this.portNumber = portNumber;
        this.centerSystem = centerSystem;
    }

    public CenterSystem getCenterSystem() {
        return centerSystem;
    }

    public void setCenterSystem(CenterSystem centerSystem) {
        this.centerSystem = centerSystem;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
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
                    System.out.printf("some data was recevied via udp");
                    String receiveData = new String(request.getData(),0,request.getLength());
                    System.out.printf(receiveData);
                    if (receiveData.equals("getCount")) {
                        String reply = centerSystem.getLocalRecordCount() + "";
                        sendBuffer = reply.getBytes();
                    }

                    DatagramPacket send = new DatagramPacket(sendBuffer,sendBuffer.length,request.getAddress(),request.getPort());
                    datagramSocket.send(send);

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
