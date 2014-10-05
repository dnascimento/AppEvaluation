package inesc.slave;

import inesc.master.Master;
import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;
import inesc.shared.AppEvaluationProtos.SlaveID;
import inesc.shared.AppEvaluationProtos.ToMaster;
import inesc.slave.clients.ClientManager;
import inesc.slave.clients.ThreadReport;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.google.common.io.PatternFilenameFilter;

public class Slave {
    private static Logger log = Logger.getLogger(Slave.class);

    static final String BASE_DIR = "slave/";
    private InetSocketAddress masterAddress;
    public InetSocketAddress myAddress;

    /** Minimum port of server */
    private static final int PORT_RANGE_MIN = 9000;
    /** Max port of server */
    private static final int PORT_RANGE_MAX = 9200;


    public ClientManager clientManager;

    public Slave() {
        masterAddress = Master.MASTER_ADDRESS;
        int port = getFreePort();
        // TODO Get local IP
        String host = "localhost";
        myAddress = new InetSocketAddress(host, port);
        try {
            new SlaveService(this).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientManager = new ClientManager(this);
        try {
            register();
        } catch (Exception e) {
            System.out.println("Master not available");
            e.printStackTrace();
        }
        log.info("Starting slave at port " + port + "....");
    }

    public Slave(String fileToExec, URL targetHost, int throughput) {
        clientManager = new ClientManager(this);
        File dir = new File(BASE_DIR);
        PatternFilenameFilter p = new PatternFilenameFilter(fileToExec);
        File[] files = dir.listFiles();
        Arrays.sort(files, new SortFilesByNumber());
        for (File f : files) {
            if (p.accept(dir, f.getName())) {
                clientManager.newFile(f, targetHost, throughput);
                clientManager.runSync();
            }
        }
        ThreadReport[] reports = clientManager.getReports();
        if (reports == null) {
            System.out.println("No reports");
        } else {
            for (ThreadReport report : reports) {
                System.out.println(report);
            }
        }
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

    /**
     * Send the reports to master
     */
    public void sendReportToMaster(ThreadReport[] clientReports) {
        if (masterAddress != null) {
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

    public ToMaster.Builder newToMaster() {
        SlaveID.Builder id = SlaveID.newBuilder().setPort(myAddress.getPort()).setHost(myAddress.getHostString());
        return ToMaster.newBuilder().setSlaveId(id);
    }


    public void newFileToExec(File f, URL targetHost, int throughput) throws IOException {
        clientManager.newFile(f, targetHost, throughput);
    }

    public void newExecutionList(HttpRequestBase[] history, short[] counter, URL url, int throughput) {
        clientManager.newClient(history, counter, url, throughput);
    }


    public static void main(String[] args) throws IOException {
        DOMConfigurator.configure("log4j.xml");
        if (args.length == 0) {
            new Slave();
            return;
        }


        HttpRequestBase[] reqs = new HttpRequestBase[] { new HttpGet("/") };
        short[] counter = new short[] { 2000 };
        URL url = new URL("http://localhost:8080");
        Slave sl = new Slave();
        sl.newExecutionList(reqs, counter, url, -1);
        sl.clientManager.runSync();
        return;
        //
        // String fileToExec;
        // URL target;
        //
        // if (args.length == 3) {
        // fileToExec = args[0];
        // target = new URL(args[1]);
        // int throughput = Integer.valueOf(args[2]);
        // new Slave(fileToExec, target, throughput);
        // return;
        // }
        //
        // if (args.length == 2) {
        // Slave slave = new Slave();
        // File dir = new File("/Users/darionascimento/git/AppEvaluation/slave/");
        // target = new URL(args[0]);
        // int throughput = Integer.valueOf(args[1]);
        // for (File f : dir.listFiles()) {
        // if (f.getName().startsWith(".")) {
        // continue;
        // }
        // slave.newFileToExec(f, target, throughput);
        // }
        // slave.clientManager.runSync();
        // return;
        // }
        //
        //
        // target = new URL(args[0]);
        // @SuppressWarnings("resource")
        // Scanner s = new Scanner(System.in);
        // while (true) {
        // System.out.println("Enter the file name: ");
        // fileToExec = s.nextLine();
        // new Slave(fileToExec, target, -1);
        // }
    }
}
