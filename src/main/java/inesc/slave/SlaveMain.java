package inesc.slave;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.PropertyConfigurator;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

public class SlaveMain {
    public static final URI BASE_URI = UriBuilder.fromUri("http://localhost/")
                                                 .port(9998)
                                                 .build();



    public static void main(String[] args) throws IOException {
        PropertyConfigurator.configure("log4j.properties");

        System.out.println("Starting grizzly....");
        URI uri = BASE_URI;
        SelectorThread threadSelector = createServer(uri);
        System.out.println(String.format("Try out %sperson\nHit enter to stop it...", uri));
        System.in.read();
        threadSelector.stopEndpoint();
    }

    public static SelectorThread createServer(URI uri) throws IOException {
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.packages", "inesc.slave");
        return GrizzlyWebContainerFactory.create(uri, initParams);
    }


}
