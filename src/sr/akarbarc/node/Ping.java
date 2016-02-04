package sr.akarbarc.node;

import sr.akarbarc.msgs.Message;
import sr.akarbarc.msgs.Type;

import java.util.Observable;

/**
 * Created by ola on 06.01.16.
 */
public class Ping extends Observable {
    private int interval = 5000;
    private Connection tracker;
    private Thread sender;
    private boolean running = false;

    public Ping(Connection tracker, int interval) {
        this.tracker = tracker;
        this.interval = interval * 1000;
    }

    public void start() {
        sender = new Thread() {
            @Override
            public void run() {
                Message msg = new Message(Type.PING);
                try {
                    while(!isInterrupted()) {
                        tracker.write(msg);
                        Thread.sleep(interval);
                    }
                } catch (Exception e) {
                    Ping.this.stop();
                }
            }
        };
        sender.start();
        setState(true);
    }

    public void stop() {
        setState(false);
        sender.interrupt();
    }

    private void setState(boolean newRunning) {
        if(running != newRunning) {
            running = newRunning;
            setChanged();
            notifyObservers(running);
        }
    }
}
