package inesc.master;

import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;
import inesc.shared.AppEvaluationProtos.ToMaster;
import inesc.shared.AppEvaluationProtos.ToMaster.SlaveID;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

public class MasterService extends
        Thread {

    public Master master;
    private static Logger log = Logger.getLogger(MasterService.class);

    ServerSocket server;

    public MasterService(Master master) throws IOException {
        this.master = master;
        server = new ServerSocket(Master.MASTER_ADDRESS.getPort());
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
        ToMaster msgEnvelop = ToMaster.parseDelimitedFrom(s.getInputStream());
        if (msgEnvelop == null) {
            return;
        }

        SlaveID msg = msgEnvelop.getSlaveId();
        InetSocketAddress address = new InetSocketAddress(msg.getHost(), msg.getPort());

        if (msgEnvelop.hasRegistry()) {
            master.addNewSlave(address);
        }

        if (msgEnvelop.hasReportMsg()) {
            ReportAgregatedMsg msg1 = msgEnvelop.getReportMsg();
            master.addReport(msg1);
        }

    }
}
