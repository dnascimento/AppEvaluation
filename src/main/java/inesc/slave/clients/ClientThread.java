/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.slave.clients;

import inesc.shared.AppEvaluationProtos.AppStartMsg.StartOpt;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * Individual whose perform resquests on server
 */
public abstract class ClientThread extends
        Thread {
    static Logger log = Logger.getLogger(ClientThread.class);

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
    private final long dataReceived = 0;

    long totalRequests;

    long startExecution;

    /** Async log file for flush the responses */
    private AsynchronousFileChannel fileChannel = null;

    /** Flag to flush or not the responses */
    private boolean diskLog = false;

    private final DefaultBHttpClientConnection conn;
    private final HttpRequestExecutor httpexecutor;
    private final ConnectionReuseStrategy connStrategy;
    private final HttpHost host;
    private final HttpCoreContext coreContext;
    private final HttpProcessor httpproc;

    ClientThread(int clientId, URL targetHost, ClientManager clientManager) {
        httpproc = HttpProcessorBuilder.create()
                                       .add(new RequestContent())
                                       .add(new RequestTargetHost())
                                       .add(new RequestConnControl())
                                       .add(new RequestUserAgent("Shuttle/1.1"))
                                       .add(new RequestExpectContinue(false))
                                       .build();

        httpexecutor = new HttpRequestExecutor();
        coreContext = HttpCoreContext.create();
        host = new HttpHost(targetHost.getHost(), targetHost.getPort());
        coreContext.setTargetHost(host);
        conn = new DefaultBHttpClientConnection(8 * 1024);
        connStrategy = DefaultConnectionReuseStrategy.INSTANCE;

        this.clientID = clientId;
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


    private HttpResponse execute(HttpRequestBase request) throws UnknownHostException, IOException, HttpException {
        if (!conn.isOpen()) {
            Socket socket = new Socket(host.getHostName(), host.getPort());
            conn.bind(socket);
        }

        // request = new HttpGet("/test");
        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        if (!connStrategy.keepAlive(response, coreContext)) {
            conn.close();
        }

        return response;
    }

    public void execRequest(HttpRequestBase req) {
        HttpResponse response;
        try {
            long start = System.currentTimeMillis();
            response = execute(req);
            long duration = (System.currentTimeMillis() - start);
            executionTimes.add((short) duration);
            if (response == null) {
                return;
            }


            // System.out.println("<< Response: " + response.getStatusLine());
            String data = EntityUtils.toString(response.getEntity());

            // responseData.add(data);
            // dataReceived += data.length();
            // if (diskLog && dataReceived > 10) {
            // // To save memory and avoid head problems, flush async
            // flushData();
            // }
        } catch (NoHttpResponseException e) {
            // Server received the request but due to overload do not reply
            log.error("No HTTP Response", e);
            executionTimes.add((short) -1);

        } catch (ConnectionPoolTimeoutException e) {
            // multithreaded connection manager fails to obtain a free connection
            log.warn("Connection Pool Timeout Exception");
            executionTimes.add((short) -2);

        } catch (ConnectTimeoutException e) {
            try {
                sleep(500);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            // unable to establish a connection
            log.warn("Connect Timeout Exception");
            executionTimes.add((short) -3);

        } catch (Exception e) {
            log.warn(e);
            log.error(e);
            e.printStackTrace();
            executionTimes.add((short) -4);
        }

    }
}
