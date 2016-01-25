package sr.akarbarc.node;

import sr.akarbarc.msgs.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeoutException;
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
    private List<Connection> nodes;

    // THREADS
    private Ping ping;
    private Server server;

    // SYNCHRONIZATION
    final Object trackerLock = new Object();
    final Object nodesLock = new Object();

    // PUBLIC METHODS

    public Node(String trackerHost, int trackerPort, int serverPort) {
        this.trackerHost = trackerHost;
        this.trackerPort = trackerPort;
        this.serverPort = serverPort;
        nodes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public boolean start() {
        try {
            initialize();
            sendJoinNetworkReq();
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

    public void getResource() throws TimeoutException {
        // TODO
    }

    public void releaseResource() {
        // TODO
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
        Message msg = new Message(data);
        switch (msg.getType()) {
            case HELLO:
                handleHello(new IdMessage(data), node);
                return;
            case DISCONNECT:
                handleDisconnect(new IdMessage(data), node);
                return;
            case INVALID:
                logger.warning("Invalid message received.");
        }
    }

    void handleTrackerMessage(String data) {
        logger.fine("Message from tracker: " + data);
        Message msg = new Message(data);
        switch (msg.getType()) {
            case JOIN_NETWORK_RESP:
                handleJoinNetworkResp(new AddressMessage(data));
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

    private void handleJoinNetworkResp(AddressMessage msg) {
        if (msg.getId().equals(id))
            return;
        else if (msg.getIp().equals("0.0.0.0")) {
            stop();
            return;
        }

        try {
            Connection node = new Connection(msg.getId(), new Socket(msg.getIp(), msg.getPort()));
            addNode(node);
            logger.info("Connection with " + node.getId() + " node started.");
            sendHello();
        } catch (IOException e) {
            logger.warning("Cannot connect " + msg.getId() + " node.");
            // TODO
        }
    }

    private void handleHello(IdMessage msg, Connection sender) {
        // TODO: node has the token
        sender.setId(msg.getId());
        logger.info("Set id for node: " + sender.getId());
    }

    private void handleDisconnect(IdMessage msg, Connection sender) {
        // TODO: node has the token, synchronization
        sendDisconnect(msg.getId(), sender);
        logger.info("Disconnect message received - node " + msg.getId());
    }

    // SEND FUNCTIONS

    private void sendJoinNetworkReq() {
        synchronized (trackerLock) {
            tracker.write(new JoinMessage(Type.JOIN_NETWORK_REQ, id, serverPort));
        }
    }

    private void sendHello() {
        Message msg = new IdMessage(Type.HELLO, id);
        synchronized (nodesLock) {
            for (Connection node : nodes)
                node.write(msg);
        }
    }

    private void sendDisconnect(String id) {
        sendDisconnect(id, null);
    }

    private void sendDisconnect(String id, Connection sender) {
        Message msg = new IdMessage(Type.DISCONNECT, id);
        synchronized (nodesLock) {
            nodes.stream().filter(node -> node != sender).forEach(node -> node.write(msg));
        }
    }


    // OTHERS

    private void initialize() throws IOException {
        tracker = new Connection("tracker", new Socket(trackerHost, trackerPort));
        tracker.addObserver(this);
        logger.info("Connection with tracker started.");

        ping = new Ping(tracker);
        ping.addObserver(this);
        ping.start();

        server = new Server(serverPort);
        server.addObserver(this);
        server.start();
    }

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
            sendDisconnect(node.getId());
    }

    private void disconnect() {
        sendDisconnect(id);
        if (tracker != null)
            tracker.close();
        synchronized (nodesLock) {
            nodes.forEach(Connection::closeNoNotify);
        }
    }
}
