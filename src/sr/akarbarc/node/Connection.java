package sr.akarbarc.node;

import sr.akarbarc.msgs.Message;

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
    private String id = "unknown";
    private Socket socket;
    private Thread receiver;
    private boolean running = true;

    public Connection(Socket socket, Method callback, Object callbackObj) {
        this.socket = socket;
        receiver = new Thread() {
            @Override
            public void run() {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String input;
                    while (!isInterrupted() && (input = in.readLine()) != null) {
                        callback.invoke(callbackObj, input);
                    }
                    setState(false);
                } catch (Exception e) {
                    setState(false);
                }
            }
        };
        receiver.start();
    }

    public Connection(String id, Socket socket, Method callback, Object callbackObj) {
        this(socket, callback, callbackObj);
        this.id = id;
    }

    public void write(Message msg) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String outputLine = msg.toString();
            out.println(outputLine);
        } catch (IOException e) {
            setState(false);
        }
    }

    public void closeNoNotify() {
        try {
            receiver.interrupt();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        setState(false);
        closeNoNotify();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private void setState(boolean newRunning) {
        if(running != newRunning) {
            running = newRunning;
            setChanged();
            notifyObservers(running);
        }
    }
}
