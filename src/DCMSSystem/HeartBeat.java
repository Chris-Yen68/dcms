package DCMSSystem;

import DCMSSystem.UDP.UDPClient;

import java.util.Date;
import java.util.HashMap;

public class HeartBeat implements Runnable {
    public HashMap<String, ServerProperties> servers;
    private String serverName;

    public HeartBeat(HashMap<String, ServerProperties> servers, String serverName) {
        this.servers = servers;
        this.serverName=serverName;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Date dateNow = new Date();
                long timeNow = dateNow.getTime();
                servers.keySet().stream().forEach((v) -> {
                    ServerProperties server = servers.get(v);
                    if (timeNow - server.lastHB.getTime() > 3000) {
                        server.status = 0;
                    } else {
                        server.status = 1;
                    }
                    UDPClient.heartbit(serverName,server.hostName,server.udpPort);
                });

                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
