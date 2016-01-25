package sr.akarbarc.msgs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ola on 25.01.16.
 */
public class TokenReqMessage extends Message {
    private String id;
    private int r;

    public TokenReqMessage(Type type, String id, int r) {
        super(type);
        this.id = id;
        this.r = r;
    }

    public TokenReqMessage(String data) {
        super(data);
        try {
            JSONObject obj = new JSONObject(data);
            id = obj.getString("id");
            r = obj.getInt("r");
        } catch (JSONException e) {
            type = Type.INVALID;
        }
    }

    public String getId() {
        return id;
    }

    public int getR() {
        return r;
    }

    @Override
    protected void setData(JSONObject obj) {
        super.setData(obj);
        obj.put("id", id);
        obj.put("r", r);
    }
}
