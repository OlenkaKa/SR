package sr.akarbarc.node;

import sr.akarbarc.msgs.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeoutException;

/**
 * Created by ola on 05.01.16.
 */
public class Node implements Observer {
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
            server.close();
        if(ping != null)
            ping.close();
        if(tracker != null)
            tracker.close();
        nodes.forEach(Connection::close);
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
            System.out.println("Ping stop.");
        } else if(o == server) {
            System.out.println("Server stop.");
        } else if(o == tracker) {
            System.out.println("Connection with tracker stop.");
        } else if(nodes.contains(o)) {
            System.out.println("Connection with node stop.");
        }
    }

    // CALLBACKS AND HANDLERS

    @SuppressWarnings("unused")
    void nodeCallback(Message msg) {
        System.out.println("Node callback: " + msg.toString());
    }

    @SuppressWarnings("unused")
    void trackerCallback(Message msg) {
        System.out.println("Tracker callback: " + msg.toString());
    }

    @SuppressWarnings("unused")
    void handleNewConnection(Socket socket) throws NoSuchMethodException {
        Class params[] = {Message.class};
        Connection node = new Connection(socket, Node.class.getDeclaredMethod("nodeCallback", params), this);
        node.addObserver(this);
        nodes.add(node);
        System.out.println("new node");
    }

    // OTHERS

    private void connectTracker() throws IOException, NoSuchMethodException {
        Class params[] = {Message.class};
        tracker = new Connection(new Socket(trackerHost, trackerPort),
                Node.class.getDeclaredMethod("trackerCallback", params), this);
        tracker.addObserver(this);
        ping = new Ping(tracker);
        ping.addObserver(this);
    }

    private void startServer() throws IOException, NoSuchMethodException {
        Class params[] = {Socket.class};
        server = new Server(serverPort, Node.class.getDeclaredMethod("handleNewConnection", params), this);
        server.addObserver(this);
    }
}
