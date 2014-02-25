package inesc.master;

import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.shared.AppEvaluationProtos.AppRequest.ReqType;



/**
 * Generate request to Invoke the website (Highest level API)
 * 
 * @author darionascimento
 */
public class AskInterface extends
        Thread {


    public static AppRequest getHomepage(int nExec) {
        return AppRequest.newBuilder()
                         .setType(ReqType.GET)
                         .setNExec(nExec)
                         .setUrl("http://localhost:8888/")
                         .build();
    }


}
