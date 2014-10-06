package inesc.slave.clients;

import java.util.LinkedList;

public class Statistics {
    /** Collect the execution round trip time, negative if failed or cancelled */
    public LinkedList<Integer> executionTimes;

    public long startExecution;

    public long dataReceived = 0;
    public int exceptionResponse = 0;

    public long endExecution;


    public Statistics() {
        startExecution = System.nanoTime();
        executionTimes = new LinkedList<Integer>();
    }

    public synchronized void requestCompleted(long duration) {
        executionTimes.add((int) duration);
    }

    public synchronized void requestCancelled(long duration) {
        executionTimes.add((int) (duration * -1));
    }

    public synchronized void requestFailed(long duration) {
        executionTimes.add((int) (duration * -1));
    }


    public synchronized void newResponse(int responseSize, boolean exception) {
        dataReceived += responseSize;
        exceptionResponse += (exception) ? 1 : 0;
    }

    public void over() {
        endExecution = System.nanoTime();
    }




}
