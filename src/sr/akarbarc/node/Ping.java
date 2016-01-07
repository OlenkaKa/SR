package sr.akarbarc.node;

import java.util.Observable;

/**
 * Created by ola on 06.01.16.
 */
public class Ping extends Observable {
    private final int INTERVAL = 5000;
    private Connection tracker;
    private Thread sender;

    public Ping(Connection tracker) {
        this.tracker = tracker;
        sender = new Thread() {
            @Override
            public void run() {
                while(!isInterrupted()) {
                    try {
                        tracker.write();
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        interrupt();
                        return;
                    } catch (Exception e) {
                        setChanged();
                        notifyObservers();
                    }
                }
            }
        };
        sender.start();
    }

    public void close() {
        sender.interrupt();
    }
}
