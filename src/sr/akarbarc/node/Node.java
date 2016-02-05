package sr.akarbarc.node;

import sr.akarbarc.msgs.*;
import sr.akarbarc.ricartagrawala.*;

import java.io.IOException;
import java.net.Socket;
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
    private boolean reqReceived = false;
    private boolean isRunning;

    // ADDRESSES
    private String trackerHost;
    private int trackerPort;
    private int serverPort;

    // CONNECTIONS
    private Connection tracker;
    private List<Connection> nodes;

    // THREADS
    private Ping ping;
    private Server server;
    private Thread user;

    // RICHART-AGRAWALA
    private Token token = null;
    private Clock tokenClock;
    private Timer tokenReqTimeout;

    // TIMEOUTS AND TIME INTERVALS IN SECONDS
    private final int TOKEN_REQ_RECEIVED_TIME = 10;
    private final int PING_TIME = 5;

    // SYNCHRONIZATION
    private final Object tokenLock = new Object();

    // PUBLIC METHODS

    public Node(String trackerHost, int trackerPort, int serverPort) {
        this.trackerHost = trackerHost;
        this.trackerPort = trackerPort;
        this.serverPort = serverPort;
        nodes = new CopyOnWriteArrayList<>();
        tokenClock = new Clock();
        token = null;
    }

    public String getId() {
        return id;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean start() {
        try {
            isRunning = true;

            tracker = new Connection("tracker", new Socket(trackerHost, trackerPort), false);
            tracker.addObserver(this);
            logger.info("Connection with tracker started.");

            ping = new Ping(tracker, PING_TIME);
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
        isRunning = false;
        if (server != null)
            server.stop();
        if (ping != null)
            ping.stop();
        disconnect();
    }

    public void getToken() throws InterruptedException {
        // current node has a token
        synchronized (tokenLock) {
            if (token != null) {
                token.setInUse(true);
                token.addMember(id);
                token.setMemberR(id, tokenClock.increaseTime());
                token.setOwner(id);
                return;
            }
        }

        // send token request
        sendAll(new TokenReqMessage(Type.TOKEN_REQ, id, tokenClock.increaseTime()));
        waitForTokenResp();
        if (user == null) {
            user = Thread.currentThread();
            synchronized (user) {
                user.wait();
            }
        }
    }

    public void releaseToken() {
        synchronized (tokenLock) {
            token.setMemberG(id, tokenClock.increaseTime());
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

                //IdMessage msg = new IdMessage(Type.DISCONNECT, conn.getId());
                //sendAll(msg);
                //send(msg, tracker);
                if (!conn.isIncomming()) {
                    ConnectInfoMessage req = new ConnectInfoMessage(Type.JOIN_NETWORK_REQ, id, serverPort);
                    for (Connection node : nodes)
                        req.addConnection(node.getId(), node.isIncomming(), node.getIp(), node.getPort());
                    send(req, tracker);
                }
            } else if (arg instanceof String)
                handleNodeMessage((String) arg, conn);
        }
    }

    // MAIN HANDLERS

    void handleNodeMessage(String data, Connection node) {
        logger.info("Message from node: " + data);
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
            case TOKEN_BUSY:
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
        logger.info("Message from tracker: " + data);
        switch (Type.getType(data)) {
            case JOIN_NETWORK_RESP:
                handleJoinNetworkResp(data);
                return;
            case DISCONNECT:
                handleDisconnect(data, tracker);
                return;
            case ELECTION_FINISHED:
                handleElectionFinished(data);
                return;
            case INVALID:
                logger.warning("Invalid message received.");
        }
    }

    void handleNewConnection(Socket socket) {
        Connection node = new Connection(socket, true);
        addNode(node);
        logger.info("Connection with new node started.");
    }

    void handleTrackerDisconnected() {
        logger.info("Tracker disconnected, node stopped.");
        stop();
        isRunning = false;
        /*
        while (true) {
            try {
                tracker = new Connection("tracker", new Socket(trackerHost, trackerPort), false);
                tracker.addObserver(this);
                logger.info("Connection with tracker started.");
                break;
            } catch (IOException e) {
                try {
                    logger.info("Trying to connect tracker...");
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    stop();
                    return;
                }
            }
        }
        ConnectInfoMessage msg = new ConnectInfoMessage(Type.CONNECTIONS_INFO, id, serverPort);
        for(Connection node: nodes)
            msg.addConnection(node.getId(), node.isIncomming(), node.getIp(), node.getPort());
        send(msg, tracker);

        ping = new Ping(tracker, PING_TIME);
        ping.addObserver(this);
        ping.start();
        */
    }

    // MESSAGES HANDLERS

    private void handleJoinNetworkResp(String data) {
        AddressMessage msg = new AddressMessage(data);
        String respId = msg.getId();

        if (respId == null || respId.equals(id)) {
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
                if (getNode(respId) != null)
                    send(new ConnectInfoMessage(Type.JOIN_NETWORK_REQ, id, serverPort), tracker);
                else {
                    Connection node = new Connection(respId, new Socket(msg.getIp(), msg.getPort()), false);
                    addNode(node);
                    send(new IdMessage(Type.HELLO, id), node);
                    logger.info("Connection with " + node.getId() + " node started.");
                }
            } catch (IOException e) {
                logger.warning("Cannot connect " + msg.getId() + " node.");
                //send(new IdMessage(Type.DISCONNECT, msg.getId()), tracker);
                send(new ConnectInfoMessage(Type.JOIN_NETWORK_REQ, id, serverPort), tracker);
            }
        }
        joinNetwork = true;
    }

    private void handleElectionFinished(String data) {
        synchronized (tokenLock) {
            token = new Token();
            token.setInUse(false);
        }
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

        //sendForward(msg, sender);
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

                if (!token.isInUse() && token.setNextOwner()) {
                    TokenMessage tokenMsg = token.createMessage();
                    token = null;
                    send(tokenMsg, sender);
                    logger.info("Send token to " + tokenMsg.getId() + " node.");
                } else {
                    send(new IdMessage(Type.TOKEN_BUSY, reqId), sender);
                    logger.info("Send \"token request received\" to " + reqId + " node.");
                }
            }
        }
    }

    private void handleTokenReqReceived(String data, Connection sender) {
        // TODO!!!
        IdMessage msg = new IdMessage(data);
        if (!msg.getId().equals(id))
            sendForward(msg, sender);
        else {
            reqReceived = true;
            /*
            try {
                stopWaitingForTokenResp();
                Thread.sleep(TOKEN_REQ_RECEIVED_TIME * 1000);
                sendAll(new TokenReqMessage(Type.TOKEN_REQ, id, tokenClock.getTime()));
                waitForTokenResp();
            } catch (InterruptedException e) {
                //e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            */
        }
    }

    private void handleToken(String data, Connection sender) {
        TokenMessage msg = new TokenMessage(data);
        if (msg.getId().equals(id)) {
            logger.info("Token received!");
            stopWaitingForTokenResp();
            reqReceived = false;

            synchronized (tokenLock) {
                token = Token.createToken(msg);
            }
            if (user != null) {
                synchronized (user) {
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
                if (newId.equals(node.getId())) {
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
        if (id == null)
            return null;

        for (Connection node: nodes)
            if (id.equals(node.getId()))
                return node;
        return null;
    }

    private void send(Message msg, Connection receiver) {
        logger.info("Send to " + receiver.getId() + ": " + msg);
        receiver.write(msg);
    }

    private void sendAll(Message msg) {
        logger.info("Send to all nodes: " + msg);
        for (Connection node: nodes)
            node.write(msg);
    }

    private void sendForward(Message msg, Connection sender) {
        logger.info("Send forward beside node " + sender.getId() + ": " + msg);
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

    private void waitForTokenResp() {
        tokenReqTimeout = new Timer();
        tokenReqTimeout.schedule(new TimerTask() {
            @Override
            public void run() {
                if (reqReceived) {
                    reqReceived = false;
                    sendAll(new TokenReqMessage(Type.TOKEN_REQ, id, tokenClock.getTime()));
                    logger.info("Token request send again.");
                } else {
                    reqReceived = false;
                    logger.info("Token request received timeout!");
                    send(new Message(Type.ELECTION_REQ), tracker);
                    try {
                        Thread.sleep(5000);
                        if (token != null) {
                            token.setInUse(true);
                            token.addMember(id);
                            token.setMemberR(id, tokenClock.getTime());
                            token.setOwner(id);
                            logger.info("Token received!");
                            if (user != null) {
                                synchronized (user) {
                                    user.notify();
                                    user = null;
                                }
                            }
                            // TODO: is it allowed?
                            this.cancel();
                        }
                        else {
                            sendAll(new TokenReqMessage(Type.TOKEN_REQ, id, tokenClock.getTime()));
                            logger.info("Token request send again.");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, TOKEN_REQ_RECEIVED_TIME * 1000, TOKEN_REQ_RECEIVED_TIME * 1000);
    }

    private void stopWaitingForTokenResp() {
        if (tokenReqTimeout != null) {
            tokenReqTimeout.cancel();
            tokenReqTimeout = null;
        }
    }
}
