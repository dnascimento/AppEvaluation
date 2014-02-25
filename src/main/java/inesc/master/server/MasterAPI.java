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
 * Master API for slaves
 * 
 * @author darionascimento
 */
@Path("/master")
public class MasterAPI {
    private static Logger log = Logger.getLogger(MasterAPI.class);

    @GET
    @Produces("text/plain")
    public String getReports() {
        return MasterMain.puppetMaster.getReports();
    }

    @POST
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public AppResponse reflect(AppEvaluationProtos.ReportAgregatedMsg reportList) {
        log.info(reportList);
        MasterMain.puppetMaster.addReport(reportList);
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
        URI uri = UriBuilder.fromUri(registryMsg.getUrl())
                            .port(registryMsg.getPort())
                            .build();
        MasterMain.puppetMaster.addNewSlave(uri);
    }
}
