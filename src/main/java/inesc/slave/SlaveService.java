package inesc.slave;

import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.shared.AppEvaluationProtos.AppStartMsg;
import inesc.shared.AppEvaluationProtos.FromMaster;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;

public class SlaveService extends
        Thread {
    private static Logger log = Logger.getLogger(SlaveService.class);
    private final Slave slave;
    private final ServerSocket server;

    public SlaveService(Slave slave) throws IOException {
        this.slave = slave;
        server = new ServerSocket(slave.myAddress.getPort());
    }


    @Override
    public void run() {
        while (true) {
            Socket s;
            try {
                s = server.accept();
                process(s);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    private void process(Socket s) throws IOException {
        FromMaster msgEnvelop = FromMaster.parseDelimitedFrom(s.getInputStream());
        if (msgEnvelop == null) {
            return;
        }


        if (msgEnvelop.hasFilename()) {
            log.info("New filename to exec: " + msgEnvelop.getFilename());
            slave.newFileToExec(msgEnvelop.getFilename(), new URL(msgEnvelop.getDestination()), s);
        }

        if (msgEnvelop.hasStartMsg()) {
            log.info("Master asked to start...");
            AppStartMsg m2 = msgEnvelop.getStartMsg();
            slave.clientManager.setStartOptions(m2.getOptList());
            slave.clientManager.start();
        }

        if (msgEnvelop.hasReqListMsg()) {
            AppReqList reqList = msgEnvelop.getReqListMsg();
            slave.clientManager.restart();
            int nRequests = reqList.getRequestsCount();
            int nClients = reqList.getNClients();
            log.info("Got " + nRequests + " requests for " + nClients + " clients");

            HttpRequestBase[] history = new HttpRequestBase[nRequests];
            short[] historyCounter = new short[nRequests];
            int i = 0;
            for (AppRequest req : reqList.getRequestsList()) {
                try {
                    history[i] = RequestCreation.convertReqBufferToHTTPRequest(req);
                    log.info(history[i]);
                    historyCounter[i] = (short) req.getNExec();
                    i++;
                } catch (UnsupportedEncodingException e) {
                    log.error("Unsupported Encoding");
                }
            }
            // create the clients
            for (i = 0; i < nClients; i++) {
                short[] counter = Arrays.copyOf(historyCounter, historyCounter.length);
                slave.clientManager.newClient(history, counter, new URL(msgEnvelop.getDestination()));
            }
        }

    }
}
