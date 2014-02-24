package inesc.master;

import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.slave.RequestsService;
import inesc.slave.SlaveMain;

import java.net.URI;

import org.apache.log4j.Logger;



/**
 * Create execution stories and send the stories for invocation
 * 
 * @author darionascimento
 */
public class MasterMain {
    private static Logger log = Logger.getLogger(RequestsService.class);

    public static void main(String[] args) {

        URI url = SlaveMain.BASE_URI;
        Master puppetMaster = new Master(url);

        // Create the request list from AskInterface
        AppRequest req = AskInterface.getHomepage();
        AppReqList list = AppReqList.newBuilder().addRequests(req).build();

        // Send the request list using puppet
        puppetMaster.sendRequest(list);
        // Start Execution
        puppetMaster.start();
    }
}
