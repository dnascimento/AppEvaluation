/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.master.server;

import inesc.shared.AppEvaluationProtos;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.shared.AppEvaluationProtos.AppResponse.ResStatus;
import inesc.shared.AppEvaluationProtos.SlaveRegistryMsg;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;

/**
 * Master API for slaves.
 * Holds the PuppetMaster: the server controller
 * 
 * @author darionascimento
 */
@Path("/master")
public class MasterService {
    private static Logger log = Logger.getLogger(MasterService.class);

    /** The domain controller - standalone */
    public static Master puppetMaster = new Master();

    /**
     * HTTP Interface for browsers to collect reports
     * 
     * @return
     */
    @GET
    @Produces("text/plain")
    public String getReports() {
        return puppetMaster.getReports();
    }

    /**
     * REST Interface for slaves to add the reports
     * 
     * @param reportList
     * @return OK
     */
    @POST
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public AppResponse reflect(AppEvaluationProtos.ReportAgregatedMsg reportList) {
        log.info(reportList);
        puppetMaster.addReport(reportList);
        return AppResponse.newBuilder().setStatus(ResStatus.OK).build();
    }

    /**
     * Registry new slave
     * 
     * @param registryMsg
     */
    @POST
    @Path("registry")
    @Consumes("application/x-protobuf")
    public void registry(SlaveRegistryMsg registryMsg) {
        URI uri = UriBuilder.fromUri(registryMsg.getUrl()).port(registryMsg.getPort()).build();
        puppetMaster.addNewSlave(uri);
    }
}
