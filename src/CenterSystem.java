import CenterServerOrb.*;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.Object;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;


import java.beans.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class CenterSystem extends CenterServerPOA {

    /*

    Class which implements CORBA IDL through its framework extending CenterServerPOA.
    Supported methods are as per IDL declaration.

    ORB object is set by launcher (particular server implementation), in which it is actually initialized and bind to orbd.

    Database is implemented by the way of HashMap which puts are synchronised.

    getRecordCounts is implemented using the CenterRegistry which in RMI version of appliction was acting as
    RMIRegistry and UDPRegistry. For CORBA implementation RMI part was stripped off, and registry acts as UDP address/port
    catalog of online servers. Some level of dynamism is supported, since this application can be functional with partially
    started and available servers.

     */

    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    private String centerName;
    protected HashMap<Character, ArrayList<Records>> database = new HashMap<>();
    private UDPServer udpServer;
    private int portNumber;
    private String centerRegistryHost;
    private int centerRegistryUDPPort;
    private Thread thread;
    private ReentrantLock lock = new ReentrantLock();

    /*
     Constructor besides of creating supplementary udp listeners, registers object in CenterRegistry.
      */
    public CenterSystem(String centerName, int portNumber, String centerRegistryHost, int centerRegistryUDPPort) throws Exception {
        super();
        this.portNumber = portNumber;
        this.centerRegistryHost = centerRegistryHost;
        this.centerRegistryUDPPort = centerRegistryUDPPort;
        this.centerName = centerName;
        udpServer = new UDPServer(portNumber, this);
        thread = new Thread(udpServer);
        thread.start();
        UDPClient.request("register:" + centerName + ":" + InetAddress.getLocalHost().getHostName() + ":" + this.portNumber, centerRegistryHost, centerRegistryUDPPort);

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

    /*
    I don't think we need comments over this, code is obvious.
     */
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
        //gets the list of registered servers.
        String reply = UDPClient.request("getservers", centerRegistryHost, centerRegistryUDPPort);
        String[] serverList = reply.split(";");

        //generates result querying servers from list above in parallel
        result = Arrays.stream(serverList).parallel().map((v) ->
        {
            String[] serverParams = v.split(":");
            byte[] getCount = ByteUtility.toByteArray("getCount");
            String result1 = serverParams[0] + ":" + UDPClient.request(getCount, serverParams[1], Integer.parseInt(serverParams[2]));
            System.out.printf("\n"+serverParams[0]+" processed\n");
            return result1;
        }).collect(Collectors.joining(" "));

        System.out.printf("\n" + result);
        Log.log(Log.getCurrentTime(), managerId, "getRecordCounts", "Successful");
        return result;
    }

    /*
    Returns local record count for the particular object instance, which is executed by some instance getRecordCount query.

     */
    public int getLocalRecordCount() throws RemoteException {
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
                                synchronized (database) {
                                    Statement stmt = new Statement(record, prop.getWriteMethod().getName(), new java.lang.Object[]{newValue});
                                    try {
                                        stmt.execute();
                                    } catch (Exception e) {
                                        return e.getMessage();
                                    }
                                    result = "Record updated";
                                }
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
                } else {
                    result = "No such record Id for this manager";
                    Log.log(Log.getCurrentTime(), managerId, "edit: " + fieldName, result);
                }
            }
        }
        return result;
    }
    /*
       transfer record from the server associated with manager if it is varified to the remotecenter which is given by name
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
               check if the id is existing and the remotecenter is valide
             */
            boolean isValidatedCenter = remoteCenterServerName.equals("MTL") || remoteCenterServerName.equals("LVL") || remoteCenterServerName.equals("DDO");
            boolean ableToTransfer = isValidatedCenter && has && !centerName.equals(remoteCenterServerName);
            byte[] serializedMessage = ByteUtility.toByteArray(transferedRecord);
            /*
             using udp to request the function and parse the object to bytes to do the work.
             */
            if (ableToTransfer) {
                String reply = UDPClient.request("getservers", centerRegistryHost, centerRegistryUDPPort);
                String[] serverList = reply.split(";");
                for (String server : serverList) {
                    String[] serverParams = server.split(":");
                    if (serverParams[0].equals(remoteCenterServerName)) {
                        String response = UDPClient.request(serializedMessage, serverParams[1], Integer.parseInt(serverParams[2]));
                        result += response;
                    }
                }
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

    public void shutdown() {

        UDPClient.request("unregister:" + centerName, centerRegistryHost, centerRegistryUDPPort);
        orb.shutdown(false);

    }

}
