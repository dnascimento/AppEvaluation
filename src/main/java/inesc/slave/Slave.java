package inesc.slave;

import inesc.master.Master;
import inesc.shared.AppEvaluationProtos.AppAck;
import inesc.shared.AppEvaluationProtos.AppAck.ResStatus;
import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;
import inesc.shared.AppEvaluationProtos.SlaveID;
import inesc.shared.AppEvaluationProtos.ToMaster;
import inesc.slave.clients.ClientManager;
import inesc.slave.clients.ThreadReport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class Slave {
    private static Logger log = Logger.getLogger(Slave.class);

    private final String BASE_DIR = "slave/";
    private final InetSocketAddress masterAddress;
    public final InetSocketAddress myAddress;

    /** Minimum port of server */
    private final static int PORT_RANGE_MIN = 9000;
    /** Max port of server */
    private final static int PORT_RANGE_MAX = 9200;
    private static int K = 1024;


    public ClientManager clientManager;

    Slave() throws UnknownHostException, IOException {
        masterAddress = Master.MASTER_ADDRESS;
        int port = getFreePort();
        // TODO Get local IP
        String host = "localhost";
        myAddress = new InetSocketAddress(host, port);
        new SlaveService(this).start();
        clientManager = new ClientManager(this);
        register();
        log.info("Starting slave at port" + port + "....");
    }


    public static void main(String[] args) throws IOException {
        DOMConfigurator.configure("log4j.xml");
        new Slave();
    }



    /**
     * Register slave on master
     * 
     * @param url
     * @param port
     * @throws IOException
     * @throws UnknownHostException
     */
    private void register() throws UnknownHostException, IOException {
        Socket s = new Socket(masterAddress.getAddress(), masterAddress.getPort());
        ToMaster msg = newToMaster().setRegistry(true).build();
        msg.writeDelimitedTo(s.getOutputStream());
        s.close();
    }




    /**
     * Send the reports to master
     */
    public void sendReportToMaster(ThreadReport[] clientReports) {
        try {
            Socket s = new Socket(masterAddress.getAddress(), masterAddress.getPort());
            ReportAgregatedMsg.Builder bd = ReportAgregatedMsg.newBuilder();
            for (int i = 0; i < clientReports.length; i++) {
                bd.addReports(clientReports[i].toProtBuffer());
            }
            ReportAgregatedMsg msg = bd.build();
            newToMaster().setReportMsg(msg).build().writeDelimitedTo(s.getOutputStream());
            s.close();
        } catch (Exception e) {
            log.error(e);
        }
        showReports(clientReports);
        clientManager = new ClientManager(this);
    }

    /**
     * List all reports
     */
    private void showReports(ThreadReport[] clientReports) {
        for (int i = 0; i < clientReports.length; i++) {
            log.info(clientReports[i]);
        }
    }


    /**
     * Select a random free port on given range
     * 
     * @return the port
     */
    public static int getFreePort() {
        Random rand = new Random();
        int port;
        while (true) {
            port = (rand.nextInt(PORT_RANGE_MAX - PORT_RANGE_MIN)) + PORT_RANGE_MIN;
            // Test if port is free
            try {
                ServerSocket sock = new ServerSocket(port);
                sock.close();
                return port;
            } catch (Exception e) {
                // IGNORE
            }
        }
    }

    private ToMaster.Builder newToMaster() {
        SlaveID.Builder id = SlaveID.newBuilder().setPort(myAddress.getPort()).setHost(myAddress.getHostString());
        return ToMaster.newBuilder().setSlaveId(id);
    }




    public void newFileToExec(String filename, String targetHost, Socket s) throws IOException {
        log.info("new file to exec " + filename);
        File dir = new File(BASE_DIR);
        File f = new File(dir, filename);
        if (f.exists()) {
            log.info("file exists");
            AppAck msg = AppAck.newBuilder().setStatus(ResStatus.OK).build();
            newToMaster().setAckMsg(msg).build().writeDelimitedTo(s.getOutputStream());
        } else {
            // needs the file, ask for transfer
            AppAck msg = AppAck.newBuilder().setStatus(ResStatus.ERROR).build();
            newToMaster().setAckMsg(msg).build().writeDelimitedTo(s.getOutputStream());
            transferFile(f, s);
            log.info("File transfered with success");
        }
        clientManager.newFile(f, targetHost);
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
