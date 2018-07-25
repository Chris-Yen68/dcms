package DCMSSystem.Client;

import DCMSSystem.CenterServerOrb.CenterService;
import DCMSSystem.CenterServerOrb.CenterServiceHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
public class DataSeedingClient {
    public static HashMap<String, String> recordForTestMultiThread= new HashMap<String, String>();

    public void scan( NamingContextExt ncRef) throws Exception {
        File file=new File("src/DCMSSystem/operation.txt");
        InputStreamReader reader=new InputStreamReader(new FileInputStream(file));
        BufferedReader input=new BufferedReader(reader);
        String line=input.readLine();

        while (line!=null){
            String[] parameters=line.split("\\|");
            if (!verifyId(parameters[0])){
                line=input.readLine();
                continue;
            }
            String serverName=parameters[0].substring(0,3);
            CenterService service= CenterServiceHelper.narrow(ncRef.resolve_str(serverName));
            CompletableFuture<String> result;
            switch (parameters[1]) {
                case "createTRecord": {
                    result=createTRecord(service,parameters);
                    result.thenAccept(s-> {
                        System.out.println("Create Successful, recordId is "+s);
                        recordForTestMultiThread.put(s, serverName);
                    });
                    break;
                }
                case "createSRecord": {
                    result=createSRecord(service,parameters);
                    result.thenAccept(s-> {
                        System.out.println("Create Successful, recordId is "+s);
                        recordForTestMultiThread.put(s, serverName);
                    });
                    break;
                }
                case "getRecordCounts": {
                    result=getRecordCounts(service,parameters[0]);
                    result.thenAccept(s-> System.out.println("Record number is "+s));
                    break;
                }
            }
            line=input.readLine();
        }
    }

    private CompletableFuture<String> createTRecord(CenterService stub, String[] parameters)  {
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

    private CompletableFuture<String> createSRecord(CenterService stub, String[] parameters) {
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

    public CompletableFuture<String> getRecordCounts(CenterService stub, String managerId) {
        CompletableFuture<String> future=new CompletableFuture<>();
        new Thread(()->{
                String counts=stub.getRecordCounts(managerId);
                future.complete(counts);
        }).start();
        return future;
    }

    private boolean verifyId(String managerId) {
        String addr = managerId.substring(0, 3);
        return addr.equals("MTL") || addr.equals("LVL") || addr.equals("DDO");
    }
}