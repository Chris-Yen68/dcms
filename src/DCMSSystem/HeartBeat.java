package DCMSSystem;

import DCMSSystem.UDP.UDPClient;

import java.util.Date;
import java.util.HashMap;

public class HeartBeat implements Runnable {
    public HashMap<String, ServerProperties> servers;

    /**
     * here I'm using a serverName as a part of hb-* message string, to determine the source of HB.
     * I'm not sure if we have other means for this, if we run everything on localhost and don't
     * control our outbound port (that's gonna be too much).
     * With HB we spread info about PID of the server.
    **/
    private CenterServer centerServer;
    public Boolean isLeader;

    public HeartBeat(HashMap<String, ServerProperties> servers, CenterServer centerServer) {
        this.servers = servers;
        this.centerServer = centerServer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Date dateNow = new Date();
                long timeNow = dateNow.getTime();
                servers.keySet().stream()
                        //means if leader - send HB to everybody, including FE, else - exclude FE
                        .filter(isLeader ? ((v) -> true) : ((v) -> servers.get(v).status != 2))
                        .forEach((v) -> {
                            ServerProperties server = servers.get(v);
                            //TODO: refactor the var name to make it more readable.
                            if (timeNow - server.lastHB.getTime() > 3000) {
                                if(server.status==1){
                                    centerServer.bullyElect();
                                }
                                server.state = 0;
                            } else {
                                server.state = 1;
                            }
                            UDPClient.heartbit(centerServer.getCenterName(), server.hostName, server.pid, server.udpPort);
                        });

                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
