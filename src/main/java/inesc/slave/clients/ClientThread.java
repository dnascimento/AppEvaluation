/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.slave.clients;

import inesc.shared.AppEvaluationProtos.AppStartMsg.StartOpt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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
abstract class ClientThread extends
        Thread {
    static Logger log = Logger.getLogger(ClientThread.class);
    protected CloseableHttpClient httpClient;
    private final HttpContext context;

    /** Client unique ID */
    protected int clientID;


    /** Collect the execution round trip time */
    protected ArrayList<Short> executionTimes;

    /** Collect the responses */
    protected ArrayList<ByteBuffer> responseData;

    private int flushedRequests = 0;

    private long flushedFilePosition;


    /** Summary of execution */
    public ThreadReport report;

    /** Controller */
    protected ClientManager clientManager;

    /** Bytes received counter */
    private long dataReceived = 0;

    int totalRequests;

    long startExecution;

    /** Async log file for flush the responses */
    private AsynchronousFileChannel fileChannel = null;

    /** Flag to flush or not the responses */
    private boolean diskLog = false;

    ClientThread(CloseableHttpClient httpClient, int clientID, ClientManager clientManager) {
        context = new BasicHttpContext();
        this.httpClient = httpClient;
        this.clientManager = clientManager;
    }





    void initStatistics() {
        startExecution = System.currentTimeMillis();
    }


    void collectStatistics() {
        long totalExecutionTime = System.currentTimeMillis() - startExecution;
        // TODO optional: create better reportString
        String reportString = "";
        ThreadReport report = new ThreadReport(totalRequests, clientID, executionTimes, totalExecutionTime, reportString, dataReceived);
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

    }

    public void flushData() {
        ByteBuffer separator = ByteBuffer.wrap("\n--------\n".getBytes());
        long separatorSize = separator.capacity();
        while (flushedRequests < responseData.size() && responseData.get(flushedRequests) != null) {

            long written = responseData.get(flushedRequests).capacity();

            separator.rewind();
            responseData.get(flushedRequests).rewind();

            fileChannel.write(responseData.get(flushedRequests), flushedFilePosition);
            flushedFilePosition += written;
            fileChannel.write(separator, flushedFilePosition);
            flushedFilePosition += separatorSize;
            responseData.set(flushedRequests, null);
            flushedRequests++;
        }
    }

    public void setStartOptions(List<StartOpt> optList) {
        for (StartOpt opt : optList) {
            switch (opt.getNumber()) {
            case StartOpt.Disk_VALUE:
                diskLog = true;
                int slavePort = clientManager.slave.myAddress.getPort();
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



    public void execRequest(HttpRequestBase req) {
        CloseableHttpResponse response;
        try {
            long start = System.currentTimeMillis();
            response = httpClient.execute(req, context);
            long duration = (System.currentTimeMillis() - start);
            executionTimes.add((short) duration);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                ByteBuffer resp = ByteBuffer.wrap(EntityUtils.toByteArray(entity));
                responseData.add(resp);
                dataReceived += resp.capacity();
                if (diskLog && dataReceived > 10) {
                    // To save memory and avoid head problems, flush async
                    flushData();
                }
            }
        } catch (NoHttpResponseException e) {
            // Server received the request but due to overload do not reply
            log.warn("No HTTP Response");
            executionTimes.add((short) -1);

        } catch (ConnectionPoolTimeoutException e) {
            // multithreaded connection manager fails to obtain a free connection
            log.warn("Connection Pool Timeout Exception");
            executionTimes.add((short) -2);

        } catch (ConnectTimeoutException e) {
            // unable to establish a connection
            log.warn("Connect Timeout Exception");
            executionTimes.add((short) -3);

        } catch (Exception e) {
            log.warn(e);
            e.printStackTrace();
            executionTimes.add((short) -4);
        }

    }

}
