package pt.inesc.slave.clients;


/**
 * A circular array to keep track of the latest questions created
 * 
 * @author darionascimento
 */
public class ReadTracker {
    String[] list;
    int pos = 0;
    int it = 0;
    int pushed = 0;
    boolean empty = true;

    public ReadTracker(int nReadsIn100Requests) {
        list = new String[nReadsIn100Requests];
    }

    public void newQuestion(String questionTitle) {
        empty = false;
        list[pos] = questionTitle;
        pos = (pos + 1) % list.length;
    }

    public ReadTrackerIterator getInfinitIterator() {
        return new ReadTrackerIterator(this);
    }

    public boolean isEmpty() {
        return empty;
    }
}
