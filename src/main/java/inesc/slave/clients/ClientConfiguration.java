package inesc.slave.clients;

import inesc.shared.AppEvaluationProtos.Configuration;

import org.apache.http.HttpHost;

public class ClientConfiguration {
    final HttpHost target;
    final int throughput;
    final boolean asynchronous;
    final boolean logToDisk;
    final boolean measureDataReceived;

    public ClientConfiguration(Configuration configuration) {
        this.target = new HttpHost(configuration.getTarget());
        this.throughput = configuration.getThroughput();
        this.asynchronous = configuration.getAssynchronous();
        this.logToDisk = configuration.getLogToDisk();
        this.measureDataReceived = configuration.getMeasureDataReceived();
    }


    public ClientConfiguration(HttpHost target, int throughput, boolean asynchronous, boolean logToDisk, boolean measureDataReceived) {
        super();
        this.target = target;
        this.throughput = throughput;
        this.asynchronous = asynchronous;
        this.logToDisk = logToDisk;
        this.measureDataReceived = measureDataReceived;
    }


}
