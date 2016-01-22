package sr.akarbarc.node;

import sr.akarbarc.msgs.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Created by ola on 05.01.16.
 */
public class Node implements Observer {
    private final String ID = UUID.randomUUID().toString();

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

    // PUBLIC METHODS

    public Node(String trackerHost, int trackerPort, int serverPort) {
        this.trackerHost = trackerHost;
        this.trackerPort = trackerPort;
        this.serverPort = serverPort;
        nodes = new ArrayList<>();
    }

    public String getId() {
        return ID;
    }

    public boolean start() {
        try {
            connectTracker();
            startServer();
            sendJoinNetworkReq();
        } catch (IOException e) {
            return false;
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if(server != null)
            server.stop();
        if(ping != null)
            ping.stop();
        if(tracker != null)
            tracker.close();
        nodes.forEach(Connection::closeNoNotify);
    }

    public void getResource() throws TimeoutException {
        // TODO
    }

    public void releaseResource() {
        // TODO
    }

    @Override
    public void update(Observable o, Object arg) {
        if(o == ping) {
            System.out.println("Ping "
                    + ((boolean)arg ? "started." : "stopped."));
        } else if(o == server) {
            System.out.println("Server "
                    + ((boolean)arg ? "started." : "stopped."));
        } else if(o == tracker) {
            System.out.println("Connection with tracker stopped.");
            ping.stop();
        } else {
            Connection conn = (Connection) o;
            if(nodes.contains(conn)) {
                System.out.println("Connection with " + conn.getId() + " node stopped.");
                nodes.remove(conn);
                sendDisconnect(conn.getId());
            }
        }
    }

    // MAIN HANDLERS

    @SuppressWarnings("unused")
    void handleNodeMessage(String data, Connection node) {
        //System.out.println("Message from node: " + data);
        Message msg = new Message(data);
        switch(msg.getType()) {
            case HELLO:
                handleHello(new IdMessage(data), node);
                return;
            case DISCONNECT:
                handleDisconnect(new IdMessage(data), node);
                break;
            case INVALID:
                System.out.println("Invalid message received.");
        }
    }

    @SuppressWarnings("unused")
    void handleTrackerMessage(String data, Connection tracker) {
        //System.out.println("Message from tracker: " + data);
        Message msg = new Message(data);
        switch(msg.getType()) {
            case JOIN_NETWORK_RESP:
                handleJoinNetworkResp(new AddressMessage(data));
                return;
            case INVALID:
                System.out.println("Invalid message received.");
        }
    }

    @SuppressWarnings("unused")
    void handleNewConnection(Socket socket) {
        try {
            Class params[] = {String.class, Connection.class};
            Connection node = new Connection(socket,
                    Node.class.getDeclaredMethod("handleNodeMessage", params), this);
            node.addObserver(this);
            nodes.add(node);
            System.out.println("Connection with new node started.");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    // MESSAGES HANDLERS

    private void handleJoinNetworkResp(AddressMessage msg) {
        if(msg.getType() == Type.INVALID) {
            System.out.println("Invalid message received.");
            return;
        } else if(msg.getId().equals(ID)) {
            return;
        } else if(msg.getIp().equals("0.0.0.0")) {
            stop();
            return;
        }

        try {
            Class params[] = {String.class, Connection.class};
            Connection node = new Connection(msg.getId(),
                    new Socket(msg.getIp(), msg.getPort()),
                    Node.class.getDeclaredMethod("handleNodeMessage", params), this);
            node.addObserver(this);
            nodes.add(node);
            System.out.println("Connection with " + node.getId() + " node started.");
            sendHello();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Cannot connect " + msg.getId() + " node.");
            // TODO
        }
    }

    private void handleHello(IdMessage msg, Connection sender) {
        // TODO: node has the token
        sender.setId(msg.getId());
        System.out.println("Set id for node: " + sender.getId());
    }

    private void handleDisconnect(IdMessage msg, Connection sender) {
        // TODO: node has the token
        nodes.stream().filter(node -> node != sender).forEach(node -> node.write(msg));
        System.out.println("Disconnect message received - node " + msg.getId());
    }

    // SEND FUNCTIONS

    private void sendJoinNetworkReq() {
        tracker.write(new JoinMessage(Type.JOIN_NETWORK_REQ, ID, serverPort));
    }

    private void sendHello() {
        Message msg = new IdMessage(Type.HELLO, ID);
        for (Connection node: nodes) {
            node.write(msg);
        }
    }

    private void sendDisconnect(String id) {
        Message msg = new IdMessage(Type.DISCONNECT, id);
        for (Connection node: nodes) {
            node.write(msg);
        }
    }


    // OTHERS

    private void connectTracker() throws IOException, NoSuchMethodException {
        Class params[] = {String.class, Connection.class};
        tracker = new Connection("tracker", new Socket(trackerHost, trackerPort),
                Node.class.getDeclaredMethod("handleTrackerMessage", params), this);
        tracker.addObserver(this);
        System.out.println("Connection with tracker started.");
        ping = new Ping(tracker);
        ping.addObserver(this);
        ping.start();
    }

    private void startServer() throws IOException, NoSuchMethodException {
        Class params[] = {Socket.class};
        server = new Server(serverPort,
                Node.class.getDeclaredMethod("handleNewConnection", params), this);
        server.addObserver(this);
        server.start();
    }
}
