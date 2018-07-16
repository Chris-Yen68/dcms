package DCMSSystem;
import java.util.Date;

public class ServerProperties {
    public int udpPort;
    //since we are hardocding, we assume we use localhost everywhere
    public final String hostName="localhost";

    //1 = online, 0 = dead... potentially something else
    public int state;

    //1 = leader, 0 = non leader, 2 = FE server, or if you'd like: 1=primary, 0=non primary, 2=front end
    public int status;

    public Date lastHB;

    //assuming we have control over server names replica group name for MTL* servers is MTL, and so on...
    public String replicaGroup;

    public ServerProperties(int udpPort, String replicaGroup){
        this.udpPort = udpPort;
        this.replicaGroup = replicaGroup;
    }
}
