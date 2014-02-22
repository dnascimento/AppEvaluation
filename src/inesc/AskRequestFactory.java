package inesc;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;

public class AskRequestFactory {

    // URL, TYPE, ARGS LIST
    HttpRequest createRequest(String url, ReqType type, String... args) {
        HttpPost request = null;
        switch (type) {
        case GET:
            request = new HttpGet(url);
            break;
        case PUT:
            request = new HttpPut(url);
            break;
        case POST:
            request = new HttpPost(url);
            request.
            break;
        case DELETE:
            request = new HttpDelete(url);
            break;
        }
        request
        
        return request;
    }
}
