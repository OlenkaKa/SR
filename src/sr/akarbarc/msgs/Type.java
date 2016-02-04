package sr.akarbarc.msgs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ola on 07.01.16.
 */
public enum Type {
    INVALID(-1),
    PING(0),
    JOIN_NETWORK_REQ(1),
    JOIN_NETWORK_RESP(2),
    HELLO(3),
    DISCONNECT(4),
    TOKEN_REQ(5),
    TOKEN_BUSY(6),
    TOKEN(7),
    ELECTION_REQ(10),
    ELECTION_FINISHED(12);

    private int typeNum;

    Type(int typeNum) {
        this.typeNum = typeNum;
    }

    public int getNum() {
        return typeNum;
    }

    public static Type getType(int num) {
        for(Type type: Type.values())
            if(type.typeNum == num)
                return type;
        return INVALID;
    }

    public static Type getType(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        try {
            return getType(obj.getInt("type"));
        } catch (JSONException e) {
            return INVALID;
        }
    }
}
