package DCMSSystem.FrontEnd;// A server for the Hello object

import DCMSSystem.CenterServerOrb.CenterService;
import DCMSSystem.CenterServerOrb.CenterServiceHelper;
import org.omg.CosNaming.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.POA;

public class FrontEnd {

    public static void main(String args[]) {
        try{
            // create and initialize the ORB
            ORB orb = ORB.init(args, null);

            // get reference to rootpoa & activate the POAManager
            POA rootpoa =
                    (POA)orb.resolve_initial_references("RootPOA");
            rootpoa.the_POAManager().activate();

            // create servant and register it with the ORB
            FrontEndImpl frontEndImpl =new FrontEndImpl();
            frontEndImpl.setORB(orb);

            // get object reference from the servant
            org.omg.CORBA.Object ref =
                    rootpoa.servant_to_reference(frontEndImpl);
            // and cast the reference to a CORBA reference
            CenterService href = CenterServiceHelper.narrow(ref);

            // get the root naming context
            // NameService invokes the transient name service
            org.omg.CORBA.Object objRef =
                    orb.resolve_initial_references("NameService");
            // Use NamingContextExt, which is part of the
            // Interoperable Naming Service (INS) specification.
            NamingContextExt ncRef =
                    NamingContextExtHelper.narrow(objRef);

            // bind the Object Reference in Naming
            String name = "FrontEndImpl";
            NameComponent path[] = ncRef.to_name( name );
            ncRef.rebind(path, href);

            System.out.println
                    ("FrontEndImpl ready and waiting ...");

            // wait for invocations from clients
            orb.run();
        }

        catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }

        System.out.println("FrontEndImpl Exiting ...");

    } //end main
} // end class
