package sr.akarbarc.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Observable;

/**
 * Created by ola on 06.01.16.
 */
public class Connection extends Observable {
    private Socket socket;
    private Method callback;
    private Object callbackObj;
    private Thread receiver;

    public Connection(Socket socket, Method callback, Object obj) {
        this.socket = socket;
        this.callback = callback;
        this.callbackObj = obj;

        receiver = new Thread() {
            @Override
            public void run() {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String input;
                    while (!isInterrupted()) {
                        if ((input = in.readLine()) != null) {
                            callback.invoke(callbackObj);
                            System.out.println(input);
                        }
                    }
                } catch (Exception e) {
                    setChanged();
                    notifyObservers();
                }
            }
        };
        receiver.start();
    }

    public boolean write() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String outputLine = "Connection: write method called";
            out.println(outputLine);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void close() {
        try {
            receiver.interrupt();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
