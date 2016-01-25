package sr.akarbarc.ricartagrawala;

import sr.akarbarc.msgs.TokenMessage;
import sr.akarbarc.msgs.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ola on 23.01.16.
 */
public class Token {
    private List<Member> table = new ArrayList<>();

    public Token() {
    }

    public TokenMessage createTokenMessage(String lastId) {
        TokenMessage msg = new TokenMessage(Type.TOKEN);
        // TODO
        return msg;
    }

    public synchronized void addMember(String id) {
        Member member = new Member(id);
        table.add(member);
    }

    public synchronized void removeMember(String id) {
        for (Member member: table)
            if (member.id.equals(id)) {
                table.remove(member);
                return;
            }
    }

    public synchronized void setMemberR(String id, int r) {
        table.stream().filter(cell -> cell.id.equals(id)).forEach(cell -> cell.r = r);
    }

    public synchronized void setMemberG(String id, int g) {
        table.stream().filter(cell -> cell.id.equals(id)).forEach(cell -> cell.g = g);
    }
}

class Member {
    String id;
    int r = 0;
    int g = 0;

    Member(String id) {
        this.id = id;
    }
}
