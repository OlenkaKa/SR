package sr.akarbarc.msgs;

import org.json.JSONObject;

/**
 * Created by ola on 07.01.16.
 */
public class IdMessage extends Message {
    private String id;
    private int port;

    public IdMessage(Type type, String id, int port) {
        super(type);
        this.id = id;
        this.port = port;
    }

    public IdMessage(String data) {
        super(data);
        JSONObject json = new JSONObject();
        id = json.getString("id");
        port = json.getInt("port");
    }

    public String getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    @Override
    protected void setData(JSONObject obj) {
        super.setData(obj);
        obj.put("id", id);
        obj.put("port", port);
    }
}
