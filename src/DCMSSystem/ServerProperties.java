package DCMSSystem;
import java.util.Date;

public class ServerProperties {
    public int udpPort;
    //since we are hardocding, we assume we use localhost everywhere
    public String hostName="localhost";
    public int pid;
    public int state;
    public int status;
    public Date lastHB;
    public String replicaGroup;

    public ServerProperties(int udpPort, String replicaGroup){
        this.udpPort = udpPort;
        this.replicaGroup = replicaGroup;
    }
}
