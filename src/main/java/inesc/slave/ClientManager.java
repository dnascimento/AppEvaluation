package inesc.slave;


import inesc.share.ProtobufProviders;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.shared.AppEvaluationProtos.AppStartMsg.StartOpt;
import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;
import inesc.shared.AppEvaluationProtos.SlaveRegistryMsg;
import inesc.slave.reports.ThreadReport;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * Manages the set of ClientThreads. Each thread represents a distinct parallel client
 * 
 * @author darionascimento
 */
public class ClientManager extends
        Thread {
    private static Logger log = Logger.getLogger(ClientManager.class);

    public static final int SOCKET_TIMEOUT = 5000;
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int REQUEST_TIMEOUT = 5000;

    /* Max number of concurrent threads */
    public static final int MAX_CONNECTIONS_TOTAL = 200;
    /* Max number of concurrent threads using same route */
    public static final int MAX_CONNECTIONS_PER_ROUTE = 200;

    /* milisecounds delay */
    public static final int DELAY_BETWEEN_REQUESTS = 10;

    private final LinkedList<ClientThread> clientThreads = new LinkedList<ClientThread>();
    private ThreadReport[] clientReports;
    private CloseableHttpClient httpClient;
    private int id;
    private final WebResource r;


    public ClientManager(URI masterURI) {
        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyReader.class);
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyWriter.class);
        Client c = Client.create(cc);
        r = c.resource(masterURI);

        restart();
    }

    public void restart() {
        clientThreads.clear();
        clientReports = null;
        /*
         * Perform explicit garbage collection to remove old client threads and reports.
         * System.gc();
         */
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(MAX_CONNECTIONS_TOTAL);
        cm.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                                                          .setSocketTimeout(SOCKET_TIMEOUT)
                                                          .setConnectTimeout(CONNECTION_TIMEOUT)
                                                          .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                                                          .setStaleConnectionCheckEnabled(true)
                                                          .build();

        httpClient = HttpClients.custom()
                                .setConnectionManager(cm)
                                .setDefaultRequestConfig(defaultRequestConfig)
                                .build();
        id = 0;
    }

    public void newClient(HttpRequestBase[] history, short[] historyCounter) {
        ClientThread thread = new ClientThread(httpClient, history, historyCounter, id++,
                this);
        clientThreads.add(thread);
        log.info("New Client with " + history.length + " requests");
    }

    /**
     * Start all clients at same time
     */
    @Override
    public void run() {
        clientReports = new ThreadReport[id];
        log.info("Starting " + clientThreads.size() + "Clients....");

        // start the threads
        for (ClientThread thread : clientThreads) {
            thread.start();
        }

        // join the threads
        for (ClientThread thread : clientThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.error("Interrupted Execution" + e);
            }
        }

        try {
            httpClient.close();
        } catch (IOException e) {
            log.error("Error closing HTTP Client" + e);
        }
        log.info("Clients done...");
        showReports();
        sendReportToMaster();
        // Clean the threads and connections
        this.restart();

    }

    private void sendReportToMaster() {
        ReportAgregatedMsg.Builder bd = ReportAgregatedMsg.newBuilder();
        for (int i = 0; i < clientReports.length; i++) {
            bd.addReports(clientReports[i].toProtBuffer());
        }

        WebResource wr = r.path("master");
        AppResponse res = wr.type("application/x-protobuf").post(AppResponse.class,
                                                                 bd.build());
        log.info(res);
    }

    private void showReports() {
        for (int i = 0; i < clientReports.length; i++) {
            log.info(clientReports[i]);
        }
    }



    public synchronized void addReport(int clientId, ThreadReport report) {
        clientReports[clientId] = report;
    }

    public void register(String url, int port) {
        WebResource wr = r.path("master/registry");
        SlaveRegistryMsg msg = SlaveRegistryMsg.newBuilder()
                                               .setPort(port)
                                               .setUrl(url)
                                               .build();

        wr.type("application/x-protobuf").post(msg);
        log.info("Client Registerd");
    }

    /** Set the client Thead Execution Options */
    public void setStartOptions(List<StartOpt> optList) {
        for (ClientThread thread : clientThreads) {
            thread.setStartOptions(optList);
        }
    }
}
