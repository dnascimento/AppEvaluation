package inesc.slave;

import inesc.slave.clients.ClientConfiguration;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

public class SlaveMain {

    private static final int THROUGHPUT = 5000;
    private static final boolean ASSYNC = false;
    private static final boolean TO_DISK = false;
    private static final boolean MEASURE = false;
    static Slave sl;
    static ClientConfiguration config;

    public static void main(String[] args) {
        sl = new Slave();

        HttpHost target = new HttpHost("localhost", 8080);
        config = new ClientConfiguration(target, THROUGHPUT, ASSYNC, TO_DISK, MEASURE);

        singleRequest();

        // File dir = new File(Slave.BASE_DIR);
        // PatternFilenameFilter p = new PatternFilenameFilter(args[0]);
        // File[] files = dir.listFiles();
        // Arrays.sort(files, new SortFilesByNumber());
        //
        // for (File f : files) {
        // if (p.accept(dir, f.getName())) {
        // sl.newFile(f, config);
        // }
        // }
        // sl.startSync();
        // return;
    }


    private static void singleRequest() {
        HttpRequestBase[] reqs = new HttpRequestBase[] { new HttpGet("/test") };
        long[] counter = new long[] { 60000 };
        for (int i = 0; i < 20; i++) {
            sl.newHistory(reqs, counter, config);
        }
        sl.startSync();
    }
}
