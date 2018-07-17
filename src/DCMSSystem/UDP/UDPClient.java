package DCMSSystem.UDP;

import java.io.IOException;
import java.net.*;

public class UDPClient {
    public static String request(String operation, String hostname ,int UDPServerPort){
        String receivedInfor = "";
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket();
            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                byte[] opsBytes = operation.getBytes();

                DatagramPacket datagramPacket = new DatagramPacket(opsBytes,operation.length(),inetAddress,UDPServerPort);
                try {
                    datagramSocket.send(datagramPacket);
                    byte[] buffer = new byte[1024];
                    DatagramPacket replayByte = new DatagramPacket(buffer,buffer.length);
                    datagramSocket.receive(replayByte);
                    receivedInfor = new String(replayByte.getData(),0, replayByte.getLength());
                    datagramSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        finally {
            datagramSocket.close();
        }
        return receivedInfor;
    }

    public static String request(byte[] objBytes, int centerPortNumber){
        String receivedInfor = "";
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(1000);
            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                int portNumber = centerPortNumber;

                DatagramPacket datagramPacket = new DatagramPacket(objBytes,objBytes.length,inetAddress,portNumber);
                try {
                    datagramSocket.send(datagramPacket);
                    byte[] buffer = new byte[1024];
                    DatagramPacket replayByte = new DatagramPacket(buffer,buffer.length);
                    datagramSocket.receive(replayByte);
                    receivedInfor = new String(replayByte.getData(),0, replayByte.getLength());
                    datagramSocket.close();
                } catch (SocketTimeoutException e){
                    receivedInfor = "no reply";
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        finally {
            datagramSocket.close();
        }
        return receivedInfor;
    }

    public static void heartbit (String sourceServer, String hostname, int pid, int port){
        DatagramSocket datagramSocket = null;
        String hb = "hb:"+sourceServer+":"+pid;
        try {

            datagramSocket = new DatagramSocket();
            InetAddress inetAddress = InetAddress.getByName(hostname);
            byte[] hbBytes = hb.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(hbBytes,hb.length(),inetAddress,port);
            datagramSocket.send(datagramPacket);
            datagramSocket.close();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            datagramSocket.close();
        }
    }
}
