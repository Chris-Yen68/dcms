package DCMSSystem.FrontEnd;

import DCMSSystem.ByteUtility;
import DCMSSystem.FrontEnd.FrontEndImpl;
import DCMSSystem.ServerProperties;
import DCMSSystem.UDP.UDPClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

public class FEUdpServer implements Runnable {
    private int portNumber;
    FrontEndImpl frontEnd;
    private boolean stop = true;
    private DatagramSocket datagramSocket = null;

    public FEUdpServer(int portNumber, FrontEndImpl frontEnd) {
        this.portNumber = portNumber;
        this.frontEnd = frontEnd;
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(portNumber);
            byte[] buffer = new byte[1024];
            byte[] sendBuffer = new byte[1024];
            while (stop) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                try {
                    Object object = null;
                    String receive = "";
                    datagramSocket.receive(request);
                    object = ByteUtility.toObject(request.getData());
                    receive = (String) object;
                    String[] info = receive.split(":");
                    if (info[0].equals("hb")) {
                        /*
                            Receive heartbeat and set the lastHB time.
                         */
                        String heartbeat = info[1];
                        frontEnd.servers.entrySet().stream()
                                .filter(s->s.getValue().replicaGroup.equals(heartbeat.substring(0,3)))
                                .forEach(s->s.getValue().state=1);
                        frontEnd.servers.get(heartbeat).lastHB = new Date();
                        System.out.println("Get hb from " + heartbeat);
                    } else {
                        /*
                            Receive election victory info, remove the old leader and set the new leader.
                         */
                        String victory = info[1];
                        frontEnd.servers.get(victory).status = 1;
                        System.out.println("The new leader for group " + victory.substring(0, 3) + " is " + victory);
                        String oldLeader = frontEnd.servers.entrySet().stream()
                                .filter(s -> (s.getValue().status == 1) && (s.getValue().state == 0) && (s.getValue().replicaGroup.equals(victory.substring(0, 3))))
                                .findFirst().get().getKey();
                        frontEnd.servers.remove(oldLeader);
                        synchronized (frontEnd.lock) {
                            frontEnd.lock.notify();
                        }
                        /*
                            Broadcast updated leaders info to each leader.
                         */
                        Map<String, ServerProperties> leaders = frontEnd.servers.entrySet().stream()
                                .filter(s -> (s.getValue().status == 1) && (s.getValue().state == 1))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        byte[] bytes = ByteUtility.toByteArray(leaders);
                        leaders.entrySet().stream()
                                .forEach(entry -> UDPClient.request(bytes, entry.getValue().udpPort));

                        /*
                            reply with ok
                         */
                        String reply = "ok";
                        System.out.println("Sending: " + reply);
                        sendBuffer = reply.getBytes();
                        DatagramPacket send = new DatagramPacket(sendBuffer, sendBuffer.length, request.getAddress(), request.getPort());
                        datagramSocket.send(send);
                    }

                } catch (IOException e) {
                    System.out.println("UDP Server socket is closed!");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
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
