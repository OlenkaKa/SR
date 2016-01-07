package sr.akarbarc.msgs;

/**
 * Created by ola on 07.01.16.
 */
public enum Type {
    TRACKER_PING(0),
    NODE_WANT_TO_CONNECT_NETWORK(1),
    TRACKER_REQUEST_NODE_TO_CONNECT(2),
    NODE_ID(3),
    NODE_DISCONNECT(4),
    NODE_WANT_TO_GET_RESOURCE(5),
    NODE_RESOURCE_RECEIVED(6),
    NODE_ACCEPT_TO_GET_RESOURCE(7),
    NODE_CONNECTION_LIST(8),
    NODE_ELECT(9),
    NODE_WANT_TO_ELECT(10),
    TRACKER_AGREEMENT_TO_ELECT(11),
    NODE_ELECTION_FINISHED(12);

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
        return null;
    }
}
