package inesc.slave;

import inesc.shared.AppEvaluationProtos;
import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.shared.AppEvaluationProtos.AppResponse.ResStatus;
import inesc.shared.AppEvaluationProtos.Parameter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

@Path("/resources")
public class RequestsService {
    private static Logger log = Logger.getLogger(RequestsService.class);
    ClientManager clientManager;

    // /////////// SERVER ///////////////////
    public RequestsService() {
        clientManager = new ClientManager();
    }




    @GET
    @Path("/start")
    @Produces("application/x-protobuf")
    public AppEvaluationProtos.AppResponse start() {
        // Async start the clients
        clientManager.start();
        return AppResponse.newBuilder().setStatus(ResStatus.OK).build();
    }

    @POST
    @Path("/requests")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public AppResponse reflect(AppEvaluationProtos.AppReqList reqList) {
        int nRequests = reqList.getRequestsCount();
        int nClients = reqList.getNClients();
        System.out.println("Got " + nRequests + " requests for " + nClients + " clients");
        return AppResponse.newBuilder().setStatus(ResStatus.OK).build();

        // HttpRequestBase[] history = new HttpRequestBase[nRequests];
        // int[] historyCounter = new int[nRequests];
        // int i = 0;
        // for (AppRequest req : reqList.getRequestsList()) {
        // try {
        // history[i] = convertReqBufferToHTTPRequest(req);
        // historyCounter[i] = req.getNExec();
        // i++;
        // } catch (UnsupportedEncodingException e) {
        // log.error("Unsupported Encoding");
        // }
        // }
        // clientManager.newClient(history, historyCounter);
        // return AppResponse.newBuilder().setStatus(ResStatus.OK).build();
    }

    /**
     * Convert Protocol Buffer to HTTP Package
     * 
     * @param reqBuffer
     * @return HTTPRequest package
     * @throws UnsupportedEncodingException
     */
    public HttpRequestBase convertReqBufferToHTTPRequest(AppRequest reqBuffer) throws UnsupportedEncodingException {
        // Convert parameters to from Buffer to HTTP
        Parameters params = new Parameters(reqBuffer.getParametersList());
        String url = reqBuffer.getUrl();
        HttpRequestBase request = null;

        switch (reqBuffer.getType()) {
        case GET:
            request = new HttpGet(url);
            break;
        case DELETE:
            request = new HttpDelete(url);
            break;
        case PUT:
            HttpPut put = new HttpPut(url);
            put.setEntity(new UrlEncodedFormEntity(params.build()));
            request = put;
            break;
        case POST:
            HttpPost post = new HttpPost(url);
            post.setEntity(new UrlEncodedFormEntity(params.build()));
            request = post;
            break;
        }
        return request;
    }

    /**
     * Convert Protobuff parameters to HTTP
     * 
     * @author darionascimento
     */
    public class Parameters {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

        public Parameters(List<Parameter> parametersList) {
            for (Parameter param : parametersList) {
                this.add(param.getKey(), param.getValue());
            }
        }

        public Parameters add(String name, String value) {
            params.add(new BasicNameValuePair(name, value));
            return this;
        }

        public ArrayList<NameValuePair> build() {
            return params;
        }
    }




}