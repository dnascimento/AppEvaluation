package pt.inesc.slave.clients;

public class CountDown {

    private int running = 0;
    private boolean waiting = false;

    public synchronized void await() throws InterruptedException {
        if (running > 0) {
            waiting = true;
            this.wait();
        }
    }

    public synchronized void increment(int length) {
        running += length;
    }

    public synchronized void countDown() {
        running--;
        if (waiting && running == 0) {
            this.notifyAll();
        }
    }

    public synchronized void increment() {
        running++;
    }
}
