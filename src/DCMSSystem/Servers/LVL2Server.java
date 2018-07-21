package DCMSSystem.Servers;

import DCMSSystem.CenterServer;

import java.lang.management.ManagementFactory;
import java.util.Scanner;

public class LVL2Server {

    public static void main(String args[]) throws Exception {
        //Some hardcoded parameters
        String serverName = "LVL2";
        int pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        System.out.println("this server PID: "+pid);

        CenterServer server = new CenterServer(serverName, 8161, pid);

        System.out.println(serverName+" is launched");
        System.out.println("input s to shut down!");
        Scanner scanner = new Scanner(System.in);
        if (scanner.nextLine().equals("s")){
            server.shutdown();
            System.exit(0);
        }
    }
}
