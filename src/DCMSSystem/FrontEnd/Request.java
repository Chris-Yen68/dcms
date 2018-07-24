package DCMSSystem.FrontEnd;

import DCMSSystem.ServerProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable {
    public int id;
    public Map<String,ServerProperties> leaders;
    public HashMap<String, String> params;

    public Request(HashMap<String, String> params) {
        this.params = params;
    }

    public Request(Map<String,ServerProperties> leaders, HashMap<String, String> params) {

        this.leaders = leaders;
        this.params = params;
    }

    public Request() {

    }
}
