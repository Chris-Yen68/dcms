package DCMSSystem.FrontEnd;

import DCMSSystem.ByteUtility;
import DCMSSystem.CenterServerOrb.CenterServicePOA;
import DCMSSystem.CenterServerOrb.CenterServicePackage.except;
import DCMSSystem.ServerProperties;
import DCMSSystem.UDP.UDPClient;
import org.omg.CORBA.ORB;

import javax.swing.text.html.Option;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FrontEndImpl extends CenterServicePOA {
    public static int[] hardcodedServerPorts = {8180, 8181, 8182, 8170, 8171, 8172, 8160, 8116, 8162};
    public static String[] hardcodedServerNames = {"MTL", "LVL", "DDO", "MTL1", "LVL1", "DDO1", "MTL2", "LVL2", "DDO2"};
    public static HashMap<String, ServerProperties> servers = new HashMap<String, ServerProperties>();
    public Object lock = new Object();
    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public FrontEndImpl() {
        IntStream.rangeClosed(0, 8)
                .forEach((v) -> {
                    servers.put(hardcodedServerNames[v], new ServerProperties(hardcodedServerPorts[v], hardcodedServerNames[v].substring(0, 3)));
                    if (hardcodedServerPorts[v] >= 8180) {
                        servers.get(hardcodedServerNames[v]).status = 1;
                    } else {
                        servers.get(hardcodedServerNames[v]).status = 0;
                    }
                });
        CheckHeartbeat scheduler = new CheckHeartbeat();
        new Thread(new FEUdpServer(8190, this)).start();
        new Thread(scheduler).start();
    }

    @Override
    public String createTRecord(String managerId, String firstName, String lastName, String address, String phone, String specialization, String location) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("operation", "createTRecord");
        params.put("managerId", managerId);
        params.put("firstName", firstName);
        params.put("lastName", lastName);
        params.put("address", address);
        params.put("phone", phone);
        params.put("specialization", specialization);
        params.put("location", location);
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(()->udpSender(new Request(params)));
        String result="";
        try {
            result=future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String createSRecord(String managerId, String firstName, String lastName, String[] courseRegistered, String status, String statusDate) {
        HashMap<String, String> params = new HashMap<String, String>();
        StringBuilder stringBuilder = new StringBuilder();
        params.put("operation", "createSRecord");
        params.put("managerId", managerId);
        params.put("firstName", firstName);
        params.put("lastName", lastName);
        params.put("statusDate", statusDate);
        params.put("status", status);
        for (String s :
                courseRegistered) {
            stringBuilder.append(s + " ");
        }
        params.put("courseRegistered", stringBuilder.toString());
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(()->udpSender(new Request(params)));
        String result="";
        try {
            result=future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String getRecordCounts(String managerId) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("operation", "getRecordCounts");
        params.put("managerId", managerId);
        Map<String, ServerProperties> leaders = servers.entrySet()
                .stream()
                .filter((s) -> s.getValue().status == 1)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(()->udpSender(new Request(params)));
        String result="";
        try {
            result=future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String editRecord(String managerId, String recordID, String fieldName, String newValue) throws except {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("operation", "editRecord");
        params.put("managerId", managerId);
        params.put("recordID", recordID);
        params.put("fieldName", fieldName);
        params.put("newValue", newValue);
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(()->udpSender(new Request(params)));
        String result="";
        try {
            result=future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String transferRecord(String managerId, String recordID, String remoteCenterServerName) throws except {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("operation", "transferRecord");
        params.put("managerId", managerId);
        params.put("recordID", recordID);
        params.put("remoteCenterServerName", remoteCenterServerName);
        Map<String, ServerProperties> leaders = servers.entrySet()
                .stream()
                .filter((s) -> (s.getValue().status == 1) && (s.getKey().equals(remoteCenterServerName)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(()->udpSender(new Request(params)));
        String result="";
        try {
            result=future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void shutdown() {
        HashMap<String, String> request = new HashMap<String, String>();
        request.put("operation", "shutdown");
    }

    public String udpSender(Request request) {
        request.id = ByteUtility.generateSeq();
        byte[] serializedRequest = ByteUtility.toByteArray(request);
        String serverName = request.params.get("managerId").substring(0, 3);
        ServerProperties destination = servers.entrySet()
                .stream()
                .filter(s -> (s.getKey().substring(0, 3).equals(serverName))
                        && (s.getValue().status == 1))
                .map(s -> s.getValue())
                .findFirst().get();
        String result = "";
        /*
            First do(){}while loop means that FE will send request again if udp client doesn't receive reply.
            The second do(){}while loop means that FE will wait a new leader generated when the current leader dead.
         */
        do {
            if (destination.state == 0) {
                do {
                    try {
                        Thread.sleep(3000);
                        destination = servers.entrySet()
                                .stream()
                                .filter(s -> (s.getKey().substring(0, 3).equals(serverName))
                                        && (s.getValue().status == 1))
                                .map(s -> s.getValue())
                                .findFirst().get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }while (destination.state==0);
            }
            result = UDPClient.request(serializedRequest, destination.udpPort);
        } while (result.equals("no reply"));
        return result;
    }
}

class CheckHeartbeat implements Runnable {

    @Override
    public void run() {
        while (true) {
            try {
                Date dateNow = new Date();
                long timeNow = dateNow.getTime();
                FrontEndImpl.servers.entrySet().stream()
                        .filter(s -> (s.getValue().status == 1) && (s.getValue().lastHB != null))
                        .filter(s -> (timeNow - s.getValue().lastHB.getTime()) / 1000 > 3)
                        .forEach(s -> s.getValue().state = 0);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}