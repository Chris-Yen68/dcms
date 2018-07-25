package DCMSSystem.Client;

import DCMSSystem.CenterServerOrb.CenterService;
import DCMSSystem.CenterServerOrb.CenterServiceHelper;
import DCMSSystem.CenterServerOrb.CenterServicePackage.except;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class InteractonClient {
    public static void main(String args[]) throws Exception {

        /*
        using scanner get manager name, check if subs(0,3) within MTL LVL DDO
        set serverName based on that
        */
        /*
        some method calls part
        in form of:
        mtlServer.createTRecord(a,b,c,d...)
        probably client should be interactive, and consume input with Scanner
        based on which we should make case{} for called methods
         */
        // create and initialize the ORB
        ORB orb = ORB.init(args, null);
        // get the root naming context
        org.omg.CORBA.Object objRef =
                orb.resolve_initial_references("NameService");
        // Use NamingContextExt instead of NamingContext,
        // part of the Interoperable naming Service.
        NamingContextExt ncRef =
                NamingContextExtHelper.narrow(objRef);

//        new DataSeedingClient().scan(ncRef);
        new InteractonClient().scan(ncRef);
    }

    public void scan(NamingContextExt ncRef) throws Exception {

        CenterService service;

        String managerId = "";
        boolean ifContinue = true;
        do {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Please input your manager ID:");
            managerId = scanner.nextLine();
            if (!verifyId(managerId)) {
                System.out.println("ManagerId error. Please input again");
                continue;
            }
            service = CenterServiceHelper.narrow(ncRef.resolve_str("FrontEndImpl"));
            ifContinue = processOperation(service,managerId);

        } while (ifContinue);
    }

    public boolean processOperation(CenterService stub, String managerId) throws Exception {
        Scanner scanner = new Scanner(System.in);
        int option = -1;
        System.out.println("Please select your operation:");
        System.out.println("1> Create Teacher Record.");
        System.out.println("2> Create Student Record.");
        System.out.println("3> Get Record Counts.");
        System.out.println("4> Edit Record.");
        System.out.println("5> transferRecord.");
        System.out.println("6> Test concurrently edit and transfer the same record.");
        System.out.println("7> Exit.");
        option = scanner.nextInt();
        switch (option) {
            case 1: {
                createTRecord(stub,managerId);
                break;
            }
            case 2: {
                createSRecord(stub,managerId);
                break;
            }
            case 3: {
                getRecordCounts(stub,managerId);
                break;
            }
            case 4: {
                editRecord(stub, managerId);
                break;
            }
            case 5:
                this.transferRecord(stub, managerId);
                break;
            case 6:
                this.testMultiThread(stub, managerId);
                break;
            case 7: {
                System.out.println("GoodBye.");
                break;
            }
        }
        if (option == 7) return false;
        else return true;
    }

    public void createTRecord(CenterService stub, String managerId) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input teacher's first name:");
        String firstName = scanner.nextLine().trim();
        System.out.println("Please input teacher's last name:");
        String lastName = scanner.nextLine().trim();
        System.out.println("Please input teacher's address");
        String address = scanner.nextLine().trim();
        System.out.println("Please input your teacher's specialization:");
        String specialiazation=scanner.nextLine().trim();
        System.out.println("Please input teacher's location:");
        String location = scanner.nextLine().trim();
        System.out.println("Please input teacher's phone:");
        String phone =  scanner.nextLine();
        String result=stub.createTRecord(managerId, firstName, lastName, address, phone, specialiazation, location);
        DataSeedingClient.recordForTestMultiThread.put(result, managerId.substring(0,3));
        System.out.println("Teacher record with id: "+result+" was created");
    }

    public void createSRecord(CenterService stub, String managerId) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input student's first name:");
        String firstName = scanner.nextLine().trim();
        System.out.println("Please input student's last name:");
        String lastName = scanner.nextLine().trim();
        System.out.println("Please input student's status:");
        String status = scanner.nextLine().trim();
        System.out.println("Please input your student's statusDate:");
        String statusDate = scanner.nextLine().trim();
        System.out.println("Please input student's courses(split with space):");
        String[] coursesRegistered=scanner.nextLine().split(" ");
        String result = stub.createSRecord(managerId, firstName, lastName, coursesRegistered, status, statusDate);
        DataSeedingClient.recordForTestMultiThread.put(result, managerId.substring(0,3));
        System.out.println("Student record with id: "+result+" was created");
    }

    public void getRecordCounts(CenterService stub, String managerId) {
        System.out.println(stub.getRecordCounts(managerId));
    }

    public void editRecord(CenterService stub, String managerId) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input your record id:");
        String recordId = scanner.nextLine().trim();
        System.out.println("Please input the field name you want to change:");
        String fieldName = scanner.nextLine().trim();
        System.out.println("Please input new value:");
        String newValue = scanner.nextLine().trim();
        String result = stub.editRecord(managerId, recordId, fieldName, newValue);
        System.out.printf(result+"\n");

    }

    public void transferRecord(CenterService stub, String managerId) throws except {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input the transfer record id:");
        String recordId = scanner.nextLine().trim();
        System.out.println("Please input the destination to transfer:");
        String centerName = scanner.nextLine().trim();
        String result = stub.transferRecord(managerId,recordId,centerName);
        DataSeedingClient.recordForTestMultiThread.put(recordId, centerName);
        System.out.println(result);
    }

    public void testMultiThread(CenterService stub, String managerId) {
        String serverName=managerId.substring(0,3);
        System.out.println("The record id list below are belongs to your server. Please input the record id as your test case.");
        for (Map.Entry<String, String> entry: DataSeedingClient.recordForTestMultiThread.entrySet()
                ) {
            if(entry.getValue().equals(serverName)){
                System.out.println(entry.getKey());
            }
        }
        Scanner scanner=new Scanner(System.in);
        String recordId=scanner.nextLine().trim();
        System.out.println("Please input the field name you want to change:");
        String fieldName = scanner.nextLine().trim();
        System.out.println("Please input new value:");
        String newValue = scanner.nextLine().trim();
        System.out.println("Please input the destination to transfer:");
        String centerName = scanner.nextLine().trim();
        CompletableFuture<String> edit=new CompletableFuture<>();
        new Thread(()->{
            String result= null;
            try {
                result = stub.editRecord(managerId,recordId,fieldName,newValue);
                edit.complete(result);
            } catch (DCMSSystem.CenterServerOrb.CenterServicePackage.except except) {
                except.printStackTrace();
            }
        }).start();

        CompletableFuture<String> transfer=new CompletableFuture<>();
        new Thread(()->{
            String result = null;
            try {
                result = stub.transferRecord(managerId, recordId,centerName);
                transfer.complete(result);

            } catch (DCMSSystem.CenterServerOrb.CenterServicePackage.except except) {
                except.printStackTrace();
            }
        }).start();

        edit.thenAccept(s-> System.out.println(s));
        transfer.thenAccept(s-> {
            System.out.println(s);
            DataSeedingClient.recordForTestMultiThread.put(s, centerName);
        });
    }

    public boolean verifyId(String managerId) {
        if (managerId.length()<7){
            return false;
        }
        String addr = managerId.substring(0, 3);
        if (addr.equals("MTL") || addr.equals("LVL") || addr.equals("DDO")) {
            return true;
        } else {
            return false;
        }
    }
}
