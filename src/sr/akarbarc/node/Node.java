package sr.akarbarc.node;

import sr.akarbarc.msgs.*;
import sr.akarbarc.ricartagrawala.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by ola on 05.01.16.
 */
public class Node implements Observer {
    private static final Logger logger = Logger.getLogger(Node.class.getName());
    private final String id = UUID.randomUUID().toString();

    // ADDRESSES
    private String trackerHost;
    private int trackerPort;
    private int serverPort;

    // CONNECTIONS
    private Connection tracker;
    private List<Connection> nodes = new ArrayList<>();

    // THREADS
    private Ping ping;
    private Server server;
    private Thread user;

    // RICHART-AGRAWALA
    private Token token = null;
    private Clock clock = new Clock();

    // SYNCHRONIZATION
    final Object trackerLock = new Object();
    final Object nodesLock = new Object();

    // PUBLIC METHODS

    public Node(String trackerHost, int trackerPort, int serverPort) {
        this.trackerHost = trackerHost;
        this.trackerPort = trackerPort;
        this.serverPort = serverPort;
    }

    public String getId() {
        return id;
    }

    public boolean start() {
        try {
            tracker = new Connection("tracker", new Socket(trackerHost, trackerPort));
            tracker.addObserver(this);
            logger.info("Connection with tracker started.");

            ping = new Ping(tracker);
            ping.addObserver(this);
            ping.start();

            server = new Server(serverPort);
            server.addObserver(this);
            server.start();

            send(new ConnectInfoMessage(Type.JOIN_NETWORK_REQ, id, serverPort), tracker);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void stop() {
        if (server != null)
            server.stop();
        if (ping != null)
            ping.stop();
        disconnect();
    }

    public void getResource() throws InterruptedException {
        if (token != null)
            token.setInUse(true);
        else {
            sendAll(new TokenReqMessage(Type.TOKEN_REQ, id, clock.increaseTime()));
            user = Thread.currentThread();
            synchronized (user) {
                user.wait();
            }
        }
    }

    public void releaseResource() {
        token.setMemberG(id, clock.increaseTime());
        token.setInUse(false);
        if (token.setNextOwner()) {
            TokenMessage msg = token.createMessage();
            token = null;
            sendAll(msg);
        }
    }

    @Override
    public void update(Observable o, Object arg) {

        if (o == server) {
            if (arg instanceof Boolean)
                logger.info("Server " + ((boolean)arg ? "started." : "stopped."));
            else if (arg instanceof Socket)
                handleNewConnection((Socket) arg);
            return;
        }

        if (o == ping) {
            logger.info("Ping " + ((boolean)arg ? "started." : "stopped."));
            return;
        }

        Connection conn = (Connection) o;

        synchronized (trackerLock) {
            if (conn == tracker) {
                if (arg instanceof Boolean) {
                    logger.info("Connection with tracker stopped.");
                    ping.stop();
                } else if (arg instanceof String)
                    handleTrackerMessage((String) arg);
                return;
            }
        }

        synchronized (nodesLock) {
            if (nodes.contains(conn)) {
                if (arg instanceof Boolean) {
                    logger.info("Connection with " + conn.getId() + " node stopped.");
                    removeNode(conn);
                } else if (arg instanceof String)
                    handleNodeMessage((String) arg, conn);
            }
        }
    }

    // MAIN HANDLERS

    void handleNodeMessage(String data, Connection node) {
        logger.fine("Message from node: " + data);
        switch (Type.getType(data)) {
            case HELLO:
                handleHello(data, node);
                return;
            case DISCONNECT:
                handleDisconnect(data, node);
                return;
            case TOKEN_REQ:
                handleTokenReq(data, node);
                return;
            case TOKEN_REQ_RECEIVED:
                handleTokenReqReceived(data, node);
                return;
            case TOKEN:
                handleToken(data, node);
                return;
            case INVALID:
                logger.warning("Invalid message received.");
        }
    }

    void handleTrackerMessage(String data) {
        logger.fine("Message from tracker: " + data);
        switch (Type.getType(data)) {
            case JOIN_NETWORK_RESP:
                handleJoinNetworkResp(data);
                return;
            case INVALID:
                logger.warning("Invalid message received.");
        }
    }

    void handleNewConnection(Socket socket) {
        Connection node = new Connection(socket);
        addNode(node);
        logger.info("Connection with new node started.");
    }

    // MESSAGES HANDLERS

    private void handleJoinNetworkResp(String data) {
        AddressMessage msg = new AddressMessage(data);
        if (msg.getId().equals(id)) {
            // node is a root so it receives a token
            token = new Token();
            token.addMember(id);
            token.setOwner(id);
            token.setInUse(false);
            return;
        } else if (msg.getIp().equals("0.0.0.0")) {
            stop();
            return;
        }

        try {
            Connection node = new Connection(msg.getId(), new Socket(msg.getIp(), msg.getPort()));
            addNode(node);
            send(new IdMessage(Type.HELLO, id), node);
            logger.info("Connection with " + node.getId() + " node started.");
        } catch (IOException e) {
            logger.warning("Cannot connect " + msg.getId() + " node.");
            // TODO
        }
    }

    private void handleHello(String data, Connection sender) {
        IdMessage msg = new IdMessage(data);
        sender.setId(msg.getId());
        logger.info("Set id for node: " + sender.getId());

        if (token != null) {
            token.addMember(sender.getId());
            logger.info("Node " + sender.getId() + " added to token table.");
        }
    }

    private void handleDisconnect(String data, Connection sender) {
        // removing node from nodes in update method
        IdMessage msg = new IdMessage(data);
        sendForward(msg, sender);
        logger.info("Disconnect message received - node " + msg.getId());

        if (token != null) {
            token.removeMember(sender.getId());
            logger.info("Node " + sender.getId() + " removed from token table.");
        }
    }

    private void handleTokenReq(String data, Connection sender) {
        TokenReqMessage msg = new TokenReqMessage(data);
        if (token == null)
            sendForward(msg, sender);
        else {
            token.setMemberR(msg.getId(), msg.getR());
            send(new IdMessage(Type.TOKEN_REQ_RECEIVED, msg.getId()), sender);
            logger.info("Send token request receive to " + msg.getId() + " node.");

            if (!token.isInUse() &&token.setNextOwner()) {
                TokenMessage tokenMsg = token.createMessage();
                token = null;
                send(tokenMsg, sender);
            }
        }
    }

    private void handleTokenReqReceived(String data, Connection sender) {
        // TODO
    }

    private void handleToken(String data, Connection sender) {
        TokenMessage msg = new TokenMessage(data);
        if (msg.getDstId().equals(id)) {
            token = Token.createToken(msg);
            synchronized (user) {
                if (user != null)
                    user.notify();
            }
        } else {
            sendForward(msg, sender);
        }
    }


    // OTHERS

    private void addNode(Connection node) {
        node.addObserver(this);
        synchronized (nodesLock) {
            nodes.add(node);
        }
    }

    private void removeNode(Connection node) {
        boolean removed;
        synchronized (nodesLock) {
            removed = nodes.remove(node);
        }
        if (removed)
            sendAll(new IdMessage(Type.DISCONNECT, node.getId()));
    }

    private void send(Message msg, Connection receiver) {
        receiver.write(msg);
    }

    private void sendAll(Message msg) {
        synchronized (nodesLock) {
            for (Connection node: nodes)
                node.write(msg);
        }
    }

    private void sendForward(Message msg, Connection sender) {
        synchronized (nodesLock) {
            nodes.stream().filter(node -> node != sender).forEach(node -> node.write(msg));
        }
    }

    private void disconnect() {
        sendAll(new IdMessage(Type.DISCONNECT, id));
        if (tracker != null)
            tracker.close();
        synchronized (nodesLock) {
            nodes.forEach(Connection::closeNoNotify);
        }
    }
}
