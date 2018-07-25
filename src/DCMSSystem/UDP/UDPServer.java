package DCMSSystem.UDP;

import DCMSSystem.ByteUtility;
import DCMSSystem.CenterServer;
import DCMSSystem.FrontEnd.Request;
import DCMSSystem.Record.Records;
import net.rudp.ReliableServerSocket;
import net.rudp.ReliableSocketOutputStream;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class UDPServer implements Runnable {
    private int portNumber;
    private CenterServer centerServer;
    private boolean stop = true;
    ReliableServerSocket ssocket;

    public UDPServer(int portNumber, CenterServer centerServer) {
        this.portNumber = portNumber;
        this.centerServer = centerServer;
    }


    @Override
    public void run() {
        try {
            ssocket = new ReliableServerSocket(portNumber);
            ssocket.setReuseAddress(true);

            //datagramSocket = new DatagramSocket(portNumber);

            byte[] buffer = new byte[1024];
            byte[] inBytes = new byte[1024];

            while (stop) {
                CompletableFuture<String> reply=CompletableFuture.supplyAsync(() -> "");
                //DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                try {
                    Object object = null;
                    //datagramSocket.receive(request);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Socket consocket = ssocket.accept();
                    //BufferedInputStream is = consocket.getInputStream();


                    byte[] subbuffer = new byte[1024];
                    int read;
//                    while ((read = consocket.getInputStream().read(subbuffer, 0, subbuffer.length)) != -1) {
//                        baos.write(subbuffer, 0, read);
//
// }
                    consocket.getInputStream().read(subbuffer, 0, subbuffer.length);
                    baos.write(subbuffer, 0, subbuffer.length);
                    baos.flush();
                    inBytes = baos.toByteArray();

                    System.out.printf("some data was recevied via udp "+inBytes.length+"\n");
                    try {
                        object = ByteUtility.toObject(inBytes);

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (object instanceof String) {
                        String receiveData = (String) object;
                        System.out.println(object.toString());
                        if (receiveData.equals("getCount")) {
                            reply = CompletableFuture.supplyAsync(() -> centerServer.getLocalRecordCount());
                        } else if (receiveData.equals("getCountt")) {
                            System.out.println("it works somehow");
                        } else if (receiveData.substring(0, 2).equals("hb")) {
                            System.out.println("incoming hb");
                            String[] hb = receiveData.split(":");
                            if (centerServer.servers.get(hb[1]) != null) {
                                centerServer.servers.get(hb[1]).lastHB = new Date();
                                centerServer.servers.get(hb[1]).pid = Integer.parseInt(hb[2]);
                            } else {
                                System.out.println("non-existent server\n");
                            }
                        } else if (receiveData.equals("elect")) {
                            System.out.println("incoming elect\n");
                            reply = CompletableFuture.supplyAsync(() -> "ok");
                            //centerServer.bullyElect();
                        } else if (receiveData.split(":")[0].equals("victory")) {
                            // upon recieval of victory message from some server - we update the status of that server to 1
                            if (centerServer.servers.get(receiveData.split(":")[1]) != null) {
                                centerServer.servers.get(receiveData.split(":")[1]).status = 1;
                                reply = CompletableFuture.supplyAsync(() -> "ok");
                            } else {
                                System.out.println("wrong victory message from" + centerServer.servers.get(receiveData.split(":")[1]));
                            }
                        } else if (receiveData.split(":")[0].equals("rollback")) {
                            centerServer.removeRecord(receiveData.split(":")[1]);
                            reply = CompletableFuture.supplyAsync(() -> "ok");
                        }
                    } else if (object instanceof Records) {
                        Records record = (Records) object;
                        reply = CompletableFuture.supplyAsync(() -> centerServer.landRecord(record));
                    } else if (object instanceof Request) {
                        Request inRequest = (Request) object;
                        if (inRequest.leaders.size() > 0) {
                            centerServer.leaders = inRequest.leaders;
                        } else {
                            //TODO: describe method calls from hashmap with params
                        }
                    }
                    reply.thenApply(v -> {
                        if (v.length() > 0) {
                            try {
                                byte[] sendBuffer = v.getBytes();
                                System.out.println("Sending: " + v);
                                ReliableSocketOutputStream outToClient = (ReliableSocketOutputStream) consocket.getOutputStream();
                                PrintWriter outputBuffer = new PrintWriter(outToClient);
                                outputBuffer.println(v);
                            } catch (Exception e){
                                e.printStackTrace();
                            }


                        }
                        return "ok";
                    });

                } catch (IOException e) {
                    System.out.println("UDP Server socket is closed!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("UDP Server is closed!");

    }

    public void stopServer() {
        ssocket.close();
        stop = false;

    }
}
