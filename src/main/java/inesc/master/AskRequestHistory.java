package inesc.master;

import inesc.master.server.MasterService;
import inesc.shared.AppEvaluationProtos.AppReqList;

/**
 * Perform requests on server using the Master client and AskInterface
 * This is the defacto class here the users can change which actions are executed.
 * 
 * @author darionascimento
 */
public class AskRequestHistory extends
        Thread {


    /**
     * New thread to create the execution histories and require the client to start
     */
    @Override
    public void run() {
        try {
            // Secure time to let every slave stable, no horries
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AppReqList.Builder reqSequence = AppReqList.newBuilder();

        // Create the request list from AskInterface
        // reqSequence.addRequests(AskInterface.getGoogle(100));
        reqSequence.addRequests(AskInterface.getHomepage(100));
        // reqSequence.addRequests(AskInterface.getNewQuestion(1));
        // reqSequence.addRequests(AskInterface.postNewQuestion(1,
        // "Teste",
        // "fixe",
        // "ADOREI FUNCIONAR"));

        AppReqList reqList = reqSequence.setNClients(25).build();

        // Send the request list using puppet
        MasterService.puppetMaster.sendRequest(reqList);
        // Start Execution
        // MasterMain.puppetMaster.start(StartOpt.Disk);
        MasterService.puppetMaster.start();
    }
}
