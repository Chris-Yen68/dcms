package DCMSSystem;

import DCMSSystem.CenterServerOrb.CenterServicePOA;
import DCMSSystem.CenterServerOrb.CenterServicePackage.except;
import DCMSSystem.UDP.UDPClient;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.INACTIVE;

import java.util.Date;
import java.util.HashMap;

public class FrontEndImpl extends CenterServicePOA{
    public static int[] hardcodedServerPorts = {8180, 8181, 8182};
    public static String[] hardcodedServerNames = {"MTL", "LVL", "DDO"};
    private static HashMap<String, Integer> primaryReplica=new HashMap<String, Integer>();
    private static HashMap<String, Integer> backupReplica=new HashMap<String, Integer>();
    public static HashMap<String, Date> lastNotified;
    public static HashMap<String, Boolean> status;

    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public FrontEndImpl() {
        for (int i = 0; i <hardcodedServerNames.length; i++) {
            primaryReplica.put(hardcodedServerNames[i], hardcodedServerPorts[i]);
            lastNotified.put(hardcodedServerNames[i],new Date());
            status.put(hardcodedServerNames[i], true);
        }
        CheckHeartbeat scheduler= new CheckHeartbeat();
        new Thread(scheduler).start();
    }

    @Override
    public String createTRecord(String managerId, String firstName, String lastName, String address, String phone, String specialization, String location) {
        HashMap<String, String> request=new HashMap<String, String>();
        request.put("operation", "createTRecord");
        request.put("managerId",managerId);
        request.put("firstName",firstName);
        request.put("lastName",lastName);
        request.put("address",address);
        request.put("phone",phone);
        request.put("specialization",specialization);
        request.put("location",location);
        String destination=managerId.substring(0,3);
        byte[] serializedRequest=ByteUtility.toByteArray(request);
        return UDPClient.request(serializedRequest, primaryReplica.get(destination));
    }

    @Override
    public String createSRecord(String managerId, String firstName, String lastName, String[] courseRegistered, String status, String statusDate) {
        HashMap<String, String> request=new HashMap<String, String>();
        StringBuilder stringBuilder = new StringBuilder();
        request.put("operation", "createSRecord");
        request.put("managerId",managerId);
        request.put("firstName",firstName);
        request.put("lastName",lastName);
        request.put("statusDate",statusDate);
        request.put("status",status);
        for (String s :
                courseRegistered) {
            stringBuilder.append(s+" ");
        }
        request.put("courseRegistered", stringBuilder.toString());
        byte[] serializedRequest=ByteUtility.toByteArray(request);
        return UDPClient.request(serializedRequest, primaryReplica.get(managerId.substring(0,3)));
    }

    @Override
    public String getRecordCounts(String managerId) {
        HashMap<String, String> request=new HashMap<String, String>();
        request.put("operation", "getRecordCounts");
        request.put("managerId",managerId);
        byte[] serializedRequest=ByteUtility.toByteArray(request);
        return UDPClient.request(serializedRequest, primaryReplica.get(managerId.substring(0,3)));
    }

    @Override
    public String editRecord(String managerId, String recordID, String fieldName, String newValue) throws except {
        HashMap<String, String> request=new HashMap<String, String>();
        request.put("operation", "editRecord");
        request.put("managerId",managerId);
        request.put("recordID",recordID);
        request.put("fieldName", fieldName);
        request.put("newValue",newValue);
        byte[] serializedRequest=ByteUtility.toByteArray(request);
        return UDPClient.request(serializedRequest, primaryReplica.get(managerId.substring(0,3)));
    }

    @Override
    public String transferRecord(String managerId, String recordID, String remoteCenterServerName) throws except {
        HashMap<String, String> request=new HashMap<String, String>();
        request.put("operation", "transferRecord");
        request.put("managerId",managerId);
        request.put("recordID",recordID);
        request.put("remoteCenterServerName", remoteCenterServerName);
        byte[] serializedRequest=ByteUtility.toByteArray(request);
        return UDPClient.request(serializedRequest, primaryReplica.get(managerId.substring(0,3)));
    }

    @Override
    public void shutdown() {
        HashMap<String, String> request=new HashMap<String, String>();
        request.put("operation", "shutdown");
    }
}

class CheckHeartbeat implements Runnable {

    @Override
    public void run() {
        while (true){
            try {
                Date dateNow=new Date();
                long timeNow=dateNow.getTime();
                for (String name :
                        FrontEndImpl.hardcodedServerNames) {
                    long difference=timeNow-FrontEndImpl.lastNotified.get(name).getTime();
                    if (difference/1000>5){
                        FrontEndImpl.status.put(name, false);
                    }else {
                        FrontEndImpl.status.put(name,true);
                    }
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}