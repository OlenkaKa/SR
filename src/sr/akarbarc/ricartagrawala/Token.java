package sr.akarbarc.ricartagrawala;

import sr.akarbarc.msgs.TokenMessage;
import sr.akarbarc.msgs.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by ola on 23.01.16.
 */
public class Token {
    private static final Logger logger = Logger.getLogger(Token.class.getName());
    private boolean inUse = true;
    private Member owner;
    private int ownerIdxBackup; // remember position when owner is removed
    private List<Member> members = new ArrayList<>();

    public static Token createToken(TokenMessage msg) {
        Token token = new Token();
        for (TokenMessage.TableElem elem: msg.getTable()) {
            Member member = new Member(elem.getId(), elem.getR(), elem.getG());
            token.members.add(member);
            if (member.id.equals(msg.getId()))
                token.owner = member;
        }
        return token;
    }

    public TokenMessage createMessage() {
        TokenMessage msg = new TokenMessage(Type.TOKEN);
        msg.setId(owner.id);
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
        logger.info("Node " + member.id + " added to token table.");
    }

    public synchronized void removeMember(String id) {
        for (Member member: members)
            if (member.id.equals(id)) {
                if (member == owner)
                    ownerIdxBackup = (members.indexOf(member) - 1) % members.size();
                members.remove(member);
                logger.info("Node " + member.id + " removed from token table.");
                return;
            }
    }

    public synchronized void setMemberR(String id, int r) {
        for (Member member: members)
            if (member.id.equals(id)) {
                member.r = r;
                return;
            }
    }

    public synchronized void setMemberG(String id, int g) {
        for (Member member: members)
            if (member.id.equals(id)) {
                member.g = g;
                return;
            }
    }

    // return false when there is no waiting nodes
    public synchronized boolean setNextOwner() {
        int size = members.size();
        int endIdx = (owner == null) ? ownerIdxBackup : members.indexOf(owner);

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

    public synchronized void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public synchronized boolean isInUse() {
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
