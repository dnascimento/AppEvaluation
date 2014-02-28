package inesc;

import inesc.share.ProtobufProviders;
import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.shared.AppEvaluationProtos.AppRequest.ReqType;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.slave.server.SlaveMain;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import junit.framework.TestCase;

import org.apache.log4j.xml.DOMConfigurator;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;



public class MainTest extends
        TestCase {

    private SelectorThread threadSelector;
    private WebResource r;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        DOMConfigurator.configure("log4j.xml");
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.packages",
                       "inesc.slave; inesc.share");
        threadSelector = GrizzlyWebContainerFactory.create(UriBuilder.fromUri("http://localhost/")
                                                                     .port(9998)
                                                                     .build(),
                                                           initParams);


        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyReader.class);
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyWriter.class);
        Client c = Client.create(cc);
        r = c.resource(SlaveMain.SLAVE_URI);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        threadSelector.stopEndpoint();
    }

    public void testRequestsList() {
        WebResource wr = r.path("requests");

        // AppResponse p = wr.get(AppResponse.class);
        AppRequest req = AppRequest.newBuilder()
                                   .setType(ReqType.GET)
                                   .setNExec(50)
                                   .setUrl("http://google.pt")
                                   .build();
        AppReqList reqList = AppReqList.newBuilder()
                                       .addRequests(req)
                                       .setNClients(1)
                                       .build();
        wr.type("application/x-protobuf").post(reqList);
        // assertEquals(AppResponse.ResStatus.OK, res.getStatus());

        // Start Requests
        wr = r.path("start");
        AppResponse res = wr.get(AppResponse.class);
        assertEquals(AppResponse.ResStatus.OK, res.getStatus());

    }

}
