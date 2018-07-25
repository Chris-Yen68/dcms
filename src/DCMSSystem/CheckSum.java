package DCMSSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class CheckSum implements Runnable {
    private HashMap<Integer,LinkedList<String>> sentPacket;
    private CenterServer centerServer;

    public CheckSum(CenterServer centerServer) {
        this.centerServer = centerServer;
        sentPacket = centerServer.getSentPacket();
    }

    @Override
    public void run() {
        while (centerServer.servers.get(centerServer.getCenterName()).state == 1 && !sentPacket.isEmpty()) {
            for (Map.Entry<String, ServerProperties> entry : centerServer.servers.entrySet()) {
                if (entry != null) {
                    if (entry.getValue().state == 0 && sentPacket.get(entry.getKey()) != null) {
                        for (LinkedList<String> strings : sentPacket.values()) {
                            if (strings.equals(entry.getKey())) {
                                strings.remove(strings);
                            }
                        }
                    }
                }

            }
        }
    }
}
