package inesc.master;

import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppRequest;

// Execute requests on server
public class AskRequestHistory extends
        Thread {



    @Override
    public void run() {
        try {
            sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Create the request list from AskInterface
        AppRequest req = AskInterface.getHomepage(20);
        AppReqList list = AppReqList.newBuilder().addRequests(req).setNClients(1).build();

        // Send the request list using puppet
        MasterMain.puppetMaster.sendRequest(list);
        // Start Execution
        MasterMain.puppetMaster.start();
    }
}
