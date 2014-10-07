package pt.inesc.slave.clients;

import inesc.shared.AppEvaluationProtos.Configuration;
import inesc.shared.AppEvaluationProtos.Configuration.Builder;

import org.apache.http.HttpHost;

public class ClientConfiguration {

    public static final Integer ALL_LINES = -1;
    public static final double NO_READS = 0;

    final HttpHost target;
    final int throughput;
    final boolean asynchronous;
    final boolean logToDisk;

    public ClientConfiguration(Configuration configuration) {
        String addr = configuration.getTarget();
        this.target = new HttpHost(addr.split(":")[0], Integer.valueOf(addr.split(":")[1]));
        this.throughput = configuration.getThroughput();
        this.asynchronous = configuration.getAssynchronous();
        this.logToDisk = configuration.getLogToDisk();
    }


    public ClientConfiguration(HttpHost target, int throughput, boolean asynchronous, boolean logToDisk) {
        super();
        this.target = target;
        this.throughput = throughput;
        this.asynchronous = asynchronous;
        this.logToDisk = logToDisk;
    }


    public Configuration toProtoBuf() {
        Builder c = Configuration.newBuilder();
        return c.setTarget(target.toHostString()).setThroughput(throughput).setAssynchronous(asynchronous).setLogToDisk(logToDisk).build();
    }
}
