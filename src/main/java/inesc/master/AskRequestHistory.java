/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.master;

import inesc.master.server.MasterService;
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

    AppReqList.Builder reqSequence;
    int nClients = 1;

    public AskRequestHistory() {
        reqSequence = AppReqList.newBuilder();
    }

    public void addRequest(AppRequest req) {
        reqSequence.addRequests(req);
    }

    public void setNClients(int clients) {
        this.nClients = clients;
    }

    public void getHome() {
        reqSequence.addRequests(AskInterface.getHomepage(100));
        this.nClients = 25;
    }

    /**
     * New thread to create the execution histories and require the client to start
     */
    @Override
    public void run() {
        try {
            // Secure time to let every slave stable
            sleep(2000);
            AppReqList reqList = reqSequence.setNClients(nClients).build();

            // Send the request list using puppet
            MasterService.puppetMaster.sendRequest(reqList);
            // Start Execution
            // MasterMain.puppetMaster.start(StartOpt.Disk);
            MasterService.puppetMaster.start();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
