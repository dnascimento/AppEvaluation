/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package pt.inesc.slave.clients;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.log4j.Logger;

/**
 * Individual whose perform resquests on server
 */
public abstract class ClientThread extends
        Thread {


    static Logger log = Logger.getLogger(ClientThread.class);

    /** Measurements of the execution */
    public Statistics stats;

    /** Controller */
    protected ClientManager clientManager;

    private final CloseableHttpAsyncClient httpclient;
    private final CountDown countDown;

    /** number of requests per second */
    private final int requestRate;
    private int requestRateSent = 0;
    private long currentSecound = 0;
    private long delay;
    private final double THROUGHPUT_MARGIN = 0.1;
    private static final int TIMEOUT = 2000;
    private static final String USER_AGENT = "Shuttle";

    /**
     * Show the current request rate every x seconds
     */
    private static final int SHOW_RATE_PERIOD = 10;

    private static final String OUTPUT_FILE = "requests.txt";

    private final boolean asynchronous;

    final int clientId;

    private final ClientConfiguration config;

    private int showRate = SHOW_RATE_PERIOD;

    private FileOutputStream fileChannel;

    private static AtomicInteger countRequests = new AtomicInteger(0);

    /**
     * @param clientId
     * @param targetHost
     * @param clientManager
     * @param throughput if 0 or minor, then maximum throughput
     * @param clientManager
     * @param readPercentage
     */
    ClientThread(int clientId, ClientManager clientManager, ClientConfiguration config) {
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(TIMEOUT).setConnectTimeout(TIMEOUT).build();
        httpclient = HttpAsyncClients.custom()
                                     .setDefaultRequestConfig(requestConfig)
                                     .setUserAgent(USER_AGENT)
                                     .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                                     .build();
        countDown = new CountDown();
        this.asynchronous = config.asynchronous;

        if (config.throughput <= 0) {
            requestRate = Integer.MAX_VALUE;
            delay = -1;
        } else {
            requestRate = config.throughput;
            delay = (long) (((double) 1 / config.throughput) * 1000);
        }

        if (config.logToDisk) {
            try {
                fileChannel = new FileOutputStream(new File(OUTPUT_FILE));
            } catch (FileNotFoundException e) {
                log.error("Creating file to log to disk", e);
                fileChannel = null;
            }
        }

        this.clientId = clientId;
        this.clientManager = clientManager;
        httpclient.start();
        this.config = config;
    }


    public void execRequest(HttpRequestBase request) {
        if (request == null) {
            return;
        }
        if (stats == null) {
            // init statistics on first request
            stats = new Statistics();
        }

        throughputControl();
        countDown.increment();
        long start = System.nanoTime();
        Future<HttpResponse> callback;
        if (fileChannel != null) {
            callback = httpclient.execute(HttpAsyncMethods.create(config.target, request),
                                          new ResponseHandlerFlush(stats, fileChannel),
                                          new FutureEvalCallback(start, countDown, request, stats));

        } else {
            callback = httpclient.execute(HttpAsyncMethods.create(config.target, request),
                                          new ResponseHandler(stats),
                                          new FutureEvalCallback(start, countDown, request, stats));
        }

        if (!asynchronous) {
            try {
                callback.get();
            } catch (Exception e) {

            }
        }
    }

    /**
     * Terminate the thread waiting for the latest requests and generating the report
     * 
     * @throws Exception
     */
    public void end() throws Exception {
        // wait to finish the requests
        countDown.await();
        stats.over();
        // Store the report in controller to send later to master
        clientManager.addStats(clientId, stats);
        httpclient.close();
    }

    /**
     * The delay controls the throughput, at end of each second, the throughput is
     * compared and the delay is adapted.
     */
    private void throughputControl() {
        countRequests.incrementAndGet();

        // delay the request to control the throughput
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long now = System.currentTimeMillis() / 1000;
        if (currentSecound == 0) {
            currentSecound = now;
        }

        if (now != currentSecound) {
            currentSecound = now;
            if (showRate-- == 0) {
                System.out.println("Rate: " + requestRateSent + " total: " + countRequests.get());
                showRate = SHOW_RATE_PERIOD;
            }
            if (delay >= 0) {
                if (Math.abs(requestRateSent - requestRate) > (requestRate * THROUGHPUT_MARGIN)) {
                    delay = (delay == 0) ? 1000 : delay;
                    delay = (long) (delay * ((double) requestRateSent / requestRate));
                }
            }
            requestRateSent = 0;
        }
        requestRateSent++;
    }

    /**
     * Force to terminate the thread
     */
    public abstract void over();
}
