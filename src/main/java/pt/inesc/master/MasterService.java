package pt.inesc.master;

import inesc.shared.AppEvaluationProtos.ToMaster;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

import pt.inesc.slave.clients.Report;

public class MasterService extends
        Thread {

    public Master master;
    private static Logger log = Logger.getLogger(MasterService.class);
    boolean running = true;
    ServerSocket server;

    public MasterService(Master master) throws IOException {
        this.master = master;
        server = new ServerSocket(Master.MASTER_PORT);
    }

    public void end() {
        running = false;
        try {
            server.close();
        } catch (IOException e) {
        }
    }

    @Override
    public void run() {
        while (running) {
            Socket s;
            try {
                s = server.accept();
                process(s);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private void process(Socket s) throws Exception {
        ToMaster msgEnvelop = ToMaster.parseDelimitedFrom(s.getInputStream());
        if (msgEnvelop == null) {
            return;
        }

        if (msgEnvelop.hasTransferFile()) {
            String file = msgEnvelop.getTransferFile();
            master.sendFile(file, s);
            return;
        }

        if (msgEnvelop.hasReportMsg()) {
            master.addReport(Report.fromProtBuffer(msgEnvelop.getReportMsg()), msgEnvelop.getSlaveHost());
        }

    }
}
