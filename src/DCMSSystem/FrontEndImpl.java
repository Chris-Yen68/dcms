package DCMSSystem;

import DCMSSystem.CenterServerOrb.CenterServicePOA;
import DCMSSystem.CenterServerOrb.CenterServicePackage.except;
import DCMSSystem.UDP.FEUdpServer;
import DCMSSystem.UDP.UDPClient;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.INACTIVE;

import java.util.Date;
import java.util.HashMap;
import java.util.stream.IntStream;

public class FrontEndImpl extends CenterServicePOA {
    public static int[] hardcodedServerPorts = {8180, 8181, 8182, 8170, 8171, 8172, 8160, 8116, 8162};
    public static String[] hardcodedServerNames = {"MTL", "LVL", "DDO", "MTL1", "LVL1", "DDO1", "MTL2", "LVL2", "DDO2"};
    public static HashMap<String, ServerProperties> servers = new HashMap<String, ServerProperties>();
    public Object lock;
    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public FrontEndImpl() {
        IntStream.rangeClosed(0, 8)
                .forEach((v) -> {
            servers.put(hardcodedServerNames[v], new ServerProperties(hardcodedServerPorts[v], hardcodedServerNames[v].substring(0, 3)));
            if (hardcodedServerPorts[v]>=8180){
                servers.get(hardcodedServerNames[v]).localNumber=1;
                servers.get(hardcodedServerNames[v]).status=1;
            }else if (hardcodedServerPorts[v]>=8170){
                servers.get(hardcodedServerNames[v]).localNumber=2;
                servers.get(hardcodedServerNames[v]).status=0;
            }else {
                servers.get(hardcodedServerNames[v]).localNumber=3;
                servers.get(hardcodedServerNames[v]).status=0;
            }
                });
        CheckHeartbeat scheduler = new CheckHeartbeat();
        new Thread(new FEUdpServer(8150, this)).start();
        new Thread(scheduler).start();
    }

    @Override
    public String createTRecord(String managerId, String firstName, String lastName, String address, String phone, String specialization, String location) {
        HashMap<String, String> request = new HashMap<String, String>();
        request.put("operation", "createTRecord");
        request.put("managerId", managerId);
        request.put("firstName", firstName);
        request.put("lastName", lastName);
        request.put("address", address);
        request.put("phone", phone);
        request.put("specialization", specialization);
        request.put("location", location);
        return udpSender(request);
    }

    @Override
    public String createSRecord(String managerId, String firstName, String lastName, String[] courseRegistered, String status, String statusDate) {
        HashMap<String, String> request = new HashMap<String, String>();
        StringBuilder stringBuilder = new StringBuilder();
        request.put("operation", "createSRecord");
        request.put("managerId", managerId);
        request.put("firstName", firstName);
        request.put("lastName", lastName);
        request.put("statusDate", statusDate);
        request.put("status", status);
        for (String s :
                courseRegistered) {
            stringBuilder.append(s + " ");
        }
        request.put("courseRegistered", stringBuilder.toString());
        return udpSender(request);
    }

    @Override
    public String getRecordCounts(String managerId) {
        HashMap<String, String> request = new HashMap<String, String>();
        request.put("operation", "getRecordCounts");
        request.put("managerId", managerId);
        return udpSender(request);
    }

    @Override
    public String editRecord(String managerId, String recordID, String fieldName, String newValue) throws except {
        HashMap<String, String> request = new HashMap<String, String>();
        request.put("operation", "editRecord");
        request.put("managerId", managerId);
        request.put("recordID", recordID);
        request.put("fieldName", fieldName);
        request.put("newValue", newValue);
        return udpSender(request);
    }

    @Override
    public String transferRecord(String managerId, String recordID, String remoteCenterServerName) throws except {
        HashMap<String, String> request = new HashMap<String, String>();
        request.put("operation", "transferRecord");
        request.put("managerId", managerId);
        request.put("recordID", recordID);
        request.put("remoteCenterServerName", remoteCenterServerName);
        return udpSender(request);
    }

    @Override
    public void shutdown() {
        HashMap<String, String> request = new HashMap<String, String>();
        request.put("operation", "shutdown");
    }

    public String udpSender(HashMap<String, String> request) {
        byte[] serializedRequest = ByteUtility.toByteArray(request);
        String serverName = request.get("managerId").substring(0, 3);
        if (servers.get(serverName).state == 0) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return UDPClient.request(serializedRequest, servers.get(serverName).udpPort);
    }
}

class CheckHeartbeat implements Runnable {

    @Override
    public void run() {
        while (true) {
            try {
                Date dateNow = new Date();
                long timeNow = dateNow.getTime();
                for (int i = 0; i < 3; i++) {
                    if (FrontEndImpl.servers.get(FrontEndImpl.hardcodedServerNames[i]) == null) {
                        continue;
                    }
                    long difference = timeNow - FrontEndImpl.servers.get(FrontEndImpl.hardcodedServerNames[i]).lastHB.getTime();
                    if (difference / 1000 > 5) {
                        FrontEndImpl.servers.get(FrontEndImpl.hardcodedServerNames[i]).state = 0;
                    } else {
                        FrontEndImpl.servers.get(FrontEndImpl.hardcodedServerNames[i]).state = 1;
                    }
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}