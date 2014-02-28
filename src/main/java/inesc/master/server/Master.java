package inesc.master.server;

import inesc.master.AskRequestHistory;
import inesc.share.ProtobufProviders;
import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.shared.AppEvaluationProtos.AppStartMsg;
import inesc.shared.AppEvaluationProtos.AppStartMsg.StartOpt;
import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * Responsible for:
 * - Track existing slaves
 * - Send requests to every slave
 * - Collect responses
 * 
 * @author darionascimento
 */
public class Master {
    Client c;
    private static Logger log = Logger.getLogger(Master.class);
    private final LinkedList<URI> slaves = new LinkedList<URI>();
    private static final LinkedList<ReportAgregatedMsg> reports = new LinkedList<ReportAgregatedMsg>();

    /** how many slaves should registry before start actions */
    public static final int EXPECTED_SLAVES = 2;

    int clientCount = 0;


    public Master() {

        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyReader.class);
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyWriter.class);
        c = Client.create(cc);
    }

    public void addNewSlave(URI uri) {
        log.info("New slave registered:" + uri);
        slaves.add(uri);
        if (++clientCount == EXPECTED_SLAVES) {
            log.info("All slaves Registered");
            startRequests();
        }
    }


    /** Invoked after slave registry */
    public void startRequests() {
        log.info("Will start requests...");
        new AskRequestHistory().start();
    }

    /** Send request to every slave */
    public void sendRequest(AppReqList requestList) {
        sendRequest(requestList, slaves.size());
    }

    /**
     * Send request to nodes
     * 
     * @param requestList
     * @param numberOfSlaves
     */
    public void sendRequest(AppReqList requestList, int numberOfSlaves) {
        for (URI nodeURI : slaves) {
            if (numberOfSlaves-- == 0) {
                break;
            }
            WebResource wr = c.resource(nodeURI);
            wr = wr.path("requests");
            try {
                wr.type("application/x-protobuf").post(requestList);
            } catch (ClientHandlerException e) {
                log.error("Connection refused " + wr.getURI());
            }
        }

    }

    /**
     * Order slaves to start the request
     * 
     * @param logOptins Record on Disk
     */
    public void start(StartOpt... logOptions) {
        // TODO start only the nodes with requests
        for (URI nodeURI : slaves) {
            WebResource wr = c.resource(nodeURI);
            wr = wr.path("start");
            try {
                AppStartMsg startMsg = AppStartMsg.newBuilder()
                                                  .addAllOpt(Arrays.asList(logOptions))
                                                  .build();
                AppResponse res = wr.type("application/x-protobuf")
                                    .post(AppResponse.class, startMsg);
                if (res.getStatus().equals(AppResponse.ResStatus.OK)) {
                    log.info("Master: Process Start");
                } else {
                    log.error("Master: Error to start process");
                }
            } catch (ClientHandlerException e) {
                log.error("Connection refused " + wr.getURI());
            }
        }
    }

    /**
     * New report from slave
     * 
     * @param reportList
     */
    public void addReport(ReportAgregatedMsg reportList) {
        reports.add(reportList);
    }

    /**
     * Get the reports collected until now
     * 
     * @return
     */
    public String getReports() {
        StringBuilder sb = new StringBuilder();
        for (ReportAgregatedMsg msg : reports) {
            sb.append(msg);
            sb.append("-----------------------------------------\n");
        }
        return sb.toString();
    }


}
