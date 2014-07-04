/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.master.commands;

import inesc.master.Master;
import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppRequest;


/**
 * Perform requests on server using the Master client and AskInterface
 * This is the class here the users can change which actions are executed.
 * 
 * @author darionascimento
 */
public class AskRequestStory {

    AppReqList.Builder reqSequence;
    Master master;


    public AskRequestStory(Master master) {
        reqSequence = AppReqList.newBuilder();
        this.master = master;
    }

    public void addRequest(AppRequest req) {
        reqSequence.addRequests(req);
    }

    public AppReqList build(int nClientsPerNode) {
        return reqSequence.setNClients(nClientsPerNode).build();
    }
}
