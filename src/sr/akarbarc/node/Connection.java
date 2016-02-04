package sr.akarbarc.node;

import sr.akarbarc.msgs.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Observable;

/**
 * Created by ola on 06.01.16.
 */
public class Connection extends Observable {
    private String id;
    private Socket socket;
    private Thread receiver;
    private boolean isIncomming;
    private boolean running = true;

    public Connection(Socket socket, boolean isIncomming) {
        this.socket = socket;
        this.isIncomming = isIncomming;
        receiver = new Thread() {
            @Override
            public void run() {
                try {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    byte input[] = new byte[1024];
                    int size;
                    while (!isInterrupted()) {
                        size = in.readInt();
                        in.read(input, 0, size);
                        setChanged();
                        notifyObservers(new String(input));
                    }
                    setState(false);
                } catch (IOException e) {
                    setState(false);
                }
            }
        };
        receiver.start();
    }

    public Connection(String id, Socket socket, boolean isIncomming) {
        this(socket, isIncomming);
        this.id = id;
    }

    public synchronized void write(Message msg) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            byte data[] = msg.toString().getBytes();
            out.writeInt(data.length);
            out.write(data);
        } catch (IOException e) {
            setState(false);
        }
    }

    public synchronized void closeNoNotify() {
        try {
            receiver.interrupt();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void close() {
        setState(false);
        closeNoNotify();
    }

    public synchronized String getId() {
        return id;
    }

    public synchronized String getIp() {
        return socket.getInetAddress().getHostAddress();
    }

    public synchronized int getPort() {
        return isIncomming ? socket.getLocalPort() : socket.getPort();
    }

    public synchronized boolean isIncomming() {
        return isIncomming;
    }

    public synchronized void setId(String id) {
        this.id = id;
    }

    private synchronized void setState(boolean newRunning) {
        if(running != newRunning) {
            running = newRunning;
            setChanged();
            notifyObservers(running);
        }
    }
}
