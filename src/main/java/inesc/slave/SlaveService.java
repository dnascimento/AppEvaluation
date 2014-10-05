package inesc.slave;

import inesc.shared.AppEvaluationProtos.AppAck;
import inesc.shared.AppEvaluationProtos.AppAck.ResStatus;
import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.shared.AppEvaluationProtos.AppStartMsg;
import inesc.shared.AppEvaluationProtos.FromMaster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private static int K = 1024;

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
            newFileToExec(msgEnvelop.getFilename(), new URL(msgEnvelop.getDestination()), s, msgEnvelop.getThroughput());
        }

        if (msgEnvelop.hasStartMsg()) {
            log.info("Master ordered to start...");
            AppStartMsg m2 = msgEnvelop.getStartMsg();
            slave.clientManager.setStartOptions(m2.getOptList());
            slave.start();
        }

        if (msgEnvelop.hasReqListMsg()) {
            AppReqList reqList = msgEnvelop.getReqListMsg();

            slave.clientManager.restart();
            int nRequests = reqList.getRequestsCount();
            int nClients = reqList.getNClients();
            int throughput = msgEnvelop.getThroughput();
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
                slave.newExecutionList(history, counter, new URL(msgEnvelop.getDestination()), throughput);
            }
        }
    }

    private void newFileToExec(String filename, URL targetHost, Socket s, int throughput) throws IOException {
        log.info("new file to exec " + filename);
        File dir = new File(Slave.BASE_DIR);
        File f = new File(dir, filename);
        if (f.exists()) {
            log.info("file exists");
            AppAck msg = AppAck.newBuilder().setStatus(ResStatus.OK).build();
            slave.newToMaster().setAckMsg(msg).build().writeDelimitedTo(s.getOutputStream());
        } else {
            // needs the file, ask for transfer
            AppAck msg = AppAck.newBuilder().setStatus(ResStatus.ERROR).build();
            slave.newToMaster().setAckMsg(msg).build().writeDelimitedTo(s.getOutputStream());
            transferFile(f, s);
            log.info("File transfered with success");
        }
        slave.newFileToExec(f, targetHost, throughput);
    }


    private void transferFile(File f, Socket s) {
        FileWriter fw = null;
        log.info("transfering file...");
        try {
            File dir = new File(Slave.BASE_DIR);
            dir.mkdirs();
            f.createNewFile();
            fw = new FileWriter(f);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            int readed;
            char[] buffer = new char[1024 * K];
            while ((readed = in.read(buffer)) > 0) {
                fw.write(buffer, 0, readed);
            }
            fw.flush();
            log.info("transfer completed");
        } catch (Exception e) {
            log.error(e);
        } finally {
            try {
                fw.close();
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

}
