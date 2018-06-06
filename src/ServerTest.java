// A sample Java IDL object client application.
import CenterServer.CenterService;
import CenterServer.CenterServiceHelper;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;

public class ServerTest
{
    static CenterService service;

    public static void main(String args[]){
        try{
            // create and initialize the ORB
            ORB orb = ORB.init(args, null);

            // get the root naming context
            org.omg.CORBA.Object objRef =
                    orb.resolve_initial_references("NameService");
            // Use NamingContextExt instead of NamingContext,
            // part of the Interoperable naming Service.
            NamingContextExt ncRef =
                    NamingContextExtHelper.narrow(objRef);

            // resolve the Object Reference in Naming
            String name = "CenterService";
            service =
                    CenterServiceHelper.narrow(ncRef.resolve_str(name));

            System.out.println
                    ("Obtained a handle on server object: "
                            + service);
            System.out.println(service.createTRecord("MTL0000","asdfa","dfaf","dfaf","dfas","dfaf","dsfa"));
            service.shutdown();

        }
        catch (Exception e) {
            System.out.println("ERROR : " + e) ;
            e.printStackTrace(System.out);
        }
    } //end main

} // end class
