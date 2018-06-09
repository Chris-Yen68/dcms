import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class test {
    public static void main(String[] args) throws RemoteException {
        //String result = "MTL:" + UDPClient.request("getCount","localhost",8180) + ", LVL:" + UDPClient.request("getCount","localhost",8181) +", DDO:" +UDPClient.request("getCount","localhost",8182);
        String result = "MTL:" + UDPClient.request("getCount","localhost",8180);
        System.out.println(result);
    }
}
