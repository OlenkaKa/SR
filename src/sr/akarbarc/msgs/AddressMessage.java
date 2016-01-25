package sr.akarbarc.msgs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ola on 07.01.16.
 */
public class AddressMessage extends Message {
    private String id;
    private String ip;
    private int port;

    public AddressMessage(String data) {
        super(data);
        JSONObject json = new JSONObject(data);
        try {
            id = json.getString("id");
        } catch (JSONException e) {
            type = Type.INVALID;
            return;
        }
        ip = json.isNull("ip") ? null : json.getString("ip");
        port = json.isNull("port") ? -1 : json.getInt("port");
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
