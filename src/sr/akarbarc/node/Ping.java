package sr.akarbarc.node;

/**
 * Created by ola on 06.01.16.
 */
public class Ping extends Thread {
    private Connection tracker;

    public Ping(Connection tracker) {
        this.tracker = tracker;
    }

    @Override
    public void run() {
        // TODO
        super.run();
    }
}
