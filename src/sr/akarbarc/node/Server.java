package sr.akarbarc.node;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Observable;

/**
 * Created by ola on 06.01.16.
 */
public class Server extends Observable {
    private Method handler;
    private Object handlerObj;
    private ServerSocket serverSocket;
    private Thread listener;

    public Server(int port, Method handler, Object obj) throws IOException {
        this.handler = handler;
        this.handlerObj = obj;
        serverSocket = new ServerSocket(port);
        listener = new Thread() {
            @Override
            public void run() {
                while(!isInterrupted()) {
                    try {
                        Socket socket = serverSocket.accept();
                        handler.invoke(handlerObj, socket);
                    } catch (Exception e) {
                        setChanged();
                        notifyObservers();
                    }
                }
            }
        };
        listener.start();
    }

    public void close() {
        try {
            listener.interrupt();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
