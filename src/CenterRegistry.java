import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class CenterRegistry {
    private static HashMap<String,String[]> servers = new HashMap<>();
    /*
    Exchange between clients and this server due to simplification for UDP implementation uses Strings,
    parameters are encoded within String using ":" as splitter of first level and ";" as splitter of second level.
    e.g.:
    [[1,2,3,4],[5,6,7,8,]] will be encoded/decoded as 1:2:3:4;5:6:7:8

     */


    public static String register(String config){
        String result = "";
        String[] configFields = config.split(":");
        String[] address = {configFields[1], configFields[2]};
        servers.put(configFields[0], address);
        result=configFields[0]+ " successfully registered";
        System.out.printf(configFields[0]+ " successfully registered\n");
        return result;
    }

    public static String unRegister(String name){
        servers.remove(name);
        System.out.println("Stop:" + name + " Server Successfully!");
        return "Stop:" + name + " Server Successfully!";
    }

    public static String getServers() {
        String result = "";
        for (String key:servers.keySet()){
            result+=key;
            for (String param:servers.get(key)){
                result+=":"+param;
            }
            result+=";";
        }
        System.out.println(result+"\n");
        return result;
    }

    public static void main(String[] args){
//        try {
//            rmiRegistry = LocateRegistry.createRegistry(2000);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
            /*
            From here we don't have to do anything with rmiregistry binding, it's done on server implementation side.
             */
        UDPRegistryServer udpserver = new UDPRegistryServer(8190);
        Thread thread = new Thread(udpserver);
        thread.start();

        System.out.printf(" centerRegistry is waiting for servers requests\n");
        System.out.println("press stop to shut down!");
        Scanner scanner = new Scanner(System.in);
        if (scanner.nextLine().equals("stop")){
            //try {
                //UnicastRemoteObject.unexportObject(rmiRegistry,true);
                udpserver.stopServer();
//            } catch (NoSuchObjectException e) {
//                e.printStackTrace();
//            }
        }
    }

}
