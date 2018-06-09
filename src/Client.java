//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import CenterServerOrb.CenterServer;
import CenterServerOrb.CenterServerHelper;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.Scanner;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

public class Client {
    public Client() {
    }

    public static void main(String[] args) throws Exception {
        (new Client()).scan(args);
    }

    public void scan(String[] args) throws Exception {
        ORB orb = ORB.init(args, (Properties)null);
        Object objRef = orb.resolve_initial_references("NameService");
        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
        String managerId = "";
        boolean ifContinue = true;

        do {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Please input your manager ID:");
            managerId = scanner.nextLine();
            if (!this.verifyId(managerId)) {
                System.out.println("ManagerId error. Please input again");
            } else {
                String var9 = managerId.substring(0, 3);
                byte var10 = -1;
                switch(var9.hashCode()) {
                    case 67535:
                        if (var9.equals("DDO")) {
                            var10 = 2;
                        }
                        break;
                    case 75778:
                        if (var9.equals("LVL")) {
                            var10 = 1;
                        }
                        break;
                    case 76677:
                        if (var9.equals("MTL")) {
                            var10 = 0;
                        }
                }

                CenterServer service;
                switch(var10) {
                    case 0:
                        service = CenterServerHelper.narrow(ncRef.resolve_str("MTL"));
                        ifContinue = this.processOperation(service, managerId);
                        break;
                    case 1:
                        service = CenterServerHelper.narrow(ncRef.resolve_str("LVL"));
                        ifContinue = this.processOperation(service, managerId);
                        break;
                    case 2:
                        service = CenterServerHelper.narrow(ncRef.resolve_str("DDO"));
                        ifContinue = this.processOperation(service, managerId);
                }
            }
        } while(ifContinue);

    }

    public boolean processOperation(CenterServer stub, String managerId) throws Exception {
        Scanner scanner = new Scanner(System.in);
        int option = 0;
        System.out.println("Please select your operation:");
        System.out.println("1> Create Teacher Record.");
        System.out.println("2> Create Student Record.");
        System.out.println("3> Get Record Counts.");
        System.out.println("4> Edit Record.");
        System.out.println("5> Exit.");
        option = scanner.nextInt();
        switch(option) {
            case 1:
                this.createTRecord(stub, managerId);
                break;
            case 2:
                this.createSRecord(stub, managerId);
                break;
            case 3:
                this.getRecordCounts(stub, managerId);
                break;
            case 4:
                this.editRecord(stub, managerId);
                break;
            case 5:
                System.out.println("GoodBye.");
        }

        return option != 5;
    }

    public void createTRecord(CenterServer stub, String managerId) throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input teacher's first name:");
        String firstName = scanner.nextLine().trim();
        System.out.println("Please input teacher's last name:");
        String lastName = scanner.nextLine().trim();
        System.out.println("Please input teacher's address");
        String address = scanner.nextLine().trim();
        System.out.println("Please input your teacher's specialization:");
        String specialiazation = scanner.nextLine().trim();
        System.out.println("Please input teacher's location:");
        String location = scanner.nextLine().trim();
        System.out.println("Please input teacher's phone:");
        String phone = scanner.nextLine();
        String result = stub.createTRecord(managerId, firstName, lastName, address, phone, specialiazation, location);
        System.out.println("Teacher record with id: " + result + " was created");
    }

    public void createSRecord(CenterServer stub, String managerId) throws RemoteException {
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
        String[] coursesRegistered = scanner.nextLine().split(" ");
        String result = stub.createSRecord(managerId, firstName, lastName, coursesRegistered, status, statusDate);
        System.out.println("Student record with id: " + result + " was created");
    }

    public void getRecordCounts(CenterServer stub, String managerId) throws RemoteException, NotBoundException {
        System.out.println(stub.getRecordCounts(managerId));
    }

    public void editRecord(CenterServer stub, String managerId) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input your record id:");
        String recordId = scanner.nextLine().trim();
        System.out.println("Please input the field name you want to change:");
        String fieldName = scanner.nextLine().trim();
        System.out.println("Please input new value:");
        String newValue = scanner.nextLine().trim();
        String result = stub.editRecord(managerId, recordId, fieldName, newValue);
        System.out.printf(result + "\n");
    }

    public boolean verifyId(String managerId) throws Exception {
        String addr = managerId.substring(0, 3);
        return addr.equals("MTL") || addr.equals("LVL") || addr.equals("DDO");
    }
}
