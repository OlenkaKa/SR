package sr.akarbarc.node;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;

/**
 * Created by ola on 06.01.16.
 */
public class Server extends Thread {
    private int port;
    private Method connectionHandler;
    private ServerSocket serverSocket;

    public Server(int port, Method handler) throws IOException {
        this.port = port;
        this.connectionHandler = handler;
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        // TODO
        super.run();
    }
}
