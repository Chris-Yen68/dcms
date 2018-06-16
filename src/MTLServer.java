import CenterServerOrb.CenterServer;
import CenterServerOrb.CenterServerHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;

import java.util.Scanner;
/*
    Slightly hardcoded server launcher in terms of name, and port values,
    which can be implemented as 1 class with input arguments like "name" "ownPort"...
    But for ease and speed of testing and was decided to sacrifice some beautiness to comfort of usage.
    Still, this is only end point hardcoding, core classes are pretty much in style.

    Initialise ORB object and CenterSystem object, which is actual server implementation.
    Performs ORB bind to orbd.



     */
public class MTLServer {


    public static void main(String args[]) throws Exception {

        //Some hardcoded parameters
        String centerRegistryHost = "localhost";
        int centerRegistryUDPPort = 8190;
        String serverName = "MTL";

        //Some objects creation
        ORB orb = ORB.init(args, null);
        POA rootpoa =
                (POA)orb.resolve_initial_references("RootPOA");
        rootpoa.the_POAManager().activate();

        CenterSystem server = new CenterSystem(serverName, 8180,centerRegistryHost, centerRegistryUDPPort);

        //Maps ORB to CenterSystem reference.
        server.setORB(orb);

        //ORB initialisation and bind part
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
        System.out.println("input s to shut down!");
        Scanner scanner = new Scanner(System.in);
        if (scanner.nextLine().equals("s")){
            server.shutdown();
            System.exit(0);
        }
    }
}
