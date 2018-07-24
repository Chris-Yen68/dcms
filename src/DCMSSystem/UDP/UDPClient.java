package DCMSSystem.UDP;

import DCMSSystem.ByteUtility;

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
            datagramSocket.setSoTimeout(2000);
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
            byte[] hbBytes = ByteUtility.toByteArray(hb);

            DatagramPacket datagramPacket = new DatagramPacket(hbBytes,hbBytes.length,inetAddress,port);
            datagramSocket.send(datagramPacket);
            datagramSocket.close();
            System.out.println("hb "+pid+" sent to "+hostname+":"+port);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            datagramSocket.close();
        }
    }
}
