package sr.akarbarc.ricartagrawala;

/**
 * Created by ola on 25.01.16.
 */
public class Clock {
    private int time = 0;

    public synchronized int increaseTime() {
        return ++time;
    }
}
