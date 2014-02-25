package inesc.master.server;

import inesc.share.ProtobufProviders;
import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;

import java.net.URI;
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
        if (++clientCount == MasterMain.EXPECTED_SLAVES) {
            log.info("All slaves Registered");
            MasterMain.startRequests();
        }
    }

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

    public void start() {
        // TODO start only the nodes with requests
        for (URI nodeURI : slaves) {
            WebResource wr = c.resource(nodeURI);
            wr = wr.path("start");
            try {
                AppResponse res = wr.get(AppResponse.class);
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

    public void addReport(ReportAgregatedMsg reportList) {
        reports.add(reportList);
    }

    public String getReports() {
        StringBuilder sb = new StringBuilder();
        for (ReportAgregatedMsg msg : reports) {
            sb.append(msg);
            sb.append("-----------------------------------------\n");
        }
        return sb.toString();
    }


}
