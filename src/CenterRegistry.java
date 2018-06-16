import java.util.HashMap;
import java.util.Scanner;

/*
    Manages ip address and udp port of the servers online.

    * Server startup process registers its ip and port
    sending "register" message to the registry. CenterRegistry parses the message and add record to local hashmap.
    * For getRecordsCount registry replies with its database content.
    * Server stop process sends "stop" message to registry, so it removes the record from its database.
    * If server by some reason wasn't stopped correctly it will reside in DB, however, clients querying it, will
    receive "server is offline" notification, due to 2sec timeout.

    Exchange between clients and this server due to simplification for UDP implementation uses Strings,
    parameters are encoded within String using ":" as splitter of first level and ";" as splitter of second level.
    e.g.:
    [[1,2,3,4],[5,6,7,8]] will be encoded/decoded as 1:2:3:4;5:6:7:8

     */
public class CenterRegistry {
    private static HashMap<String, String[]> servers = new HashMap<>();

//register overrides the record in db, in case, there is already orphaned record which wasn't properly unregistered.

    public static String register(String config) {
        String result;
        String[] configFields = config.split(":");
        String[] address = {configFields[1], configFields[2]};
        servers.put(configFields[0], address);
        result = configFields[0] + " successfully registered";
        System.out.printf(configFields[0] + " successfully registered\n");
        return result;
    }

    //assume this method can be called only by registered and online server
    public static String unRegister(String name) {
        servers.remove(name);
        System.out.println(name + " successfully stopped");
        return name + " successfully stopped\n";
    }

    //dumps database by request
    public static String getServers() {
        String result = "";
        for (String key : servers.keySet()) {
            result += key;
            for (String param : servers.get(key)) {
                result += ":" + param;
            }
            result += ";";
        }
        System.out.println(result + "\n");
        return result;
    }

    public static void main(String[] args) {

        int defaultPort = 8190;
        int listenPort = defaultPort;
        if (args.length != 0) {
            if (Integer.parseInt(args[0]) > 8000 && Integer.parseInt(args[0]) < 9000) {
                listenPort = Integer.parseInt(args[0]);
            } else {
                System.out.printf("Wrong argument, usage: \nCenterRegistry <port number> \nwhere port number is between 8000 and 9000 ");
                System.exit(1);
            }
        }

        UDPRegistryServer udpserver = new UDPRegistryServer(listenPort);
        Thread thread = new Thread(udpserver);
        thread.start();

        System.out.printf("CenterRegistry is waiting for servers requests on udp port: "+listenPort+"\n");
        System.out.println("Input s to shut down!\n");
        Scanner scanner = new Scanner(System.in);
        if (scanner.nextLine().equals("s")) {
            udpserver.stopServer();
        }
    }

}
