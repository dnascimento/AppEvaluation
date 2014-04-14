/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.slave.server;

import inesc.shared.AppEvaluationProtos;
import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.shared.AppEvaluationProtos.AppResponse.ResStatus;
import inesc.shared.AppEvaluationProtos.Parameter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
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
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

/**
 * Slave API for Master
 * 
 * @author darionascimento
 */
@Path("/")
public class SlaveService {
    private static final String JSON = "application/json";
    private static Logger log = Logger.getLogger(SlaveService.class);
    static Slave slave = new Slave();

    /**
     * Order the begin of execution
     * 
     * @return OK
     */
    @POST
    @Path("start")
    @Produces("application/x-protobuf")
    public AppEvaluationProtos.AppResponse start(AppEvaluationProtos.AppStartMsg msg) {
        // Async start the clients
        slave.clientManager.setStartOptions(msg.getOptList());
        slave.clientManager.start();
        return AppResponse.newBuilder().setStatus(ResStatus.OK).build();
    }

    /**
     * Set the requests to perform.
     * 
     * @param reqList: list of HTTP requests
     */
    @POST
    @Path("requests")
    @Consumes("application/x-protobuf")
    public void receiveRequestList(AppEvaluationProtos.AppReqList reqList) {
        slave.resetClientManager();
        int nRequests = reqList.getRequestsCount();
        int nClients = reqList.getNClients();
        System.out.println("Got " + nRequests + " requests for " + nClients + " clients");

        HttpRequestBase[] history = new HttpRequestBase[nRequests];
        short[] historyCounter = new short[nRequests];
        int i = 0;
        for (AppRequest req : reqList.getRequestsList()) {
            try {
                history[i] = convertReqBufferToHTTPRequest(req);
                historyCounter[i] = (short) req.getNExec();
                i++;
            } catch (UnsupportedEncodingException e) {
                log.error("Unsupported Encoding");
            }
        }
        addNClients(nClients, history, historyCounter);
    }

    /**
     * Create the client threads and prepare the runner
     * 
     * @param nClients
     * @param history
     * @param counterOrg
     */
    private void addNClients(int nClients, HttpRequestBase[] history, short[] counterOrg) {
        for (int i = 0; i < nClients; i++) {
            short[] counter = Arrays.copyOf(counterOrg, counterOrg.length);
            slave.clientManager.newClient(history, counter);
        }
    }





    /**
     * Convert Protocol Buffer to HTTP Package, the object used by HTTP Client
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
            if (reqBuffer.hasContentType() && reqBuffer.getContentType().equals(JSON)) {
                put.setHeader("Content-Type", JSON);
                put.setEntity(parametersToJson(reqBuffer.getParametersList()));
            } else {
                put.setEntity(new UrlEncodedFormEntity(params.build()));
            }
            request = put;
            break;
        case POST:
            HttpPost post = new HttpPost(url);
            if (reqBuffer.hasContentType() && reqBuffer.getContentType().equals(JSON)) {
                post.setHeader("Content-Type", JSON);
                post.setEntity(parametersToJson(reqBuffer.getParametersList()));
            } else {
                post.setEntity(new UrlEncodedFormEntity(params.build()));
            }
            request = post;
            break;
        }
        return request;
    }

    private StringEntity parametersToJson(List<Parameter> parametersList) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"");
        boolean first = true;
        for (Parameter p : parametersList) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(p.getKey());
            sb.append("\":\"");
            sb.append(p.getValue());
            sb.append("\"");
        }
        sb.append("}");
        return new StringEntity(sb.toString());
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
