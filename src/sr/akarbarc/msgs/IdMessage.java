package sr.akarbarc.msgs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ola on 10.01.16.
 */
public class IdMessage extends Message {
    private String id;

    public IdMessage(Type type, String id) {
        super(type);
        this.id = id;
    }

    public IdMessage(String data) {
        super(data);
        try {
            JSONObject json = new JSONObject(data);
            id = json.getString("id");
        } catch (JSONException e) {
            type = Type.INVALID;
        }
    }

    public String getId() {
        return id;
    }

    @Override
    protected void setData(JSONObject obj) {
        super.setData(obj);
        obj.put("id", id);
    }
}
