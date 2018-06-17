
import CenterServerOrb.CenterServer;
import CenterServerOrb.CenterServerHelper;
import CenterServerOrb.CenterServerPackage.except;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
public class ConcurrentClient {
    public static String testMultiThreadRecordId="";
    public static String testMultiThreadserverName="";

    public void run(String[] args) throws Exception {
        new ConcurrentClient().scan(args);
    }

    public void scan(String[] args) throws Exception {
        File file=new File("src/operation.txt");
        InputStreamReader reader=new InputStreamReader(new FileInputStream(file));
        BufferedReader input=new BufferedReader(reader);
        String line=input.readLine();

        // create and initialize the ORB
        ORB orb = ORB.init(args, null);
        // get the root naming context
        org.omg.CORBA.Object objRef =
                orb.resolve_initial_references("NameService");
        // Use NamingContextExt instead of NamingContext,
        // part of the Interoperable naming Service.
        NamingContextExt ncRef =
                NamingContextExtHelper.narrow(objRef);

        while (line!=null){
            String[] parameters=line.split("\\|");
            if (!verifyId(parameters[0])){
                line=input.readLine();
                continue;
            }
            CenterServer service= CenterServerHelper.narrow(ncRef.resolve_str(parameters[0].substring(0,3)));
            CompletableFuture<String> result;
            switch (parameters[1]) {
                case "createTRecord": {
                    testMultiThreadserverName=parameters[0].substring(0,3);
                    result=createTRecord(service,parameters);
                    result.thenAccept(s-> {
                        System.out.println("Create Successful, recordId is "+s);
                        testMultiThreadRecordId=s;
                        }
                    );
                    break;
                }
                case "createSRecord": {
                    testMultiThreadserverName=parameters[0].substring(0,3);
                    result=createSRecord(service,parameters);
                    result.thenAccept(s-> {
                        System.out.println("Create Successful, recordId is "+s);
                        testMultiThreadRecordId=s;
                    });
                    break;
                }
                case "getRecordCounts": {
                    result=getRecordCounts(service,parameters[0]);
                    result.thenAccept(s-> System.out.println("Record number is "+s));
                    break;
                }
                case "editRecord": {
                    result=editRecord(service, parameters);
                    result.thenAccept(System.out::println);
                    break;
                }
                case "Exit": {
                    System.out.println("GoodBye.");
                    break;
                }
            }
            line=input.readLine();
        }
    }

    private CompletableFuture<String> createTRecord(CenterServer stub, String[] parameters)  {
        String firstName = parameters[2];
        String lastName = parameters[3];
        String address = parameters[4];
        String specialiazation=parameters[5];
        String location = parameters[6];
        String phone =  parameters[7];
        CompletableFuture<String> future=new CompletableFuture<>();
        new Thread(()->{
            String recordId=stub.createTRecord(parameters[0], firstName, lastName, address, phone, specialiazation, location);
            future.complete(recordId);
        }).start();
        return future;
    }

    private CompletableFuture<String> createSRecord(CenterServer stub, String[] parameters) {
        String firstName = parameters[2];
        String lastName = parameters[3];
        String status = parameters[4];
        String statusDate = parameters[5];
        String[] coursesRegistered=parameters[6].split(" ");
        CompletableFuture<String> future=new CompletableFuture<>();
        new Thread(()->{
            String recordId=stub.createSRecord(parameters[0], firstName, lastName, coursesRegistered, status, statusDate);
            future.complete(recordId);
        }).start();
        return future;
    }

    public CompletableFuture<String> getRecordCounts(CenterServer stub, String managerId) {
        CompletableFuture<String> future=new CompletableFuture<>();
        new Thread(()->{
            String counts=stub.getRecordCounts(managerId);
            future.complete(counts);
        }).start();
        return future;
    }

    private CompletableFuture<String> editRecord(CenterServer stub, String[] parameters) throws Exception {
        String recordId = parameters[2];
        String fieldName = parameters[3];
        String newValue = parameters[4];
        CompletableFuture<String> future=new CompletableFuture<>();
        new Thread(()->{
            String result= null;
            try {
                result = stub.editRecord(parameters[0], recordId, fieldName, newValue);
            } catch (CenterServerOrb.CenterServerPackage.except except) {
                except.printStackTrace();
            }
            future.complete(result);
        }).start();
        return future;
    }

    private boolean verifyId(String managerId) {
        String addr = managerId.substring(0, 3);
        return addr.equals("MTL") || addr.equals("LVL") || addr.equals("DDO");
    }
}
