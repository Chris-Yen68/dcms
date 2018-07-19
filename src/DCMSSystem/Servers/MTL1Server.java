package DCMSSystem.Servers;

import DCMSSystem.CenterServer;

import java.util.Scanner;

public class MTL1Server {

    public static void main(String args[]) throws Exception {
        //Some hardcoded parameters
        String serverName = "MTL1";
        int pid = Integer.parseInt(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        //Some objects creation
        CenterServer server = new CenterServer(serverName, 8170, pid);
        System.out.println(serverName + " is launched");
        System.out.println("input s to shut down!");
        Scanner scanner = new Scanner(System.in);
        if (scanner.nextLine().equals("s")) {
            server.shutdown();
            System.exit(0);
        }
    }
}
