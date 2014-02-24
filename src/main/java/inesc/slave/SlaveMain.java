package inesc.slave;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

public class SlaveMain {
    public static URI SLAVE_URI = UriBuilder.fromUri("http://localhost/")
                                            .port(9998)
                                            .build();
    private static Logger log = Logger.getLogger(SlaveMain.class);
    private final static int PORT_RANGE_MIN = 9000;
    private final static int PORT_RANGE_MAX = 9200;


    public static void main(String[] args) throws IOException {
        DOMConfigurator.configure("log4j.xml");

        int port = 9998;// getFreePort();
        String path = "http://localhost/";
        // SLAVE_URI =

        log.info("Starting slave....");
        SelectorThread threadSelector = createServer(SLAVE_URI);
        log.info("Client at " + SLAVE_URI);


        SlaveAPI.clientManager.register(path, port);

        log.info("Hit enter to stop it...");
        System.in.read();
        threadSelector.stopEndpoint();
    }



    public static SelectorThread createServer(URI uri) throws IOException {
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.packages",
                       "inesc.slave; inesc.share");
        return GrizzlyWebContainerFactory.create(uri, initParams);
    }

    public static int getFreePort() {
        // Pick Port
        Random rand = new Random();
        int port;
        while (true) {
            port = (rand.nextInt(PORT_RANGE_MAX - PORT_RANGE_MIN)) + PORT_RANGE_MIN;
            // Test if port is free
            try {
                ServerSocket sock = new ServerSocket(port);
                sock.close();
                return port;
            } catch (Exception e) {
                // IGNORE
            }
        }
    }









}
