package sr.akarbarc.msgs;

import org.json.JSONObject;

/**
 * Created by ola on 07.01.16.
 */
public class AddressMessage extends Message {
    private String id;
    private String ip;
    private int port;

    public AddressMessage(Type type, String id, String ip, int port) {
        super(type);
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public AddressMessage(String data) {
        super(data);
        JSONObject json = new JSONObject();
        id = json.getString("id");
        ip = json.getString("ip");
        port = json.getInt("port");
    }

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    protected void setData(JSONObject obj) {
        super.setData(obj);
        obj.put("id", id);
        obj.put("ip", ip);
        obj.put("port", port);
    }
}
