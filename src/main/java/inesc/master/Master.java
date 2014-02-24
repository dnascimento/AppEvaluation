package inesc.master;

import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.slave.ProtobufProviders;

import java.net.URI;

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
    private final WebResource r;
    private static Logger log = Logger.getLogger(Master.class);



    public Master(URI url) {
        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyReader.class);
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyWriter.class);
        Client c = Client.create(cc);
        r = c.resource(url);
    }

    public void sendRequest(AppReqList requestList) {
        WebResource wr = null;
        try {
            wr = r.path("requests");
            AppResponse res = wr.type("application/x-protobuf").post(AppResponse.class,
                                                                     requestList);
            System.out.println(res);
        } catch (ClientHandlerException e) {
            log.error("Connection refused " + wr.getURI());
        }
    }

    public void start() {
        WebResource wr = r.path("requests");
        AppResponse res = wr.get(AppResponse.class);
        if (res.getStatus().equals(AppResponse.ResStatus.OK)) {
            log.info("Master: Process Start");
        } else {
            log.error("Master: Error to start process");
        }

    }
}
