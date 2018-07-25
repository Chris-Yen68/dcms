package DCMSSystem.UDP;

import DCMSSystem.*;
import DCMSSystem.FrontEnd.Request;
import DCMSSystem.Record.Records;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import sun.security.krb5.internal.PAData;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.CompletableFuture;


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
            while (stop) {
                CompletableFuture<String> reply=CompletableFuture.supplyAsync(() -> "");
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
                            reply = CompletableFuture.supplyAsync(() -> " " + centerServer.getCenterName() + ":" + centerServer.getLocalRecordCount());
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
                        }else if (receiveData.split(":")[0].equals("operation")){
                            String[] replySequences = receiveData.split(":");

                            handleReceived(getPosition(replySequences[0]),replySequences);
                        }
                    } else if (object instanceof Records) {
                        Records record = (Records) object;
                        reply=CompletableFuture.supplyAsync(()->{
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
                            int seqNumber = ByteUtility.generateSeq();
                            Wrapper wrapper = new Wrapper("add",record);
                            String response = centerServer.groupMethodCall(seqNumber,ByteUtility.toByteArray(wrapper));
                            if (response.equals("Done" + ":" + seqNumber)) {
                                return record.getRecordID() + " is stored in the " + centerServer.getCenterName() + " | ";
                            }
                            return "";
                        });
                    } else if (object instanceof Request){
                        Request inRequest = (Request) object;
                        reply=CompletableFuture.supplyAsync(()->{
                            if(inRequest.leaders != null){
                                centerServer.leaders=inRequest.leaders;
                            }
                            int seqNumber = ByteUtility.generateSeq();
                            HashMap<String,String> askContent = inRequest.params;
                            byte [] temp =null;
                            String operation = askContent.get("operation");
                            if (askContent.get("operation").equals("createTRecord")){
                                for (String s : askContent.values()) {
                                    System.out.println(s);
                                }
                                String id = centerServer.createTRecord(askContent.get("managerId"),askContent.get("firstName"),askContent.get("lastName"),askContent.get("address"),askContent.get("phone"),askContent.get("specialization"),askContent.get("location"));

                                System.out.println(id + " is created");
                                Records broadcast = null;
                                for (Map.Entry<Character, ArrayList<Records>> listEntry : centerServer.database.entrySet()) {
                                    for (Records records : listEntry.getValue()) {
                                        if (records.getRecordID().equals(id)){
                                            broadcast = records;
                                            System.out.println(broadcast);
                                        }
                                    }
                                }
                                if (broadcast != null) {
                                    Wrapper wrapper = new Wrapper("createTRecord",askContent.get("managerId"),broadcast);
                                    temp = ByteUtility.toByteArray(wrapper);
                                }else {
                                    System.out.println("Nothing Found!");
                                }

                            } else if (askContent.get("operation").equals("createSRecord")){
                                String[] courseRegistered = askContent.get("courseRegistered").split(" ");
                                for (String s : askContent.keySet()) {
                                    System.out.println(s);
                                }
                                String id = centerServer.createSRecord(askContent.get("managerId"), askContent.get("firstName"), askContent.get("lastName"), courseRegistered, askContent.get("status"), askContent.get("statusDate"));
                                Records broadcast = null;
                                for (Map.Entry<Character, ArrayList<Records>> listEntry : centerServer.database.entrySet()) {
                                    for (Records records : listEntry.getValue()) {
                                        if (records.getRecordID().equals(id)){
                                            broadcast = records;
                                        }
                                    }
                                }
                                if (broadcast != null) {
                                    Wrapper wrapper = new Wrapper("createSRecord",askContent.get("managerId"), broadcast);
                                    temp = ByteUtility.toByteArray(wrapper);
                                    Wrapper wrapper1 = null;
                                    try {
                                        wrapper1 = (Wrapper) ByteUtility.toObject(temp);
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println(wrapper1.getRecords());
                                }
                            } else if (askContent.get("operation").equals("editRecord")){
                                centerServer.editRecord(askContent.get("managerId"),askContent.get("recordID"),askContent.get("fieldName"),askContent.get("newValue"));
                                Wrapper wrapper = new Wrapper("editRecord",askContent);
                                temp = ByteUtility.toByteArray(wrapper);

                            } else if (askContent.get("operation").equals("getRecordCounts")){
                                StringBuilder builder=new StringBuilder();
                                builder.append(centerServer.getCenterName() + ":" + centerServer.getLocalRecordCount() + " ") ;
                                for (Map.Entry<String, ServerProperties> entry : inRequest.leaders.entrySet()) {
                                    if (!entry.getKey().equals(centerServer.getCenterName())) {
                                        builder.append(UDPClient.request(ByteUtility.toByteArray("getCount"), entry.getValue().udpPort)) ;
                                    }
                                }
                                return builder.toString();
                            }else if (askContent.get("operation").equals("transferRecord")){
                                return centerServer.transferRecord(askContent.get("managerId"),askContent.get("recordID"),askContent.get("remoteCenterServerName"));
                            }
                            centerServer.getHandledRequests().add(seqNumber);
                            byte[] content = temp;
                            if (!askContent.get("operation").equals("getRecordCounts") && !askContent.get("operation").equals("transferRecord")){
                                String[] decision = centerServer.groupMethodCall(seqNumber,content).split(":");
                                if (decision.length != 0 && decision != null ){
                                    if (decision[0].equals("Done")){
                                        return askContent.get("operation") + " is done including replica servers!";
                                    }
                                }
                            }
                            return "ok";
                        });
                    }
                    else if (object instanceof Packet){
                        Packet receivedPacket = (Packet) object;
                        CompletableFuture.supplyAsync(()->{
                            String returnValue="";
                            System.out.println("Received Packet from " + receivedPacket.getSender() );
                            Object inPacket = null;

                            try {
                                inPacket = (Object)ByteUtility.toObject(receivedPacket.getContent());

                                System.out.println(receivedPacket.getContent());
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if (!centerServer.getHandledRequests().contains(receivedPacket.getSeqNUmber())){
                                if ((centerServer.getLastReceived().get(receivedPacket.getSender()) + 1) == receivedPacket.getCheckSum()){

                                    String temp = handlePacket(receivedPacket,inPacket);
                                    System.out.println(temp);
                                    if ("operation".equals(temp.split(":")[0])){
                                       String imediatelyreply="operation" + ":" +  temp.split(":")[1] + ":" +  receivedPacket.getSender() + ":" + receivedPacket.getSeqNUmber();
                                        byte[]sendBuffer = imediatelyreply.getBytes();
                                        System.out.println("---------handled: "+imediatelyreply);
                                        DatagramPacket send = new DatagramPacket(sendBuffer, sendBuffer.length, request.getAddress(), request.getPort());
                                        try {
                                            datagramSocket.send(send);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                    for (Map.Entry<String, ServerProperties> entry : centerServer.servers.entrySet()) {
                                        if (entry.getKey().equals(centerServer.getCenterName()) && entry.getValue().status == 1){
                                            String responseGroup = centerServer.groupMethodCall(receivedPacket.getSeqNUmber(), receivedPacket.getContent());
                                            returnValue="Done" + ":" + receivedPacket.getSeqNUmber();
                                        }
                                    }
//                               String[] postions = getPosition(temp.split(":")[1]).split(":");
//                                int value = Integer.valueOf(postions[1]);
                                }else if ((centerServer.getLastReceived().get(receivedPacket.getSender()) + 1) > receivedPacket.getCheckSum()){
                                    System.out.println("Duplicated Message will not be handled!");
                                }else if ((centerServer.getLastReceived().get(receivedPacket.getSender()) + 1) < receivedPacket.getCheckSum()){
                                    if (centerServer.getWaitingList().containsKey(receivedPacket.getSender())){
                                        centerServer.getWaitingList().get(receivedPacket.getSender()).add(receivedPacket);
                                    }else {
                                        LinkedList<Packet> packets = new LinkedList<>();
                                        packets.add(receivedPacket);
                                        centerServer.getWaitingList().put(receivedPacket.getSender(),packets);
                                        System.out.println("Packet from " + receivedPacket.getSender() + " is stored in the waiting list in " + centerServer.getCenterName());
                                    }
                                }
                            }else {
                                returnValue= "operation" + ":" + "DoneBefore" + ":" + receivedPacket.getSender() + ":" + receivedPacket.getSeqNUmber();
                            }

                            if (centerServer.getWaitingList().containsKey(receivedPacket.getSender())){
                                boolean c = true;
                                while (c) {
                                    for (Packet packet : centerServer.getWaitingList().get(receivedPacket.getSender())) {
                                        if ((centerServer.getLastReceived().get(receivedPacket.getSender()) + 1) == packet.getCheckSum()) {
                                            String arbitraryOrdered = handlePacket(receivedPacket, inPacket);
                                            UDPClient.request(ByteUtility.toByteArray(arbitraryOrdered),centerServer.servers.get(packet.getSender()).udpPort);
                                        }
                                    }
                                    c = false;
                                    for (Packet packet : centerServer.getWaitingList().get(receivedPacket.getSender())) {
                                        if ((centerServer.getLastReceived().get(receivedPacket.getSender()) + 1) == packet.getCheckSum()) {
                                            c =true;
                                        }
                                    }
                                }
                            }
                            return returnValue;
                        });

                    }else if (object instanceof Integer){
                        if (centerServer.getHandledRequests().contains((Integer) object)){
                            reply = CompletableFuture.supplyAsync(()->"Done");
                            System.out.println((Integer) object + " is done !!!!!!! No need to Confirm!!!!");
                        }
                    }
                    reply.thenAccept(v->{
                        if (v.length() > 0) {
                            System.out.println(System.currentTimeMillis());
                            byte[] sendBuffer = v.getBytes();
                            System.out.println("Sending: "+v);
                            DatagramPacket send = new DatagramPacket(sendBuffer, sendBuffer.length, request.getAddress(), request.getPort());
                            try {
                                datagramSocket.send(send);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (IOException e) {
                    System.out.println("UDP Server socket is closed!");
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("UDP Server is closed!");

    }

    public String handlePacket(Packet receivedPacket,Object inPacket){
        synchronized (this) {
            String result = "";
            if (inPacket instanceof Wrapper) {
                Wrapper wrapper = (Wrapper) inPacket;
                System.out.println(wrapper.getManagerID() + " : " + wrapper.getRecords());
                if (wrapper.getCommandName().equals("createTRecord") || wrapper.getCommandName().equals("createSRecord") || wrapper.getCommandName().equals("add")) {
                    if (centerServer.database.containsKey(wrapper.getRecords().getLastName().charAt(0))) {
                        centerServer.database.get(wrapper.getRecords().getLastName().charAt(0)).add(wrapper.getRecords());
                    } else {
                        ArrayList<Records> arrayList = new ArrayList<>();
                        arrayList.add(wrapper.getRecords());
                        centerServer.database.put(wrapper.getRecords().getLastName().charAt(0), arrayList);
                    }
                    Log.log(Log.getCurrentTime(), wrapper.getManagerID(), wrapper.getCommandName(), wrapper.getCommandName() + " successfully! Record ID is " + wrapper.getRecords().getRecordID());

                    result = "operation" + ":" + wrapper.getCommandName() + ":" + receivedPacket.getSender() + ":" + receivedPacket.getSeqNUmber();
                } else if (wrapper.getCommandName().equals("delete")) {
                    Records records = null;
                    synchronized (centerServer.database) {
                        for (char key : centerServer.database.keySet()) {
                            for (Records record : centerServer.database.get(key)) {
                                if (record.getRecordID().equals(wrapper.getRecordID())) {
                                    records = record;
                                }
                            }
                        }
                    }
                    if (centerServer.database.get(records.getLastName().charAt(0)).remove(records)) {

                        result = "operation" + ":" + wrapper.getCommandName() + ":" + receivedPacket.getSender() + ":" + receivedPacket.getSeqNUmber();
                    }
                } else if (wrapper.getCommandName().equals("editRecord")) {
                    HashMap<String, String> infor = wrapper.getEditInfor();

                    String response = centerServer.editRecord(infor.get("managerId"), infor.get("recordID"), infor.get("fieldName"), infor.get("newValue"));
                    System.out.println(response);
                    Log.log(Log.getCurrentTime(), infor.get("managerId"), wrapper.getCommandName(), wrapper.getCommandName() + " successfully! Record ID is " + infor.get("recordID"));
                    result = "operation" + ":" + "edit" + ":" + infor.get("recordID") + ":" + receivedPacket.getSender() + ":" + receivedPacket.getSeqNUmber();

                }
            }
            int update = centerServer.getLastReceived().get(receivedPacket.getSender()) + 1;
            centerServer.getLastReceived().replace(receivedPacket.getSender(), update);
            centerServer.getHandledRequests().add(receivedPacket.getSeqNUmber());
            return result;
        }
    }

    public String getPosition(String type){
        synchronized (this) {
            int sender = 0;
            int seqNumer = 0;
            if (type.equals("edit")) {
                sender = 3;
                seqNumer = 4;
            } else if (type.equals("createTRecord") || type.equals("createSRecord") || type.equals("add") || type.equals("delete")) {
                sender = 2;
                seqNumer = 3;
            } else if (type.equals("DoneBefore")) {
                sender = 2;
                seqNumer = 3;
            } else if (type.equals("broadcast")) {
                sender = 2;
                seqNumer = 3;
            }
            return String.valueOf(sender) + ":" + String.valueOf(seqNumer);
        }
    }

    public void handleReceived(String type,String[] sequences){
        synchronized (this) {
            String[] positions = getPosition(type).split(":");
            int sender = Integer.valueOf(positions[0]);
            int seqNumer = Integer.valueOf(positions[1]);
            Integer value = Integer.valueOf(sequences[seqNumer]);
            if (centerServer.getSentPacket().containsKey(value) && centerServer.getSentPacket().get(value) != null) {
                for (String replySequence : centerServer.getSentPacket().get(sequences[seqNumer])) {
                    if (sequences[sender].equals(replySequence)) {
                        centerServer.getSentPacket().get(sequences[seqNumer]).remove(replySequence);
                        System.out.println("remove packet from " + sequences[sender]);
                    }
                }
            }
        }
    }

    public void stopServer() {
        datagramSocket.close();
        stop = false;

    }
}
