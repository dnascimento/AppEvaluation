package pt.inesc.master;

import inesc.shared.AppEvaluationProtos.HistoryMsg.AppRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;

import pt.inesc.master.commands.AskIntBuffers;
import pt.inesc.slave.Slave;
import pt.inesc.slave.SortFilesByNumber;
import pt.inesc.slave.clients.ClientConfiguration;
import pt.inesc.slave.clients.Report;

import com.google.common.io.PatternFilenameFilter;

public class EvalMain {

    private final static String BASE_DIR = "master/";
    private static final int THROUGHPUT = -1;
    private static final boolean ASSYNC = false;
    private static final boolean TO_DISK = false;
    // how many reads over the total number of actions
    static Master master;

    public static void main(String[] args) throws Exception {
        boolean assync = ASSYNC;
        boolean logToDisk = TO_DISK;
        boolean fileParallel = false;
        double readPercentage = ClientConfiguration.NO_READS;
        HttpHost target = null;
        String filePattern = null;
        String url = null;
        Integer times = null;
        int[] stepedThroughput = new int[] { THROUGHPUT, THROUGHPUT, 0 };
        int clients = 1;
        Integer numberOfLines = ClientConfiguration.ALL_LINES;
        String dir = BASE_DIR;
        boolean perTopic = false;
        int i = 1;
        List<String> slaves = new ArrayList<String>();
        slaves.add("localhost");
        Slave localSlave;

        try {
            target = new HttpHost(args[0].split(":")[0], Integer.parseInt(args[0].split(":")[1]));
            while (i < args.length) {
                String op = args[i++];
                if (!(op.startsWith("--") || op.startsWith("-"))) {
                    throw new Exception("Wrong argument parsed: " + op);
                }
                op = op.replaceAll("-", "");

                switch (op) {
                case "t":
                case "throughput":
                    // throughput
                    System.out.println("Throughput " + args[i]);
                    Matcher matcher = Pattern.compile("\\d+").matcher(args[i++]);
                    int k = 0;
                    while (matcher.find()) {
                        stepedThroughput[k++] = Integer.valueOf(matcher.group(0));
                    }
                    break;
                case "directory":
                    System.out.println("Directory " + args[i]);
                    dir = args[i];
                case "assync":
                    System.out.println("Assynchrounous");
                    if (stepedThroughput[0] == -1) {
                        throw new Exception("To use assynchronous, define the throughput first");
                    }
                    assync = true;
                    break;
                case "readPercentage":
                    System.out.println("Read percentage " + args[i]);
                    readPercentage = Double.valueOf(args[i++]);
                    break;
                case "lines":
                    System.out.println("Number of lines " + args[i]);
                    numberOfLines = Integer.valueOf(args[i++]);
                    ;
                case "logDisk":
                    System.out.println("Log to disk");
                    logToDisk = true;
                    break;
                case "c":
                case "clients":
                    System.out.println("Number of clients " + args[i]);
                    clients = Integer.valueOf(args[i++]);
                    break;
                case "file":
                    System.out.println("Files " + args[i]);
                    filePattern = args[i++];
                    break;
                case "fileParallel":
                    System.out.println("Files in parallel " + args[i]);
                    filePattern = args[i++];
                    fileParallel = true;
                    break;
                case "get":
                    url = args[i++];
                    times = Integer.valueOf(args[i++]);
                    break;
                case "perTopic":
                    perTopic = true;
                    break;
                case "slaves":
                    slaves.addAll(Arrays.asList(args[i++].split(",")));
                    System.out.println("Slaves: " + slaves);
                    break;
                default:
                    throw new Exception("Unknown option: " + op);
                }
            }

            master = new Master(slaves);
            localSlave = new Slave("localhost");


            if (filePattern == null && url == null) {
                throw new RuntimeException("You must specify a file or url");
            }


        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.out.println("Usage: <target_host>:<target_port>  \n "
                    + " --throughput  throughput per client: fixed; [start:end:step]; default: maximum  \n  "
                    + " --file <filePattern> in serie \n" + "   --fileParallel <filePattern> files are executed in parallel \n"
                    + " --lines <numberOfLines> \n" + " --readPercentage <percentage of read operation: default: 0> \n"
                    + " --assync  assync: default is sync \n" + " --get <url> <times>  get the url x times \n"
                    + " --clients <number of concurrent clients> \n" + " --logDisk log to disk \n " + "  --directory directory \n"
                    + "  --perTopic \n");
            return;
        }



        System.out.println("Total         Rate       avg          95th percentil   90th percentil     Data Received (Bytes)");
        int throughput;
        do {
            throughput = stepedThroughput[0];

            ClientConfiguration config = new ClientConfiguration(target, throughput, assync, logToDisk);
            LinkedList<Report> reports = null;

            if (filePattern != null) {
                reports = execFile(filePattern, clients, fileParallel, numberOfLines, config, readPercentage, dir, perTopic);
            }

            if (url != null) {
                reports = getUrl(target.getHostName() + ":" + target.getPort(), url, times, clients, config);
            }

            for (Report report : reports) {
                String summ = report.nTransactions + " " + report.transactionRate + " " + report.averageResponseTime + " "
                        + report.percentil95 + " " + report.percentil95 + " " + report.dataReceived;
                System.out.println(summ.replace(',', '.'));
            }

            if (stepedThroughput[2] == 0) {
                break;
            }

            throughput += stepedThroughput[2];
        } while (stepedThroughput[2] != 0 || throughput <= stepedThroughput[1]);


        master.stop();
        localSlave.stop();
        System.out.println("TERMINATED");
    }

    private static LinkedList<Report> execFile(
            String filePattern,
                int clients,
                boolean fileParallel,
                Integer numberOfLines,
                ClientConfiguration config,
                double readPercentage,
                String directory,
                boolean perTopic) throws Exception {
        File dir = new File(directory);
        PatternFilenameFilter p = new PatternFilenameFilter(filePattern);
        File[] files = dir.listFiles();
        Arrays.sort(files, new SortFilesByNumber());

        List<File> filesToExec = new ArrayList<File>();
        for (File f : files) {
            if (p.accept(dir, f.getName())) {
                filesToExec.add(f);
            }
        }

        if (filesToExec.isEmpty()) {
            throw new Exception("No files to exec");
        }

        master.newFile(filesToExec, config, numberOfLines, readPercentage, perTopic, fileParallel);


        return master.startExec();
    }

    private static LinkedList<Report> getUrl(String target, String url, int times, int clients, ClientConfiguration config) throws Exception {
        List<AppRequest> requests = new LinkedList<AppRequest>();
        AskIntBuffers bufferCreator = new AskIntBuffers();

        AppRequest.Builder req = bufferCreator.newGet(url);
        req.setNExec(times);
        requests.add(req.build());

        master.newRequests(requests, config, clients);

        return master.startExec();
    }
}
