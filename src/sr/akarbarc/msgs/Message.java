package sr.akarbarc.msgs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ola on 07.01.16.
 */
public class Message {
    private Type type;

    public Message(Type type) {
        this.type = type;
    }

    public Message(String data) {
        try {
            JSONObject json = new JSONObject(data);
            type = Type.getType(json.getInt("type"));
        } catch (JSONException e) {
            type = Type.INVALID;
        }
    }

    public Type getType() {
        return type;
    }

    @Override
    public final String toString() {
        JSONObject obj = new JSONObject();
        setData(obj);
        return obj.toString();
    }

    protected void setData(JSONObject obj) {
        obj.put("type", type.getNum());
    }
}
