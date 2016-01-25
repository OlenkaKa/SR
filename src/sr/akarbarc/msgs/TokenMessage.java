package sr.akarbarc.msgs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ola on 25.01.16.
 */
public class TokenMessage extends Message {
    private String dst_id;
    private List<Member> table = new ArrayList<>();

    public static String checkDestination(String data) {
        JSONObject json = new JSONObject(data);
        return json.isNull("dst_id") ? null : json.getString("dst_id");
    }

    public TokenMessage(Type type) {
        super(type);
    }

    public TokenMessage(String data) {
        super(data);
        JSONObject json = new JSONObject(data);
        try {
            dst_id = json.getString("dst_id");
            JSONArray jsonTable = json.getJSONArray("table");
            for (int i = 0; i < jsonTable.length(); ++i) {
                JSONObject obj = jsonTable.getJSONObject(i);
                Member cell = new Member();
                cell.id = obj.getString("id");
                cell.r = obj.getInt("r");
                cell.g = obj.getInt("g");
                table.add(cell);
            }
        } catch (JSONException e) {
            type = Type.INVALID;
        }
    }

    public void setDstId(String dstId) {
        this.dst_id = dstId;
    }

    public void addTableCell(String id, int r, int g) {
        Member cell = new Member();
        cell.id = id;
        cell.r = r;
        cell.g = g;
        table.add(cell);
    }

    @Override
    protected void setData(JSONObject obj) {
        super.setData(obj);
        obj.put("dst_id", dst_id);
        JSONArray jsonArray = new JSONArray();
        for (Member cell : table) {
            JSONObject cellObj = new JSONObject();
            cellObj.put("id", cell.id);
            cellObj.put("r", cell.r);
            cellObj.put("g", cell.g);
            jsonArray.put(cellObj);
        }
        obj.put("table", jsonArray);
    }
}


class Member {
    String id;
    int r;
    int g;
}
