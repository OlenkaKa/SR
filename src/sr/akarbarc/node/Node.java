package sr.akarbarc.node;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by ola on 05.01.16.
 */
public class Node {
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
            System.out.println("Failed to start node.");
            return false;
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        // TODO
    }

    public void getResource() throws TimeoutException {
        // TODO
    }

    public void releaseResource() {
        // TODO
    }


    // CALLBACKS AND HANDLERS

    private void nodeCallback() {
        // TODO
    }

    private void trackerCallback() {
        // TODO
    }

    private void handleNewConnection(Socket socket) throws NoSuchMethodException {
        Class params[] = {};
        Connection node = new Connection(socket, Node.class.getDeclaredMethod("nodeCallback", params));
        nodes.add(node);
    }

    // OTHERS

    private void connectTracker() throws IOException, NoSuchMethodException {
        Class params[] = {};
        tracker = new Connection(trackerHost, trackerPort, Node.class.getDeclaredMethod("trackerCallback", params));
        ping = new Ping(tracker);
        ping.start();
    }

    private void startServer() throws IOException, NoSuchMethodException {
        Class params[] = {Socket.class};
        server = new Server(serverPort, Node.class.getDeclaredMethod("handleNewConnection", params));
        server.start();
    }
}
