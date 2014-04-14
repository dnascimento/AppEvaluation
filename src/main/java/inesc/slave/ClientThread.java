/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.slave;

import inesc.shared.AppEvaluationProtos.AppStartMsg.StartOpt;
import inesc.slave.reports.ThreadReport;
import inesc.slave.server.SlaveMain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * Individual whose perform resquests on server
 */
class ClientThread extends
        Thread {
    private static Logger log = Logger.getLogger(ClientThread.class);
    private final CloseableHttpClient httpClient;
    private final HttpContext context;

    /** Client unique ID */
    private final int clientID;

    /** Set of requests to perform **/
    private HttpRequestBase[] history;

    /** How many times each request is performed */
    private short[] historyCounter;

    /** Collect the execution round trip time */
    private final short[] executionTimes;

    /** Collect the responses */
    private final ByteBuffer[] responseData;

    private int flushedRequests = 0;

    private long flushedFilePosition;


    /** Summary of execution */
    public ThreadReport report;

    /** Controller */
    private final ClientManager clientManager;

    /** Bytes received counter */
    private long dataReceived = 0;

    /** Async log file for flush the responses */
    private AsynchronousFileChannel fileChannel = null;

    /** Flag to flush or not the responses */
    private boolean diskLog = false;


    public ClientThread(CloseableHttpClient httpClient,
            HttpRequestBase[] history,
            short[] historyCounter,
            int clientID,
            ClientManager clientManager) {
        this.httpClient = httpClient;
        this.clientManager = clientManager;
        this.history = history;
        this.historyCounter = historyCounter;
        this.clientID = clientID;

        context = new BasicHttpContext();
        report = new ThreadReport(historyCounter, clientID);
        int totalRequests = report.nTransactions;
        executionTimes = new short[totalRequests];
        responseData = new ByteBuffer[totalRequests];
        System.out.println("TOTAL:" + totalRequests);

    }

    /**
     * Executes the GetMethod and prints status information.
     */
    @Override
    public void run() {
        log.info("Client" + clientID + "starting...");
        long startExecution = System.currentTimeMillis();
        int requestID = 0;
        long start;
        long duration;

        for (int i = 0; i < history.length; i++) {
            HttpRequestBase req = history[i];

            while (historyCounter[i]-- > 0) {
                CloseableHttpResponse response;
                try {
                    start = System.currentTimeMillis();
                    response = httpClient.execute(req, context);
                    duration = (System.currentTimeMillis() - start);
                    executionTimes[requestID] = (short) duration;
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        responseData[requestID] = ByteBuffer.wrap(EntityUtils.toByteArray(entity));
                        dataReceived += responseData[requestID].capacity();
                        if (diskLog && dataReceived > 10) {
                            // To save memory and avoid head problems, flush async
                            flushData();
                        }
                    }
                } catch (NoHttpResponseException e) {
                    // Server received the request but due to overload do not reply
                    log.warn("No HTTP Response");
                    executionTimes[requestID] = -1;

                } catch (ConnectionPoolTimeoutException e) {
                    // multithreaded connection manager fails to obtain a free connection
                    log.warn("Connection Pool Timeout Exception");
                    executionTimes[requestID] = -2;

                } catch (ConnectTimeoutException e) {
                    // unable to establish a connection
                    log.warn("Connect Timeout Exception");
                    executionTimes[requestID] = -3;

                } catch (Exception e) {
                    log.warn(e);
                    e.printStackTrace();
                    executionTimes[requestID] = -4;
                }

                // Delay the next request
                try {
                    sleep(ClientManager.DELAY_BETWEEN_REQUESTS);
                } catch (InterruptedException e) {
                    log.error(e);
                }
                requestID++;
            }
        }
        long totalExecutionTime = System.currentTimeMillis() - startExecution;
        // TODO optional: create better reportString
        String reportString = "";
        report.afterExecution(executionTimes, totalExecutionTime, reportString, dataReceived);
        // Flush the remain data
        if (diskLog) {
            flushData();
            try {
                fileChannel.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
        // Store the report in controller to send later to master
        clientManager.addReport(clientID, report);
        // Free Memory (GB Collect later)
        history = null;
        historyCounter = null;
    }

    public void flushData() {
        ByteBuffer separator = ByteBuffer.wrap("\n--------\n".getBytes());
        long separatorSize = separator.capacity();
        while (flushedRequests < responseData.length && responseData[flushedRequests] != null) {

            long written = responseData[flushedRequests].capacity();

            separator.rewind();
            responseData[flushedRequests].rewind();

            fileChannel.write(responseData[flushedRequests], flushedFilePosition);
            flushedFilePosition += written;
            fileChannel.write(separator, flushedFilePosition);
            flushedFilePosition += separatorSize;
            responseData[flushedRequests] = null;

            flushedRequests++;
        }
    }


    public void setStartOptions(List<StartOpt> optList) {
        for (StartOpt opt : optList) {
            switch (opt.getNumber()) {
            case StartOpt.Disk_VALUE:
                diskLog = true;
                int slavePort = SlaveMain.SLAVE_URI.getPort();
                initAsyncFile(slavePort);
                break;
            default:
                log.error("Unknown start option");
                break;
            }
        }
    }

    public void initAsyncFile(int slavePort) {
        Path file = Paths.get("responses/" + slavePort + "/" + clientID);
        try {
            // create dirs
            file.toFile().getParentFile().mkdirs();
            // clean old file
            file.toFile().delete();
            // Open the socket to write
            fileChannel = AsynchronousFileChannel.open(file,
                                                       StandardOpenOption.CREATE,
                                                       StandardOpenOption.WRITE,
                                                       StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error(e);
        }
    }

}
