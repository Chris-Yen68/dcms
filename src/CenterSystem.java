import CenterServerOrb.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;



import java.beans.*;
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

public class CenterSystem extends CenterServerPOA {
    private ORB orb;
    public void setORB(ORB orb_val) {
        orb = orb_val;
    }


    private String centerName = "";
    protected HashMap<Character,ArrayList<Records>> database = new HashMap<>();
    private UDPServer udpServer;
    private static Registry centerRegistry;
    private static int randomId=9999;
    private int portNumber;
    private String centerRegistryHost;
    private int centerRegistryUDPPort;
    private Thread thread;


    static {
        try {
            centerRegistry = LocateRegistry.getRegistry("localhost", 2000);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int getPortNumber() {
        return portNumber;
    }


    public CenterSystem(String centerName,int portNumber, String centerRegistryHost, int centerRegistryUDPPort) throws Exception {
        super();
        this.portNumber = portNumber;
        this.centerRegistryHost=centerRegistryHost;
        this.centerRegistryUDPPort=centerRegistryUDPPort;
        this.centerName=centerName;
        udpServer = new UDPServer(portNumber,this);
        thread = new Thread(udpServer);
        thread.start();
        UDPClient.request("register:"+centerName+":"+InetAddress.getLocalHost().getHostName()+":"+portNumber,centerRegistryHost, centerRegistryUDPPort);

    }

    public UDPServer getUdpServer() {
        return udpServer;
    }

    public void setUdpServer(UDPServer udpServer) {
        this.udpServer = udpServer;
        new Thread(udpServer).start();
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

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

    public String createTRecord(String managerId, String firstName, String lastName, String address, String phone, String specialization, String location) {
        TeacherRecord teacherRecord = new TeacherRecord(firstName, lastName, address, phone, specialization, location);
        char key = lastName.charAt(0);
        validateRecordId(teacherRecord, key);
        synchronized (this) {
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
        Log.log(Log.getCurrentTime(), managerId, "createTRecord", "Create successfully! Record ID is "+teacherRecord.getRecordID());
        return teacherRecord.getRecordID();
    }

    public String createSRecord(String managerId, String firstName, String lastName, String[] courseRegistered, String status, String statusDate) {
        StudentRecord studentRecord = new StudentRecord(firstName, lastName, courseRegistered, status, statusDate);
        char key = lastName.charAt(0);
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
        Log.log(Log.getCurrentTime(), managerId, "createSRecord", "Create successfully! Record ID is "+studentRecord.getRecordID());
        return studentRecord.getRecordID();
    }

    public String getRecordCounts(String managerId) {
        String result = "";
        String reply  = UDPClient.request("getservers",centerRegistryHost,centerRegistryUDPPort);
        String[] serverList = reply.split(";");
        for (String server : serverList){
            String[] serverParams = server.split(":");
            System.out.println(Arrays.toString(serverParams));
            result+=serverParams[0]+":"+UDPClient.request("getCount",serverParams[1],Integer.parseInt(serverParams[2]))+" ";
        }
        System.out.printf("\n"+result);
        //Log.log(Log.getCurrentTime(),centerName,managerId,"getRecordCounts, Successful");
        return result;
    }

    public int getLocalRecordCount() throws RemoteException {
        int sum=0;
        for (ArrayList<Records> records:
             database.values()) {
            sum+=records.size();
        }
        return sum;
    }

    public String editRecord(String managerId, String recordID, String fieldName, String newValue) {
        String result = "";
        Boolean ableModified = true;
        BeanInfo recordInfo;

        for (char key : database.keySet()) {
            for (Records record : database.get(key)) {
                if (record.getRecordID().equals(recordID)) {
                    /* following reads information about the object, more precisely of its class, into BeanInfo
                     not sure if in this particular case it will get proper specific class of record, eg StudentRecord or
                     Teacher record, it could take its superclass Record...
                    */
                    try {
                         recordInfo = Introspector.getBeanInfo(record.getClass());
                    } catch (Exception e)
                    {
                        return e.getMessage();
                    }

                    /*
                    recordPds in this case is the array of properties available in this class
                     */
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
                                } catch (Exception e){
                                    return e.getMessage();
                                }
                                result = "Record updated";
                                String operation = "edit: " + prop.getName();
                                Log.log(Log.getCurrentTime(), managerId, operation, result);
                                return result;
                            } else {
                                String operation = "edit: " + prop.getName();
                                result = "The newValue is not valid!";
                                Log.log(Log.getCurrentTime(), managerId, operation, result);
                                return result;
                            }
                        }

                    }
                    result = "fieldName doesn't match record type";
                    String operation = "edit: " + fieldName;
                    Log.log(Log.getCurrentTime(), managerId, operation, result);
                } else{
                    result = "No such record Id for this manager";
                    Log.log(Log.getCurrentTime(), managerId, "edit: " + fieldName, result);
                }
            }
        }
        return result;
    }

    public void shutdown() {
        orb.shutdown(false);
    }

//    public  void stopServer(){
//        UDPClient.request("unregister:"+centerName,centerRegistryHost,centerRegistryUDPPort);
//        try {
//            udpServer.stopServer();
//            centerRegistry.unbind(centerName);
//            UnicastRemoteObject.unexportObject(this,true);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        } catch (NotBoundException e) {
//            e.printStackTrace();
//        }
//    }

}
