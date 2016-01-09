package sr.akarbarc.node;

import sr.akarbarc.msgs.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Created by ola on 05.01.16.
 */
public class Node implements Observer {
    private final String id = UUID.randomUUID().toString();

    // ADDRESSES
    private String trackerHost;
    private int trackerPort;
    private int serverPort;
    private int clientPort;

    // CONNECTIONS
    private Connection tracker;
    private List<Connection> nodes;

    // THREADS
    private Ping ping;
    private Server server;

    // PUBLIC METHODS

    public Node(String trackerHost, int trackerPort, int serverPort, int clientPort) {
        this.trackerHost = trackerHost;
        this.trackerPort = trackerPort;
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        nodes = new ArrayList<>();
    }

    public boolean start() {
        try {
            connectTracker();
            startServer();
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
        } else {
            Connection conn = (Connection) o;
            if(conn == tracker) {
                System.out.println("Connection with tracker stopped.");
                ping.stop();
            } else if(nodes.contains(conn)) {
                System.out.println("Connection with " + conn.getId() + " node stopped.");
                nodes.remove(conn);
            }
        }
    }

    // MAIN HANDLERS

    @SuppressWarnings("unused")
    void handleNodeMessage(String data) {
        System.out.println("Node callback: " + data);
    }

    @SuppressWarnings("unused")
    void handleTrackerMessage(String data) {
        System.out.println("Tracker callback: " + data);
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
            Class params[] = {String.class};
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

    void handleJoinNetworkResp(AddressMessage msg) {
        try {
            Class params[] = {String.class};
            Connection node = new Connection(msg.getId(),
                    new Socket(msg.getIp(), msg.getPort()),
                    Node.class.getDeclaredMethod("handleNodeMessage", params), this);
            node.addObserver(this);
            nodes.add(node);
            System.out.println("Connection with " + node.getId() + " node started.");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Cannot connect " + msg.getId() + " node.");
            // TODO
        }
    }

    // SEND FUNCTIONS

    void sendJoinNetworkReq() {
        tracker.write(new IdMessage(Type.JOIN_NETWORK_REQ, id, clientPort));
    }


    // OTHERS

    private void connectTracker() throws IOException, NoSuchMethodException {
        Class params[] = {String.class};
        tracker = new Connection("tracker", new Socket(trackerHost, trackerPort),
                Node.class.getDeclaredMethod("handleTrackerMessage", params), this);
        tracker.addObserver(this);
        System.out.println("Connection with tracker started.");
        ping = new Ping(tracker);
        ping.addObserver(this);
        ping.start();
        sendJoinNetworkReq();
    }

    private void startServer() throws IOException, NoSuchMethodException {
        Class params[] = {Socket.class};
        server = new Server(serverPort,
                Node.class.getDeclaredMethod("handleNewConnection", params), this);
        server.addObserver(this);
        server.start();
    }
}
