package inesc.master;

import inesc.share.ProtobufProviders;
import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.slave.SlaveMain;

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
    private final LinkedList<URI> clients = new LinkedList<URI>();
    int clientCount = 0;
    private final WebResource r;


    public Master() {

        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyReader.class);
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyWriter.class);
        c = Client.create(cc);
        r = c.resource(SlaveMain.SLAVE_URI);

    }

    public void addNewClient(URI uri) {
        log.info("New client registered:" + uri);
        clients.add(uri);
        if (++clientCount == MasterMain.EXPECTED_SLAVES) {
            log.info("All Clients Registered");
            MasterMain.startRequests();
        }
    }



    public void sendRequest(AppReqList requestList) {
        WebResource wr = c.resource(SlaveMain.SLAVE_URI);
        wr = wr.path("requests");
        try {
            wr.type("application/x-protobuf").post(requestList);
        } catch (ClientHandlerException e) {
            log.error("Connection refused " + wr.getURI());
        }

    }

    public void start() {
        // for (URI client : clients) {
        // WebResource wr = c.resource(client);
        WebResource wr = r.path("requests");
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
        // }
    }



}
