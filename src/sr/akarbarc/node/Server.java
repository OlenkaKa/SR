package sr.akarbarc.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Observable;

/**
 * Created by ola on 06.01.16.
 */
public class Server extends Observable {
    private int port;

    private ServerSocket serverSocket;
    private Thread listener;
    private boolean running = false;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        listener = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Socket socket = serverSocket.accept();
                        setChanged();
                        notifyObservers(socket);
                    }
                } catch (Exception e) {
                    Server.this.stop();
                }
            }
        };
        listener.start();
        setState(true);
    }

    public void stop() {
        try {
            setState(false);
            listener.interrupt();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setState(boolean newRunning) {
        if(running != newRunning) {
            running = newRunning;
            setChanged();
            notifyObservers(running);
        }
    }
}
