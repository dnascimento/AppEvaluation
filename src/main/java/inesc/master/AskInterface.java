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


    public static AppRequest getHomepage() {
        return AppRequest.newBuilder()
                         .setType(ReqType.GET)
                         .setNExec(1)
                         .setUrl("http://google.pt")
                         .build();
    }
}
