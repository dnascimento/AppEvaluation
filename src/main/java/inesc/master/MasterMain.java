package inesc.master;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;



/**
 * Create execution stories and send the stories for invocation
 * 
 * @author darionascimento
 */
public class MasterMain {
    private static Logger log = Logger.getLogger(MasterMain.class);
    public static final URI MASTER_URI = UriBuilder.fromUri("http://localhost/")
                                                   .port(9999)
                                                   .build();

    // how many slaves should registry before start actions
    public static final int EXPECTED_SLAVES = 1;
    public static Master puppetMaster;

    public static void main(String[] args) throws IOException {
        DOMConfigurator.configure("log4j.xml");
        // Start server API
        log.info("Starting Master....");
        SelectorThread threadSelector;
        threadSelector = createServer(MASTER_URI);


        // Start the Master Client
        puppetMaster = new Master();
        log.info("Waiting for slaves registry..");
        log.info("Hit stop it...");
        System.in.read();
        threadSelector.stopEndpoint();
    }

    // Method is invoked by MasterAPI after slave registry
    public static void startRequests() {
        log.info("Will start requests...");
        new AskRequestHistory().start();
    }



    public static SelectorThread createServer(URI uri) throws IOException {
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.packages",
                       "inesc.master; inesc.share");
        return GrizzlyWebContainerFactory.create(uri, initParams);
    }



}
