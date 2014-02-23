package inesc;

import inesc.restAPI.AddressBookProtos;
import inesc.restAPI.Main;
import inesc.restAPI.ProtobufProviders;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import junit.framework.TestCase;

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
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.packages", "inesc.restAPI");
        threadSelector = GrizzlyWebContainerFactory.create(UriBuilder.fromUri("http://localhost/")
                                                                     .port(9998)
                                                                     .build(),
                                                           initParams);
        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyReader.class);
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyWriter.class);
        Client c = Client.create(cc);
        r = c.resource(Main.BASE_URI);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        threadSelector.stopEndpoint();
    }

    public void testUsingJerseyClient() {
        WebResource wr = r.path("person");
        AddressBookProtos.Person p = wr.get(AddressBookProtos.Person.class);
        assertEquals("Sam", p.getName());

        AddressBookProtos.Person p2 = wr.type("application/x-protobuf")
                                        .post(AddressBookProtos.Person.class, p);
        assertEquals(p, p2);
    }

    public void testUsingURLConnection() throws IOException {
        AddressBookProtos.Person person;
        {
            URL url = new URL("http://localhost:9998/person");
            URLConnection urlc = url.openConnection();
            urlc.setDoInput(true);
            urlc.setRequestProperty("Accept", "application/x-protobuf");
            person = AddressBookProtos.Person.newBuilder()
                                             .mergeFrom(urlc.getInputStream())
                                             .build();
            assertEquals("Sam", person.getName());
        }
        {
            URL url = new URL("http://localhost:9998/person");
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setDoInput(true);
            urlc.setDoOutput(true);
            urlc.setRequestMethod("POST");
            urlc.setRequestProperty("Accept", "application/x-protobuf");
            urlc.setRequestProperty("Content-Type", "application/x-protobuf");
            person.writeTo(urlc.getOutputStream());
            AddressBookProtos.Person person2 = AddressBookProtos.Person.newBuilder()
                                                                       .mergeFrom(urlc.getInputStream())
                                                                       .build();
            assertEquals(person, person2);
        }
    }
}
