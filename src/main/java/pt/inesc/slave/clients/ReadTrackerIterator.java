package pt.inesc.slave.clients;

public class ReadTrackerIterator {

    private final ReadTracker d;
    int i;

    public ReadTrackerIterator(ReadTracker readTracker) {
        this.d = readTracker;
        this.i = readTracker.pos;
    }

    public String next() {
        i = (i - 1 + d.list.length) % d.list.length;
        if (d.list[i] == null) {
            i = (d.pos - 1 + d.list.length) % d.list.length;
        }
        return d.list[i];
    }
}
