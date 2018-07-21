package DCMSSystem.UDP;

import DCMSSystem.ByteUtility;
import DCMSSystem.CenterServer;
import DCMSSystem.FrontEnd.Request;
import DCMSSystem.Record.Records;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class UDPServer implements Runnable {
    private int portNumber;
    private CenterServer centerServer;
    private boolean stop = true;
    private DatagramSocket datagramSocket = null;

    public UDPServer(int portNumber, CenterServer centerServer) {
        this.portNumber = portNumber;
        this.centerServer = centerServer;
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(portNumber);

            byte[] buffer = new byte[1024];
            byte[] sendBuffer = new byte[1024];
            while (stop) {
                String reply = "";
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                try {
                    Object object = null;
                    datagramSocket.receive(request);
                    System.out.printf("some data was recevied via udp\n");
                    try {
                        object = ByteUtility.toObject(request.getData());

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (object instanceof String) {
                        String receiveData = (String) object;
                        System.out.println(object.toString());
                        if (receiveData.equals("getCount")) {
                            reply += centerServer.getLocalRecordCount();
                        }
                        else if (receiveData.equals("getCountt")){
                            System.out.println("it works somehow");
                        }

                        else if (receiveData.substring(0,2).equals("hb")) {
                            String[] hb = receiveData.split(":");
                            if (centerServer.servers.get(hb[1]) != null) {
                                centerServer.servers.get(hb[1]).lastHB = new Date();
                                centerServer.servers.get(hb[1]).pid = Integer.parseInt(hb[2]);
                            } else {
                                System.out.println("non-existent server\n");
                            }
                        } else if (receiveData.equals("elect")) {
                            System.out.println("incoming elect\n");
                            reply = "ok";
                            //centerServer.bullyElect();
                        } else if (receiveData.split(":")[0].equals("victory")) {
                            // upon recieval of victory message from some server - we update the status of that server to 1
                            if (centerServer.servers.get(receiveData.split(":")[1]) != null) {
                                centerServer.servers.get(receiveData.split(":")[1]).status = 1;
                                reply = "ok";
                            } else {
                                System.out.println("wrong victory message from" + centerServer.servers.get(receiveData.split(":")[1]));
                            }
                        }
                    } else if (object instanceof Records) {
                        Records record = (Records) object;
                        HashMap<Character, ArrayList<Records>> centerdata = centerServer.database;
                        synchronized (centerdata) {
                            if (centerdata.get(record.getLastName().charAt(0)) != null) {
                                centerdata.get(record.getLastName().charAt(0)).add(record);

                            } else {
                                ArrayList<Records> newArray = new ArrayList<>();
                                newArray.add(record);
                                centerdata.put(record.getLastName().charAt(0), newArray);
                                System.out.println(record.getRecordID());
                            }
                        }
                        reply = record.getRecordID() + " is stored in the " + centerServer.getCenterName() + " | ";

                    } else if (object instanceof Request){
                        Request inRequest = (Request) object;
                        if(inRequest.leaders.size()>0){
                            centerServer.leaders=inRequest.leaders;
                        }

                    }
                    if (reply.length() > 0) {
                        sendBuffer = reply.getBytes();
                        System.out.println("Sending: "+reply);
                        DatagramPacket send = new DatagramPacket(sendBuffer, sendBuffer.length, request.getAddress(), request.getPort());
                        datagramSocket.send(send);
                    }

                } catch (IOException e) {
                    System.out.println("UDP Server socket is closed!");
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("UDP Server is closed!");

    }

    public void stopServer() {
        datagramSocket.close();
        stop = false;

    }
}
