package inesc.master;

import inesc.master.server.MasterMain;
import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppRequest;

/**
 * Perform requests on server using the Master client and AskInterface
 * This is the defacto class here the users can change which actions are executed.
 * 
 * @author darionascimento
 */
public class AskRequestHistory extends
        Thread {



    @Override
    public void run() {
        try {
            // Secure time to let every slave stable, no horries
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Create the request list from AskInterface
        AppRequest req = AskInterface.getHomepage(50);
        AppReqList list = AppReqList.newBuilder().addRequests(req).setNClients(3).build();

        // Send the request list using puppet
        MasterMain.puppetMaster.sendRequest(list);
        // Start Execution
        // MasterMain.puppetMaster.start(StartOpt.Disk);
        MasterMain.puppetMaster.start();
    }
}
