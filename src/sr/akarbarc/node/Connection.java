package sr.akarbarc.node;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * Created by ola on 06.01.16.
 */
public class Connection extends Thread {
    private Socket socket;
    private Method callback;

    public Connection(String host, int port, Method callback) throws IOException {
        this.socket = new Socket(host, port);
        this.callback = callback;
    }

    public Connection(Socket socket, Method callback) {
        this.socket = socket;
        this.callback = callback;
    }

    public boolean write() {
        // TODO
        return false;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        // TODO
        close();
    }
}
