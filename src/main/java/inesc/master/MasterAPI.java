package inesc.master;

import inesc.shared.AppEvaluationProtos;
import inesc.shared.AppEvaluationProtos.AppResponse;
import inesc.shared.AppEvaluationProtos.AppResponse.ResStatus;
import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;
import inesc.shared.AppEvaluationProtos.SlaveRegistryMsg;

import java.net.URI;
import java.util.LinkedList;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;

@Path("/master")
public class MasterAPI {
    private static Logger log = Logger.getLogger(MasterAPI.class);
    private static final LinkedList<ReportAgregatedMsg> reports = new LinkedList<ReportAgregatedMsg>();

    @GET
    @Produces("text/plain")
    public String getReports() {
        StringBuilder sb = new StringBuilder();
        for (ReportAgregatedMsg msg : reports) {
            sb.append(msg);
            sb.append("-----------------------------------------\n");
        }
        return sb.toString();
    }

    @POST
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public AppResponse reflect(AppEvaluationProtos.ReportAgregatedMsg reportList) {
        log.info(reportList);
        reports.add(reportList);
        return AppResponse.newBuilder().setStatus(ResStatus.OK).build();
    }

    @POST
    @Path("registry")
    @Consumes("application/x-protobuf")
    public void registry(SlaveRegistryMsg registryMsg) {
        URI uri = UriBuilder.fromUri(registryMsg.getUrl())
                            .port(registryMsg.getPort())
                            .build();
        MasterMain.puppetMaster.addNewClient(uri);
    }
}
