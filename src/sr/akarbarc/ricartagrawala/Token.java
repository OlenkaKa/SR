package sr.akarbarc.ricartagrawala;

import sr.akarbarc.msgs.TokenMessage;
import sr.akarbarc.msgs.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ola on 23.01.16.
 */
public class Token {
    private boolean inUse = true;
    private Member owner;
    private List<Member> members = new ArrayList<>();

    public static Token createToken(TokenMessage msg) {
        Token token = new Token();
        for (TokenMessage.TableElem elem: msg.getTable()) {
            Member member = new Member(elem.getId(), elem.getR(), elem.getG());
            token.members.add(member);
            if (member.id.equals(msg.getDstId()))
                token.owner = member;
        }
        return token;
    }

    public TokenMessage createMessage() {
        TokenMessage msg = new TokenMessage(Type.TOKEN);
        msg.setDstId(owner.id);
        for (Member member: members)
            msg.addTableElem(member.id, member.r, member.g);
        return msg;
    }

    public synchronized void addMember(String id) {
        for (Member member: members) {
            if (member.id.equals(id))
                return;
        }
        Member member = new Member(id);
        members.add(member);
    }

    public synchronized void removeMember(String id) {
        for (Member member: members)
            if (member.id.equals(id)) {
                members.remove(member);
                return;
            }
    }

    public synchronized void setMemberR(String id, int r) {
        members.stream().filter(cell -> cell.id.equals(id)).forEach(cell -> cell.r = r);
    }

    public synchronized void setMemberG(String id, int g) {
        members.stream().filter(cell -> cell.id.equals(id)).forEach(cell -> cell.g = g);
    }

    // return false when there is no waiting nodes
    public synchronized boolean setNextOwner() {
        int endIdx = members.indexOf(owner);
        int size = members.size();
        for (int i = (endIdx + 1) % size; i != endIdx; i = ++i % size) {
            Member member = members.get(i);
            if (member.r > member.g) {
                owner = member;
                return true;
            }
        }
        return false;
    }

    public synchronized void setOwner(String id) {
        for (Member member: members) {
            if (member.id.equals(id)) {
                owner = member;
                return;
            }
        }
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public boolean isInUse() {
        return inUse;
    }
}

class Member {
    String id;
    int r = 0;
    int g = 0;

    Member(String id) {
        this.id = id;
    }

    Member(String id, int r, int g) {
        this.id = id;
        this.r = r;
        this.g = g;
    }
}
