package DCMSSystem.Servers;
import DCMSSystem.CenterServer;

import java.lang.management.ManagementFactory;
import java.util.Scanner;

public class DDOServer {

    public static void main(String args[]) throws Exception {
        //Some hardcoded parameters
        String serverName = "DDO";

        int pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        System.out.println(pid);

        System.out.println("this server PID: "+pid);
        CenterServer server = new CenterServer(serverName, 8182, pid);
//        FrontEndImpl.servers.get(serverName).pid=pid;
//        FrontEndImpl.servers.get(serverName).state=1;
        System.out.println(serverName+" is launched");
        System.out.println("input s to shut down!");
        Scanner scanner = new Scanner(System.in);
        if (scanner.nextLine().equals("s")){
            server.shutdown();
            System.exit(0);
        }
    }
}