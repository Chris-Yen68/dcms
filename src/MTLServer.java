import CenterServerOrb.CenterServer;
import CenterServerOrb.CenterServerHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;

import java.util.Scanner;

public class MTLServer {

    public static void main(String args[]) throws Exception {
        String centerRegistryHost = "localhost";
        int centerRegistryUDPPort = 8190;
        String serverName = "MTL";

        ORB orb = ORB.init(args, null);
        POA rootpoa =
                (POA)orb.resolve_initial_references("RootPOA");
        rootpoa.the_POAManager().activate();

        CenterSystem server = new CenterSystem(serverName, 8180,centerRegistryHost, centerRegistryUDPPort);

        server.setORB(orb);

        org.omg.CORBA.Object ref =
                rootpoa.servant_to_reference(server);
        CenterServer href = CenterServerHelper.narrow(ref);
        org.omg.CORBA.Object objRef =
        orb.resolve_initial_references("NameService");
        NamingContextExt ncRef =
                NamingContextExtHelper.narrow(objRef);
        NameComponent path[] = ncRef.to_name( serverName );
        ncRef.rebind(path, href);


        System.out.println(serverName+" is launched");
        orb.run();
        System.out.println("press stop to shut down!");
        Scanner scanner = new Scanner(System.in);
        if (scanner.nextLine().equals("stop")){
            server.shutdown();
        }
    }
}
