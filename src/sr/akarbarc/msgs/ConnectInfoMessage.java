package sr.akarbarc.msgs;

import org.json.JSONObject;

/**
 * Created by ola on 07.01.16.
 */
public class ConnectInfoMessage extends Message {
    private String id;
    private int port;

    public ConnectInfoMessage(Type type, String id, int port) {
        super(type);
        this.id = id;
        this.port = port;
    }

    @Override
    protected void setData(JSONObject obj) {
        super.setData(obj);
        obj.put("id", id);
        obj.put("port", port);
    }
}
