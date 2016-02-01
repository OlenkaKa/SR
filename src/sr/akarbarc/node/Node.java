package sr.akarbarc.node;

import sr.akarbarc.msgs.*;
import sr.akarbarc.ricartagrawala.*;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Created by ola on 05.01.16.
 */
public class Node implements Observer {
    private static final Logger logger = Logger.getLogger(Node.class.getName());
    private final String id = UUID.randomUUID().toString();
    private boolean joinNetwork = false;

    // ADDRESSES
    private String trackerHost;
    private int trackerPort;
    private int serverPort;

    // CONNECTIONS
    private Connection tracker;
    private List<Connection> nodes = new CopyOnWriteArrayList<>();

    // THREADS
    private Ping ping;
    private Server server;
    private Thread user;

    // RICHART-AGRAWALA
    private Token token = null;
    private Clock clock = new Clock();

    // SYNCHRONIZATION
    final Object tokenLock = new Object();

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
            tracker = new Connection("tracker", new Socket(trackerHost, trackerPort), true);
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
        synchronized (tokenLock) {
            if (token != null) {
                token.setInUse(true);
                token.addMember(id);
                token.setMemberR(id, clock.increaseTime());
                token.setOwner(id);
                return;
            }
        }
        sendAll(new TokenReqMessage(Type.TOKEN_REQ, id, clock.increaseTime()));
        if (user == null) {
            user = Thread.currentThread();
            synchronized (user) {
                user.wait();
            }
        }
    }

    public void releaseResource() {
        synchronized (tokenLock) {
            token.setMemberG(id, clock.increaseTime());
            token.setInUse(false);
            if (token.setNextOwner()) {
                TokenMessage msg = token.createMessage();
                token = null;
                sendAll(msg);
            }
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


        if (conn == tracker) {
            if (arg instanceof Boolean) {
                logger.info("Connection with tracker stopped.");
                ping.stop();
                handleTrackerDisconnected();
            } else if (arg instanceof String)
                handleTrackerMessage((String) arg);
            return;
        }

        if (nodes.contains(conn)) {
            if (arg instanceof Boolean) {
                logger.info("Connection with " + conn.getId() + " node stopped.");

                removeNode(conn);
                synchronized (tokenLock) {
                    if (token != null)
                        token.removeMember(conn.getId());
                }

                IdMessage msg = new IdMessage(Type.DISCONNECT, conn.getId());
                sendAll(msg);
                send(msg, tracker);
            } else if (arg instanceof String)
                handleNodeMessage((String) arg, conn);
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
            case DISCONNECT:
                handleDisconnect(data, tracker);
                return;
            case INVALID:
                logger.warning("Invalid message received.");
        }
    }

    void handleNewConnection(Socket socket) {
        Connection node = new Connection(socket, false);
        addNode(node);
        logger.info("Connection with new node started.");
    }

    void handleTrackerDisconnected() {
        while (true) {
            try {
                tracker = new Connection("tracker", new Socket(trackerHost, trackerPort), true);
                tracker.addObserver(this);
                logger.info("Connection with tracker started.");
                break;
            } catch (IOException e) {
                try {
                    logger.info("Trying to connect tracker...");
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    stop();
                }
            }
        }
        ConnectionsMessage msg = new ConnectionsMessage(Type.CONNECTIONS_INFO, id, serverPort);
        for(Connection node: nodes)
            msg.addConnection(node.getId(), node.getConnectionType(), node.getIp(), node.getPort());
        send(msg, tracker);
    }

    // MESSAGES HANDLERS

    private void handleJoinNetworkResp(String data) {
        AddressMessage msg = new AddressMessage(data);

        if (msg.getId().equals(id)) {
            // node is a root
            if (!joinNetwork) {
                synchronized (tokenLock) {
                    token = new Token();
                    token.setInUse(false);
                }
            }
        } else if (msg.getIp().equals("0.0.0.0")) {
            // election
            stop();
        } else {
            try {
                Connection node = new Connection(msg.getId(), new Socket(msg.getIp(), msg.getPort()), true);
                addNode(node);
                send(new IdMessage(Type.HELLO, id), node);
                logger.info("Connection with " + node.getId() + " node started.");
            } catch (IOException e) {
                logger.warning("Cannot connect " + msg.getId() + " node.");
                // TODO
            }
        }
        joinNetwork = true;
    }

    private void handleHello(String data, Connection sender) {
        IdMessage msg = new IdMessage(data);
        String newId = msg.getId();
        sender.setId(newId);
        logger.info("Set id for node: " + newId);
    }

    private void handleDisconnect(String data, Connection sender) {
        final IdMessage msg = new IdMessage(data);
        String disconnectedId = msg.getId();

        sendForward(msg, sender);
        logger.info("Disconnect message received - node " + disconnectedId);

        removeNode(getNode(disconnectedId));
        synchronized (tokenLock) {
            if (token != null)
                token.removeMember(disconnectedId);
        }
    }

    private void handleTokenReq(String data, Connection sender) {
        TokenReqMessage msg = new TokenReqMessage(data);

        synchronized (tokenLock) {
            if (token == null)
                sendForward(msg, sender);
            else {
                String reqId = msg.getId();
                token.addMember(reqId);
                token.setMemberR(reqId, msg.getR());
                send(new IdMessage(Type.TOKEN_REQ_RECEIVED, reqId), sender);
                logger.info("Send token \"request received\" to " + reqId + " node.");

                if (!token.isInUse() && token.setNextOwner()) {
                    TokenMessage tokenMsg = token.createMessage();
                    token = null;
                    send(tokenMsg, sender);
                }
            }
        }
    }

    private void handleTokenReqReceived(String data, Connection sender) {
        // TODO
    }

    private void handleToken(String data, Connection sender) {
        TokenMessage msg = new TokenMessage(data);
        if (msg.getId().equals(id)) {
            synchronized (tokenLock) {
                token = Token.createToken(msg);
            }
            synchronized (user) {
                if (user != null) {
                    user.notify();
                    user = null;
                }
            }
        } else {
            sendForward(msg, sender);
        }
    }


    // OTHERS

    private void addNode(Connection newNode) {
        String newId = newNode.getId();
        if (newId != null)
            for (Connection node: nodes)
                if (node.getId().equals(newId)) {
                    logger.warning("Node with " + newId + " already exists.");
                    return;
                }

        newNode.addObserver(this);
        nodes.add(newNode);
        logger.fine("Node " + newId + " added to nodes table");
    }

    private void removeNode(Connection node) {
        if (node != null) {
            nodes.remove(node);
            logger.fine("Node " + node.getId() + " removed from nodes table");
        }
    }

    private Connection getNode(String id) {
        for (Connection node: nodes)
            if (node.getId().equals(id))
                return node;
        return null;
    }

    private void send(Message msg, Connection receiver) {
        receiver.write(msg);
    }

    private void sendAll(Message msg) {
        for (Connection node: nodes)
            node.write(msg);
    }

    private void sendForward(Message msg, Connection sender) {
        nodes.stream().filter(node -> node != sender).forEach(node -> node.write(msg));
    }

    private void disconnect() {
        //IdMessage msg = new IdMessage(Type.DISCONNECT, id);
        //sendAll(msg);
        if (tracker != null) {
            //send(msg, tracker);
            tracker.close();
        }

        nodes.forEach(Connection::closeNoNotify);
    }
}
