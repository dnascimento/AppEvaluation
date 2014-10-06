package inesc.slave;

import inesc.master.Master;
import inesc.slave.clients.ClientConfiguration;
import inesc.slave.clients.Report;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import com.google.common.io.PatternFilenameFilter;

public class SlaveMain {

    private static final int THROUGHPUT = -1;
    private static final boolean ASSYNC = false;
    private static final boolean TO_DISK = false;
    private static final boolean MEASURE = false;
    static Slave sl;
    static ClientConfiguration config;

    public static void main(String[] args) {
        sl = new Slave();

        System.out.println("Usage: <target_host>:<target_port>  \n "
                + " -t  throughput: fixed; [start:end:step]; default: maximum per client \n  "
                + "-f <filePattern> in serie \n  -p <filePattern> files are executed in parallel \n"
                + "-s  sync: default: assync if throughput is defined, sync if maximum throughput"
                + "-g <url> <times>  get the url x times \n" + "-m <master_host> \n" + "-c <number of concurrent clients>"
                + "-l log to disk \n " + "-d measure data received\n");


        HttpHost target = new HttpHost(args[0].split(":")[0], Integer.parseInt(args[0].split(":")[1]));
        boolean assync = ASSYNC;
        boolean logToDisk = TO_DISK;
        boolean measureDataReceived = MEASURE;
        boolean fileParallel = false;

        String filePattern = null;
        String url = null;
        Integer times = null;
        InetSocketAddress masterAddress = Master.MASTER_ADDRESS;
        int[] stepedThroughput = new int[] { THROUGHPUT, THROUGHPUT, 0 };
        int clients = 1;

        int i = 1;
        while (i < args.length) {
            char op = args[i++].toCharArray()[1];
            switch (op) {
            case 't':
                // throughput
                Matcher matcher = Pattern.compile("\\d+").matcher(args[i++]);
                int k = 0;
                while (matcher.find()) {
                    stepedThroughput[k++] = Integer.valueOf(matcher.group(0));
                }
                break;
            case 'c':
                clients = Integer.valueOf(args[i++]);
                break;
            case 'f':
                filePattern = args[i++];
                break;
            case 'p':
                filePattern = args[i++];
                fileParallel = true;
                break;
            case 's':
                assync = false;
                break;
            case 'g':
                url = args[i++];
                times = Integer.valueOf(args[i++]);
                break;
            case 'm':
                masterAddress = new InetSocketAddress(args[i++], Master.MASTER_ADDRESS.getPort());
                break;
            default:
                break;
            }
        }


        if (filePattern == null && url == null) {
            throw new RuntimeException("You must specify a file or url");
        }

        System.out.println("Rate     avg     90th percentil   95th percentil");
        for (int throughput = stepedThroughput[0]; throughput <= stepedThroughput[1]; throughput += stepedThroughput[2]) {
            ClientConfiguration config = new ClientConfiguration(target, throughput, assync, logToDisk, measureDataReceived);
            Report report = null;

            if (filePattern != null) {
                report = execFile(filePattern, clients, fileParallel);
            }

            if (url != null) {
                report = execUrl(url, times, clients);
            }

            System.out.println(report.transactionRate + "   " + report.averageResponseTime + "   " + report.percentil90 + "    "
                    + report.percentil95);
        }
    }

    private static Report execFile(String filePattern, int clients, boolean fileParallel) {
        File dir = new File(Slave.BASE_DIR);
        PatternFilenameFilter p = new PatternFilenameFilter(filePattern);
        File[] files = dir.listFiles();
        Arrays.sort(files, new SortFilesByNumber());

        List<File> filesToExec = new ArrayList<File>();
        for (File f : files) {
            if (p.accept(dir, f.getName())) {
                filesToExec.add(f);
            }
        }

        if (fileParallel) {
            for (File f : filesToExec) {
                for (int i = 0; i < clients; i++) {
                    sl.newFile(Arrays.asList(f), config);
                }
            }
        } else {
            for (int i = 0; i < clients; i++) {
                sl.newFile(filesToExec, config);
            }
        }
        return sl.startSync();
    }



    private static Report execUrl(String url, int times, int clients) {
        HttpRequestBase[] reqs = new HttpRequestBase[] { new HttpGet(url) };
        long[] counter = new long[] { times };
        for (int i = 0; i < clients; i++) {
            sl.newHistory(reqs, counter, config);
        }
        return sl.startSync();
    }
}
