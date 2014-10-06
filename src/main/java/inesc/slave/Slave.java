package inesc.slave;

import inesc.master.Master;
import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;
import inesc.shared.AppEvaluationProtos.ToMaster;
import inesc.shared.AppEvaluationProtos.ToMaster.SlaveID;
import inesc.slave.clients.ClientConfiguration;
import inesc.slave.clients.ClientManager;
import inesc.slave.clients.Report;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class Slave {
    private static Logger log = Logger.getLogger(Slave.class);

    static final String BASE_DIR = "slave/";
    private final InetSocketAddress masterAddress;
    public InetSocketAddress myAddress;

    /** Minimum port of server */
    private static final int PORT_RANGE_MIN = 9000;
    /** Max port of server */
    private static final int PORT_RANGE_MAX = 9200;
    private SlaveService service;
    public boolean masterIsAvailable = true;

    private ClientManager clientManager;

    public Slave() {
        this(Master.MASTER_ADDRESS);
    }

    public Slave(InetSocketAddress masterAddress) {
        Thread.currentThread().setName("Slave main thread");

        this.masterAddress = masterAddress;
        int port = getFreePort();
        // TODO Get local IP
        String host = "localhost";
        myAddress = new InetSocketAddress(host, port);
        try {
            service = new SlaveService(this);
            service.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientManager = new ClientManager(this);
        if (masterAddress != null) {
            try {
                register();
            } catch (Exception e) {
                System.out.println("Master is not available");
                masterIsAvailable = false;
            }
        }
        log.info("Starting slave at port " + port + "....");
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


    public void start() {
        clientManager.start();
    }


    public Report startSync() {
        Report report = clientManager.runSync();
        service.end();
        return report;
    }

    /**
     * Send the reports to master
     */
    public void sendReportToMaster(Report clientReports) {
        if (masterAddress != null) {
            try {
                Socket s = new Socket(masterAddress.getAddress(), masterAddress.getPort());
                ReportAgregatedMsg.Builder bd = ReportAgregatedMsg.newBuilder();
                bd.addReports(clientReports.toProtBuffer());

                ReportAgregatedMsg msg = bd.build();
                newToMaster().setReportMsg(msg).build().writeDelimitedTo(s.getOutputStream());
                s.close();
            } catch (Exception e) {
                log.error(e);
            }
        }
        clientManager = new ClientManager(this);
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

    public ToMaster.Builder newToMaster() {
        SlaveID.Builder id = SlaveID.newBuilder().setPort(myAddress.getPort()).setHost(myAddress.getHostString());
        return ToMaster.newBuilder().setSlaveId(id);
    }


    public void newFile(List<File> filesToExec, ClientConfiguration conf) {
        clientManager.newFile(filesToExec, conf);
    }

    public void newHistory(HttpRequestBase[] history, long[] counter, ClientConfiguration conf) {
        long[] counterCopy = Arrays.copyOf(counter, counter.length);
        clientManager.newHistory(history, counterCopy, conf);
    }


    public static void main(String[] args) throws IOException {
        DOMConfigurator.configure("log4j.xml");
        if (args.length == 0) {
            new Slave();
        }
        if (args.length == 2) {
            new Slave(new InetSocketAddress(args[1], Integer.valueOf(args[2])));
        }
    }

}
