import CenterServer.CenterService;
import CenterServer.CenterServiceHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Client {
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

        new Client().scan(args);
    }

    public void scan(String[] args) throws Exception {

        // create and initialize the ORB
        ORB orb = ORB.init(args, null);
        // get the root naming context
        org.omg.CORBA.Object objRef =
                orb.resolve_initial_references("NameService");
        // Use NamingContextExt instead of NamingContext,
        // part of the Interoperable naming Service.
        NamingContextExt ncRef =
                NamingContextExtHelper.narrow(objRef);
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
            switch (managerId.substring(0, 3)) {
                case "MTL": {
                    service = CenterServiceHelper.narrow(ncRef.resolve_str("MTL"));
                    ifContinue = processOperation(service,managerId);
                    break;
                }
                case "LVL": {
                    service = CenterServiceHelper.narrow(ncRef.resolve_str("LVL"));
                    ifContinue = processOperation(service,managerId);
                    break;
                }
                case "DDO": {
                    service = CenterServiceHelper.narrow(ncRef.resolve_str("DDO"));
                    ifContinue = processOperation(service,managerId);
                    break;
                }
            }
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
        System.out.println("5> Exit.");
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
            case 5: {
                System.out.println("GoodBye.");
                break;
            }
        }
        if (option == 5) return false;
        else return true;
    }

    public void createTRecord(CenterService stub, String managerId) throws RemoteException {
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
        System.out.println("Teacher record with id: "+result+" was created");
    }

    public void createSRecord(CenterService stub, String managerId) throws RemoteException {
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
        System.out.println("Student record with id: "+result+" was created");
    }

    public void getRecordCounts(CenterService stub, String managerId) throws RemoteException, NotBoundException {
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


    public boolean verifyId(String managerId) throws Exception {
        String addr = managerId.substring(0, 3);
        if (addr.equals("MTL") || addr.equals("LVL") || addr.equals("DDO")) {
            return true;
        } else {
            return false;
        }
    }
}
