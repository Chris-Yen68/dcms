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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CenterServer {

    private String centerName;

    public HashMap<Character, ArrayList<Records>> database = new HashMap<>();
    private UDPServer udpServer;
    public HeartBeat heartBeat;
    private final int FEPortNumber = 8150;
    public HashMap<String, ServerProperties> servers = new HashMap<>();

    // desired by TA solution of udp address/port hardcoding, since we hardcoding everything,
    public static int[] hardcodedServerPorts = {8180, 8181, 8182, 8170, 8171, 8172, 8160, 8161, 8162, 8190};
    public static String[] hardcodedServerNames = {"MTL", "LVL", "DDO", "MTL1", "LVL1", "DDO1", "MTL2", "LVL2", "DDO2", "FEServer"};

    private Thread udpServerThread;
    private Thread heartBeatThread;


    public int pid;
    public int version;
    public Boolean isLeader = false;
    public Map<String, ServerProperties> leaders;

    public CenterServer() {
    }


    public CenterServer(String centerName, int portNumber, int pid) throws Exception {
        super();
        //form the list of hardcoded servers in the replica group except current one.
        //as a result there should be address map with 2 adjacent servers from the same replica group and frontend server.
        IntStream.rangeClosed(0, 9)
                .filter((v) -> (centerName.substring(0, 3).equals(hardcodedServerNames[v].substring(0, 3))
                        && !centerName.equals(hardcodedServerNames[v])) || hardcodedServerNames[v].equals("FEServer"))
                .forEach((v) -> servers.put(hardcodedServerNames[v], new ServerProperties(hardcodedServerPorts[v], hardcodedServerNames[v].substring(0, 3))));
        System.out.println(servers.keySet().toString());
        servers.get("FEServer").status = 2;

        //predefining first instance as the Lead
        if (servers.get(centerName.substring(0, 3)) != null) {
            servers.get(centerName.substring(0, 3)).status = 1;
        }

        this.pid = pid;
        this.centerName = centerName;
        udpServer = new UDPServer(portNumber, this);
        udpServerThread = new Thread(udpServer);
        udpServerThread.start();
        heartBeat = new HeartBeat(servers, this);
        heartBeatThread = new Thread(heartBeat);
        if (centerName.length() == 3) {
            isLeader = true;
        }
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

    public void removeRecord(String recordId) {
        synchronized (database) {
            char key = recordId.charAt(0);
            database.get(key).stream().filter(v -> v.getRecordID().equals(recordId))
                    .forEach(v -> database.get(key).remove(v));
        }
    }

    public String createTRecord(String managerId, String firstName, String lastName, String address, String phone, String specialization, String location) {
        if (isLeader) {

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
            byte[] serializedRecord = ByteUtility.toByteArray(teacherRecord);
            byte[] rollbackRecord = ByteUtility.toByteArray("rollback:" + teacherRecord.getRecordID());

            if (servers.entrySet().stream().filter(v -> v.getValue().state == 1)
                    .map(v -> UDPClient.request(serializedRecord, v.getValue().udpPort))
                    .allMatch(v -> v.substring(0, 11).equals("Transfer ok"))) {
                return teacherRecord.getRecordID() + " is created";
            } else {
                //some rollback implementation.
                database.remove(teacherRecord.getRecordID());
                servers.entrySet().stream().filter(v -> v.getValue().state == 1)
                        .forEach(v -> UDPClient.request(rollbackRecord, v.getValue().udpPort));
                return "fail during execution, rolled back";
            }
        } else {
            return "cannot execute, must be a leader";
        }
    }

    public String createSRecord(String managerId, String firstName, String lastName, String[] courseRegistered, String status, String statusDate) {
        if (isLeader) {
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
            //return studentRecord.getRecordID();
            byte[] serializedRecord = ByteUtility.toByteArray(studentRecord);
            byte[] rollbackRecord = ByteUtility.toByteArray("rollback:" + studentRecord.getRecordID());

            if (servers.entrySet().stream().filter(v -> v.getValue().state == 1)
                    .map(v -> UDPClient.request(serializedRecord, v.getValue().udpPort))
                    .allMatch(v -> v.substring(0, 11).equals("Transfer ok"))) {
                return studentRecord.getRecordID() + " is created";
            } else {
                //some rollback implementation.
                database.remove(studentRecord.getRecordID());
                servers.entrySet().stream().filter(v -> v.getValue().state == 1)
                        .forEach(v -> UDPClient.request(rollbackRecord, v.getValue().udpPort));
                return "fail during execution, rolled back";
            }
        } else {
            return "cannot execute, must be a leader";
        }

    }

    /*
      Concurrent implementation of getRecordCounts using Java8 parallel streams.
    */
    public String getRecordCounts(String managerId) {

        String result;
        //generates result querying servers from leaders list
        result = leaders.keySet().stream().parallel()
                .filter(v -> !v.equals(centerName))
                .map(v ->
                        {
                            byte[] getCount = ByteUtility.toByteArray("getCount");
                            String result1 = v + ":" + UDPClient.request(getCount, servers.get(v).udpPort);
                            return result1;
                        }
                )
                .collect(Collectors.joining(" "));
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

    /*TODO: in the same way as others this method should be modified to support operation as leader, which calls other servers in group
    additionally to regular mode. see landRecord method
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
                        try {
                            recordInfo = Introspector.getBeanInfo(record.getClass());
                        } catch (Exception e) {
                            return e.getMessage();
                        }
                        PropertyDescriptor[] recordPds = recordInfo.getPropertyDescriptors();
                        for (PropertyDescriptor prop : recordPds) {
                            if (prop.getName().equals(fieldName)) {
                                if (fieldName.equals("location")) {
                                    ableModified = newValue.equals("MTL") || newValue.equals("LVL") || newValue.equals("DDO");
                                }
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
    //TODO:refactor the flow of validations, too heavy.
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
             using udp to params the function and parse the object to bytes to do the work.
             */
            if (ableToTransfer) {
                if (leaders.keySet().stream()
                        .filter(v -> v.equals(remoteCenterServerName))
                        .map(v -> UDPClient.request(serializedMessage, servers.get(v).udpPort))
                        .allMatch(v -> v.substring(0, 11).equals("Transfer ok"))) {
                    toBeModified.remove(transferedRecord);
                    result = recordID + " is transfered to " + remoteCenterServerName;
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


    //manipulates with local records.
    public String landRecord(Records record) {
        synchronized (database) {
            if (database.get(record.getLastName().charAt(0)) != null) {
                database.get(record.getLastName().charAt(0)).add(record);

            } else {
                ArrayList<Records> newArray = new ArrayList<>();
                newArray.add(record);
                database.put(record.getLastName().charAt(0), newArray);
                System.out.println(record.getRecordID());
            }
            if (isLeader) {
                byte[] serializedRecord = ByteUtility.toByteArray(record);
                byte[] rollbackRecord = ByteUtility.toByteArray("rollback:" + record.getRecordID());

                if (servers.entrySet().stream().filter(v -> v.getValue().state == 1)
                        .map(v -> UDPClient.request(serializedRecord, v.getValue().udpPort))
                        .allMatch(v -> v.substring(0, 11).equals("Transfer ok"))) {
                    return "Transfer ok: " + record.getRecordID() + " to " + centerName;
                } else {
                    //some rollback implementation.
                    database.remove(record.getRecordID());
                    servers.entrySet().stream().filter(v -> v.getValue().state == 1)
                            .forEach(v -> UDPClient.request(rollbackRecord, v.getValue().udpPort));
                    return "fail during execution, rolled back";
                }
            } else {
                return "Transfer ok: " + record.getRecordID() + " to " + centerName;
            }
        }
    }


    //executes election of leader among the replica group members
    // as of now this implementation won't reattempt victory message in case it's not delivered to FE or
    // reply of FE is not delivered to the instance
    public void bullyElect() {
        byte[] elect = ByteUtility.toByteArray("elect");
        byte[] victory = ByteUtility.toByteArray("victory:" + centerName);

        if (servers.keySet().stream().parallel()
                .filter((v) -> servers.get(v).status != 2 && servers.get(v).state == 1 && servers.get(v).pid > this.pid)
                .count() > 0) {
            if (servers.keySet().stream()
                    .filter((v) -> servers.get(v).status != 2 && servers.get(v).state == 1 && servers.get(v).pid > this.pid)
                    .map((v) -> UDPClient.request(elect, servers.get(v).udpPort))
                    .allMatch((v) -> v.equals("no reply"))) {
                //when instance hits the case, when no bigger pid around,it declares to everybody including FE its leadership
                System.out.println("nobody replied, turning into Lead\n");
                servers.keySet().stream()
                        .filter((v) -> servers.get(v).state == 1 || servers.get(v).status == 2)
                        .forEach((v) -> UDPClient.request(victory, servers.get(v).udpPort));
                isLeader = true;
            }
        } else {
            System.out.println("Seems nobody alive bigger then me, turning into lead\n");
            servers.keySet().stream()
                    .filter((v) -> servers.get(v).state == 1 || servers.get(v).status == 2)
                    .forEach((v) -> UDPClient.request(victory, servers.get(v).udpPort));
            isLeader = true;

        }
    }

    public void shutdown() {
        udpServer.stopServer();
    }

}