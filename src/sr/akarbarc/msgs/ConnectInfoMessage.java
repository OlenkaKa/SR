package sr.akarbarc.msgs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ola on 07.01.16.
 */
public class ConnectInfoMessage extends Message {
    private String id;
    private int port;
    List<Connection> connections;

    public class Connection {
        private String id;
        private boolean isIncomming;
        private String ip;
        private int port;
    }

    public ConnectInfoMessage(Type type, String id, int port) {
        super(type);
        this.id = id;
        this.port = port;
        connections = new ArrayList<>();
    }

    public void addConnection(String id, boolean isIncomming, String ip, int port) {
        Connection connection = new Connection();
        connection.id = id;
        connection.isIncomming = isIncomming;
        connection.ip = ip;
        connection.port = port;
        connections.add(connection);
    }

    @Override
    protected void setData(JSONObject obj) {
        super.setData(obj);
        obj.put("id", id);
        obj.put("port", port);
        JSONArray jTable = new JSONArray();
        for (Connection elem: connections) {
            JSONObject jElem = new JSONObject();
            jElem.put("id", elem.id);
            jElem.put("isIncomming", elem.isIncomming);
            jElem.put("ip", elem.ip);
            jElem.put("port", elem.port);
            jTable.put(jElem);
        }
        //obj.put("connections", jTable);
    }
}
