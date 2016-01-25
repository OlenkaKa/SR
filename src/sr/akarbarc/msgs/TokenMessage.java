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
    private List<TableElem> table = new ArrayList<>();

    public class TableElem {
        private String id;
        private int r;
        private int g;

        public String getId() {
            return id;
        }

        public int getR() {
            return r;
        }

        public int getG() {
            return g;
        }
    }

    public static String getDestination(String data) {
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
                TableElem elem = new TableElem();
                elem.id = obj.getString("id");
                elem.r = obj.getInt("r");
                elem.g = obj.getInt("g");
                table.add(elem);
            }
        } catch (JSONException e) {
            type = Type.INVALID;
        }
    }

    public String getDstId() {
        return dst_id;
    }

    public List<TableElem> getTable() {
        return table;
    }

    public void setDstId(String dstId) {
        this.dst_id = dstId;
    }

    public void addTableElem(String id, int r, int g) {
        TableElem elem = new TableElem();
        elem.id = id;
        elem.r = r;
        elem.g = g;
        table.add(elem);
    }

    @Override
    protected void setData(JSONObject obj) {
        super.setData(obj);
        obj.put("dst_id", dst_id);
        JSONArray jTable = new JSONArray();
        for (TableElem elem: table) {
            JSONObject jElem = new JSONObject();
            jElem.put("id", elem.id);
            jElem.put("r", elem.r);
            jElem.put("g", elem.g);
            jTable.put(jElem);
        }
        obj.put("table", jTable);
    }
}
