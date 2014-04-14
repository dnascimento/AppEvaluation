/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.slave.server;

import inesc.master.server.MasterMain;
import inesc.share.ProtobufProviders;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;
import inesc.shared.AppEvaluationProtos.SlaveRegistryMsg;
import inesc.slave.ClientManager;
import inesc.slave.reports.ThreadReport;

import java.net.URI;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class Slave {
    private WebResource r = null;
    private URI masterURI = null;
    public ClientManager clientManager;
    private static Logger log = Logger.getLogger(Slave.class);

    /**
     * Without connection to master
     */
    public Slave() {
        // Initiate the domain controller
        clientManager = new ClientManager(this);
    }

    /**
     * With connection to master
     * 
     * @param slaveURL: To registry
     * @param slavePort: To registry
     */
    public Slave(String slaveURL, int slavePort) {
        this();
        masterURI = MasterMain.MASTER_URI;
        initMasterClient();
        // Register the slave on master
        register(slaveURL, slavePort);
    }

    public void resetClientManager() {
        clientManager = new ClientManager(this);
    }

    /**
     * Init the REST client to connect to master
     */
    private void initMasterClient() {
        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyReader.class);
        cc.getClasses().add(ProtobufProviders.ProtobufMessageBodyWriter.class);
        Client c = Client.create(cc);
        r = c.resource(masterURI);
    }

    /**
     * Register slave on master
     * 
     * @param url
     * @param port
     */
    private void register(String url, int port) {
        WebResource wr = r.path("master/registry");
        SlaveRegistryMsg msg = SlaveRegistryMsg.newBuilder().setPort(port).setUrl(url).build();

        wr.type("application/x-protobuf").post(msg);
        log.info("Client Registerd");
    }




    public void sendReportToMaster(ThreadReport[] clientReports) {
        // If master connection online
        if (r != null)
            sendToMaster(clientReports);

        showReports(clientReports);
    }

    /**
     * Send the reports to master
     */
    private void sendToMaster(ThreadReport[] clientReports) {
        ReportAgregatedMsg.Builder bd = ReportAgregatedMsg.newBuilder();
        for (int i = 0; i < clientReports.length; i++) {
            bd.addReports(clientReports[i].toProtBuffer());
        }
        ReportAgregatedMsg msg = bd.build();
        WebResource wr = r.path("master");
        AppResponse res = wr.type("application/x-protobuf").post(AppResponse.class, msg);
        log.info(res);
    }

    /**
     * List all reports
     */
    private void showReports(ThreadReport[] clientReports) {
        for (int i = 0; i < clientReports.length; i++) {
            log.info(clientReports[i]);
        }
    }

}
