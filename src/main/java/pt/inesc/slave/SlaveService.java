package pt.inesc.slave;

import inesc.shared.AppEvaluationProtos.AppAck;
import inesc.shared.AppEvaluationProtos.AppAck.ResStatus;
import inesc.shared.AppEvaluationProtos.FileMsg;
import inesc.shared.AppEvaluationProtos.FromMaster;
import inesc.shared.AppEvaluationProtos.HistoryMsg;
import inesc.shared.AppEvaluationProtos.HistoryMsg.AppRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;

import pt.inesc.slave.clients.ClientConfiguration;

public class SlaveService extends
        Thread {
    private static Logger log = Logger.getLogger(SlaveService.class);
    private final Slave slave;
    private final ServerSocket server;
    private static int K = 1024;
    private boolean running = true;

    private final String BASE_DIR = "slave/";

    public SlaveService(Slave slave) throws IOException {
        this.slave = slave;
        server = new ServerSocket(slave.myAddress.getPort());
    }


    @Override
    public void run() {
        while (running) {
            Socket s;
            try {
                s = server.accept();
                process(s);
            } catch (IOException e) {
                if (running) {
                    log.error(e);
                }
            }
        }
    }


    public void end() {
        running = false;
        try {
            server.close();
        } catch (IOException e) {
        }
    }

    /**
     * 3 requests: history, file, start
     * 
     * @param s
     * @throws IOException
     */
    private void process(Socket s) throws IOException {
        FromMaster msgEnvelop = FromMaster.parseDelimitedFrom(s.getInputStream());
        if (msgEnvelop == null) {
            return;
        }

        slave.setMaster(msgEnvelop.getMasterHost());


        if (msgEnvelop.hasFileMsg()) {
            FileMsg m = msgEnvelop.getFileMsg();
            newFileToExec(s,
                          m.getFilenameList(),
                          new ClientConfiguration(m.getConfiguration()),
                          m.getNumberOfLines(),
                          m.getReadPercentage(),
                          m.getPerTopic());
        }



        if (msgEnvelop.hasHistoryMsg()) {
            HistoryMsg m = msgEnvelop.getHistoryMsg();
            List<AppRequest> reqList = m.getRequestsList();
            ClientConfiguration config = new ClientConfiguration(m.getConfiguration());
            int nClients = m.getNClients();
            newExecHistory(reqList, config, nClients);
        }


        if (msgEnvelop.hasStart()) {
            if (msgEnvelop.getStart()) {
                log.info("Master ordered to start...");
                slave.start();
            }
        }
    }

    private void newExecHistory(List<AppRequest> reqList, ClientConfiguration config, int nClients) {
        int nRequests = reqList.size();
        log.info("Got " + nRequests + " requests for " + nClients + " clients");

        HttpRequestBase[] history = new HttpRequestBase[nRequests];
        long[] historyCounter = new long[nRequests];
        int i = 0;
        for (AppRequest req : reqList) {
            try {
                history[i] = RequestCreation.convertReqBufferToHTTPRequest(req);
                log.info(history[i]);
                historyCounter[i] = req.getNExec();
                i++;
            } catch (UnsupportedEncodingException e) {
                log.error("Unsupported Encoding");
            }
        }

        // create the clients
        for (i = 0; i < nClients; i++) {
            slave.newHistory(history, historyCounter, config);
        }
    }

    /**
     * Transfer the file if not exists and add the client
     * 
     * @param s
     * @param list
     * @param clientConfiguration
     * @throws IOException
     */
    private void newFileToExec(
            Socket s,
                List<String> list,
                ClientConfiguration clientConfiguration,
                int numberOfLines,
                double readPercentage,
                boolean perTopic) throws IOException {
        log.info("new file to exec " + list);
        File dir = new File(BASE_DIR);
        List<File> listOfFiles = new ArrayList<File>(list.size());

        // Transfer the files
        for (String filename : list) {
            File f = new File(dir, filename);
            if (f.exists()) {
                log.info("file exists");
                AppAck msg = AppAck.newBuilder().setStatus(ResStatus.OK).setText(filename).build();
                slave.newToMaster().setAckMsg(msg).build().writeDelimitedTo(s.getOutputStream());
            } else {
                // needs the file, ask for transfer
                AppAck msg = AppAck.newBuilder().setStatus(ResStatus.ERROR).setText(filename).build();
                slave.newToMaster().setAckMsg(msg).build().writeDelimitedTo(s.getOutputStream());
                transferFile(f, s);
                log.info("File transfered with success");
            }
            listOfFiles.add(f);
        }

        slave.newFile(listOfFiles, clientConfiguration, numberOfLines, readPercentage, perTopic);
    }




    private void transferFile(File f, Socket s) {
        FileWriter fw = null;
        log.info("transfering file...");
        try {
            File dir = new File(BASE_DIR);
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
