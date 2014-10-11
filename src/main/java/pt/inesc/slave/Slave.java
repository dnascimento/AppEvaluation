package pt.inesc.slave;

import inesc.shared.AppEvaluationProtos.ToMaster;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import pt.inesc.master.Master;
import pt.inesc.slave.clients.ClientConfiguration;
import pt.inesc.slave.clients.ClientManager;
import pt.inesc.slave.clients.Report;

public class Slave {
    private static Logger log = Logger.getLogger(Slave.class);


    InetSocketAddress masterAddress = null;
    public InetSocketAddress myAddress;

    /** Slave port */
    public static final int SLAVE_PORT = 9200;

    private SlaveService service;
    private ClientManager clientManager;


    public Slave(String hostName) {
        Thread.currentThread().setName("Slave main thread");

        myAddress = new InetSocketAddress(hostName, SLAVE_PORT);
        try {
            service = new SlaveService(this);
            service.start();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientManager = new ClientManager(this);
        log.info("Starting slave...");
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
        System.out.println(clientReports);
        if (masterAddress != null) {
            try {
                Socket s = new Socket(masterAddress.getAddress(), masterAddress.getPort());
                newToMaster().setReportMsg(clientReports.toProtBuffer()).build().writeDelimitedTo(s.getOutputStream());
                s.close();
            } catch (Exception e) {
                log.error(e);
            }
        }
        clientManager = new ClientManager(this);
    }



    public ToMaster.Builder newToMaster() {
        return ToMaster.newBuilder().setSlaveHost(myAddress.getHostString());
    }


    public void newFile(
            List<File> filesToExec,
                ClientConfiguration conf,
                Integer numberOfLines,
                double readPercentage,
                boolean perTopic,
                boolean parallel) {
        if (parallel) {
            for (File f : filesToExec) {
                clientManager.newFile(Arrays.asList(f), conf, numberOfLines, readPercentage, perTopic);
            }
        } else {
            clientManager.newFile(filesToExec, conf, numberOfLines, readPercentage, perTopic);
        }
    }

    public void newHistory(HttpRequestBase[] history, long[] counter, ClientConfiguration conf) {
        long[] counterCopy = Arrays.copyOf(counter, counter.length);
        clientManager.newHistory(history, counterCopy, conf);
    }


    /**
     * Start slave with specific master
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws Exception {
        DOMConfigurator.configure("log4j.xml");
        InetAddress addr = InetAddress.getLocalHost();
        System.out.println("Start slave on host: " + addr);
        new Slave(Master.getAddress());
    }

    public void setMaster(String masterHost) {
        masterAddress = new InetSocketAddress(masterHost, Master.MASTER_PORT);
    }

    public void stop() {
        service.end();
    }
}
