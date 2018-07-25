package DCMSSystem.UDP;

import DCMSSystem.ByteUtility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

import net.rudp.ReliableSocket;
import net.rudp.ReliableSocketOutputStream;

public class UDPClient {

    public static String request(byte[] objBytes, int centerPortNumber) {
        String receivedInfor = "";
        ReliableSocket socket;
        try {
            socket = new ReliableSocket();
            socket.setSoLinger(true, 0);
            socket.setSoTimeout(1000);
            try {
                //send part
                socket.connect(new InetSocketAddress("localhost", centerPortNumber),1000);
                ReliableSocketOutputStream outToClient = (ReliableSocketOutputStream) socket.getOutputStream();
                System.out.println(objBytes.length);
                outToClient.write(objBytes);
                outToClient.flush();
                outToClient.close();

                //receive part
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                receivedInfor = inFromClient.readLine();
            } catch (IOException e) {
                receivedInfor = "no reply";
            } finally {

                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return receivedInfor;
    }

    public static void heartbit(String sourceServer, String hostname, int pid, int port) {
        try {

            String hb = "hb:" + sourceServer + ":" + pid;
            byte[] hbBytes = ByteUtility.toByteArray(hb);
            try {
                ReliableSocket socket = new ReliableSocket();
                socket.setSoLinger(true, 0);
                socket.setSoTimeout(1000);
                try {
                    socket.connect(new InetSocketAddress(hostname, port), 1000);
                    ReliableSocketOutputStream outToClient = (ReliableSocketOutputStream) socket.getOutputStream();
                    outToClient.write(hbBytes, 0, hbBytes.length);
                    outToClient.flush();
                    outToClient.close();
                } catch (Exception e){

                } finally {
                    socket.close();
                }
                System.out.println("hb " + pid + " sent to " + hostname + ":" + port);
                System.out.println("\n"+hbBytes.length);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

    }
}
