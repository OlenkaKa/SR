package sr.akarbarc.msgs;

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
    TOKEN_RECEIVED(6),
    TOKEN_ACCEPT_RESP(7),
    CONNECTIONS_INFO(8),
    ELECTION_LEADER(9),
    ELECTION_REQ(10),
    ELECTION_ACCEPT_RESP(11),
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
}
