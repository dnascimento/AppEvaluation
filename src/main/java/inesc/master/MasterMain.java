package inesc.master;

import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.slave.SlaveMain;

import java.net.URI;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;



/**
 * Create execution stories and send the stories for invocation
 * 
 * @author darionascimento
 */
public class MasterMain {
    private static Logger log = Logger.getLogger(MasterMain.class);

    public static void main(String[] args) {
        DOMConfigurator.configure("log4j.xml");

        URI url = SlaveMain.BASE_URI;
        Master puppetMaster = new Master(url);

        // Create the request list from AskInterface
        AppRequest req = AskInterface.getHomepage();
        AppReqList list = AppReqList.newBuilder().addRequests(req).setNClients(1).build();

        // Send the request list using puppet
        puppetMaster.sendRequest(list);
        // Start Execution
        puppetMaster.start();
    }
}
