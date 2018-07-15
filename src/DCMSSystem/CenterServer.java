package DCMSSystem;
import DCMSSystem.Record.Records;
import DCMSSystem.Record.StudentRecord;
import DCMSSystem.Record.TeacherRecord;
import DCMSSystem.UDP.UDPClient;
import DCMSSystem.UDP.UDPServer;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.Statement;
import java.lang.annotation.Native;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CenterServer {

    private String centerName;
    private int pid;
    public HashMap<Character, ArrayList<Records>> database = new HashMap<>();
    private UDPServer udpServer;
    private HeartBeat heartBeat;
    public HashMap<String, ServerProperties> servers= new HashMap<>();

    // desired by TA solution of udp address/port hardcoding, since we hardcoding everything,
    public static int[] hardcodedServerPorts = {8180, 8181, 8182, 8170, 8171, 8172, 8160, 8116, 8162};
    public static String[] hardcodedServerNames = {"MTL", "LVL", "DDO", "MTL1", "LVL1", "DDO1", "MTL2", "LVL2", "DDO2"};

    private Thread udpServerThread;
    private Thread heartBeatThread;

    public CenterServer() {
    }


    public CenterServer(String centerName, int portNumber, int pid) throws Exception {
        super();
        //form the list of hardcoded servers in the replica group except current one.
        //as a result there should be only 2 adjacent servers from the same replica group
        IntStream.rangeClosed(0,8)
                .filter((v) -> centerName.substring(0,3).equals(hardcodedServerNames[v].substring(0,3))
                        && !centerName.equals(hardcodedServerNames[v]))
                .forEach((v) -> servers.put(hardcodedServerNames[v],new ServerProperties(hardcodedServerPorts[v],hardcodedServerNames[v].substring(0,3))));
        this.pid = pid;
        this.centerName = centerName;
        udpServer = new UDPServer(portNumber, this);
        udpServerThread = new Thread(udpServer);
        udpServerThread.start();
        heartBeat = new HeartBeat(servers, centerName);
        heartBeatThread = new Thread(heartBeat);
        heartBeatThread.start();
    }

    public String getCenterName() {
        return centerName;
    }

    /*
      Validates the record ID for existence in localDB, in case of existence - regenerates ID and validates again.
    */
    private void validateRecordId(Records inRecord, char key) {
        String recordId = inRecord.getRecordID();
        if (database.get(key) != null) {
            for (Records record : database.get(key)) {
                if (record.getRecordID().equals(recordId)) {
                    inRecord.regenRecordID();
                    validateRecordId(inRecord, key);
                    break;
                }
            }
        }
    }

    // this is going to be a FIFO method caller, which will iterate through servers hashMap and send calls via udp
    // to the members of replica group. Here I don't make any difference if current instance is leader or not,
    // relying on FE to determine who is lead. So basically it will query adjacent servers with "state 1" get their results
    // (confirming that result is returned and is correct), and finally return result to the calling FE
    public String groupMethodCall(){
        String result ="";
        return result;
    }

    public String createTRecord(String managerId, String firstName, String lastName, String address, String phone, String specialization, String location) {
        TeacherRecord teacherRecord = new TeacherRecord(firstName, lastName, address, phone, specialization, location);
        char key = lastName.charAt(0);
        synchronized (database) {
            validateRecordId(teacherRecord, key);
            if (database.get(key) == null) {
                ArrayList<Records> value = new ArrayList<>();
                value.add(teacherRecord);
                database.put(key, value);
            } else {
                ArrayList<Records> value = database.get(key);
                value.add(teacherRecord);
                database.put(key, value);
            }
        }
        Log.log(Log.getCurrentTime(), managerId, "createTRecord", "Create successfully! Record ID is " + teacherRecord.getRecordID());
        return teacherRecord.getRecordID();
    }

    public String createSRecord(String managerId, String firstName, String lastName, String[] courseRegistered, String status, String statusDate) {
        StudentRecord studentRecord = new StudentRecord(firstName, lastName, courseRegistered, status, statusDate);
        char key = lastName.charAt(0);
        synchronized (database) {
            validateRecordId(studentRecord, key);
            if (database.get(key) == null) {
                ArrayList<Records> value = new ArrayList<>();
                value.add(studentRecord);
                database.put(key, value);
            } else {
                ArrayList<Records> value = database.get(key);
                value.add(studentRecord);
                database.put(key, value);
            }
        }
        Log.log(Log.getCurrentTime(), managerId, "createSRecord", "Create successfully! Record ID is " + studentRecord.getRecordID());
        return studentRecord.getRecordID();
    }

    /*
      Concurrent implementation of getRecordCounts using Java8 parallel streams.
    */
    public String getRecordCounts(String managerId) {
        String result;

        //generates result querying servers from hardcoded udp ports, as per TA recommendations

        result = IntStream.rangeClosed(0,2).parallel().mapToObj((v) ->
        {
            byte[] getCount = ByteUtility.toByteArray("getCount");
            String result1 = hardcodedServerNames[v] + ":" + UDPClient.request(getCount, hardcodedServerPorts[v]);
            System.out.printf("\n"+hardcodedServerNames[v]+" on port "+hardcodedServerPorts[v]+" processed\n");
            return result1;
        }).collect(Collectors.joining(" "));

        System.out.printf("\n" + result);
        Log.log(Log.getCurrentTime(), managerId, "getRecordCounts", "Successful");
        return result;
    }

    /*
      Returns local record count for the particular object instance, which is executed by some instance getRecordCount query.
    */
    public int getLocalRecordCount() {
        int sum = 0;
        for (ArrayList<Records> records :
                database.values()) {
            sum += records.size();
        }
        return sum;
    }

    /*
      Edits record using java reflection, to dynamically get the object class (either Student or Teacher) and editable attributes.
    */


    public String editRecord(String managerId, String recordID, String fieldName, String newValue) {
        String result = "";
        Boolean ableModified = true;
        BeanInfo recordInfo;
        synchronized (database) {
            //looks for the recordId in local db
            for (char key : database.keySet()) {
                for (Records record : database.get(key)) {
                    if (record.getRecordID().equals(recordID)) {

                        // following reads information about the object, more precisely of its class, into BeanInfo
                        try {
                            recordInfo = Introspector.getBeanInfo(record.getClass());
                        } catch (Exception e) {
                            return e.getMessage();
                        }

                        //recordPds in this case is the array of properties available in this class
                        PropertyDescriptor[] recordPds = recordInfo.getPropertyDescriptors();
                        for (PropertyDescriptor prop : recordPds) {
                            if (prop.getName().equals(fieldName)) {
                                if (fieldName.equals("location")) {
                                    ableModified = newValue.equals("MTL") || newValue.equals("LVL") || newValue.equals("DDO");
                                }
                            /*
                            Here we form the statement to execute, in our case, update the field in the object.
                            We rely on property names captured in previous recordPds. There is no need in explicit definition
                            of particular Student of TeacherRecord class, since we can just analyze whatever record found
                            with recordId.
                            prop.getWriteMethod() looks for method which writes to property, which was filtered with previous
                            prop.getName().equals(fieldName). As a result newValue is passed as argument to method found, hopefully,
                            it is the proper setter in the end.
                            * look into java reflection and java beans library.
                             */
                                if (ableModified) {

                                    Statement stmt = new Statement(record, prop.getWriteMethod().getName(), new java.lang.Object[]{newValue});
                                    try {
                                        stmt.execute();
                                    } catch (Exception e) {
                                        return e.getMessage();
                                    }
                                    result = "Record updated";

                                    String operation = "edit: " + prop.getName();
                                    Log.log(Log.getCurrentTime(), managerId, operation, result);
                                    return result;
                                } else {
                                    String operation = "edit: " + prop.getName();
                                    result = "The new value is not valid!";
                                    Log.log(Log.getCurrentTime(), managerId, operation, result);
                                    return result;
                                }
                            }

                        }
                        result = "fieldName doesn't match record type";
                        String operation = "edit: " + fieldName;
                        Log.log(Log.getCurrentTime(), managerId, operation, result);
                        return result;
                    }
                }
            }
            result = "No such record Id for this manager";
            Log.log(Log.getCurrentTime(), managerId, "edit: " + fieldName, result);
        }
        return result;
    }

    /*
       transfer record from the server associated with manager if it is verified to the remotecenter which is given by name
    */
    public String transferRecord(String managerID, String recordID, String remoteCenterServerName) {
        String result = "";
        boolean has = false;
        ArrayList<Records> toBeModified = null;
        Records transferedRecord = null;
        synchronized (database) {
            for (char key : database.keySet()) {
                for (Records record : database.get(key)) {
                    if (record.getRecordID().equals(recordID)) {
                        has = true;
                        transferedRecord = record;
                        toBeModified = database.get(key);
                    }
                }
            }
            /*
               check if the id is existing and the remotecenter is valid
             */
            boolean isValidatedCenter = remoteCenterServerName.equals("MTL") || remoteCenterServerName.equals("LVL") || remoteCenterServerName.equals("DDO");
            boolean ableToTransfer = isValidatedCenter && has && !centerName.equals(remoteCenterServerName);
            byte[] serializedMessage = ByteUtility.toByteArray(transferedRecord);
            /*
             using udp to request the function and parse the object to bytes to do the work.
             */
            if (ableToTransfer) {
                result +=  IntStream.rangeClosed(0,2).filter((v) -> hardcodedServerNames[v].equals(remoteCenterServerName))
                        .mapToObj((v) -> UDPClient.request(serializedMessage,  hardcodedServerPorts[v])).collect(Collectors.joining());

                if (toBeModified.remove(transferedRecord)) {
                    result += recordID + " is removed from " + getCenterName();
                }

                Log.log(Log.getCurrentTime(), managerID, "transferRecord:" + recordID, result);

            } else {

                if (!has) {
                    result += "No such record Id for this manager";
                }
                if (!isValidatedCenter) {
                    result += " No such Center to transfer";
                }
                if (centerName.equals(remoteCenterServerName)) {
                    result += " The record is already in the Center,you do not need to tranfer!";

                }
                Log.log(Log.getCurrentTime(), managerID, "tranferRecord:" + recordID, result);
            }
        }
        return result;
    }

    public void bullyElect(){

    }

    public void shutdown() {
        udpServer.stopServer();
    }

}