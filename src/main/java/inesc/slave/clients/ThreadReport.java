/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.slave.clients;


import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg.ThreadReportMsg;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Thread execution statistics
 * 
 * @author darionascimento
 */
public class ThreadReport {
    public long totalExecutionTime;
    public int clientId, totalResponseLatency;
    public double averageResponseTime, longest, shortest, percentil90, percentil95;

    public long nTransactions, successTransactions, failTransactions;
    public double transactionRate;
    private long dataReceived;
    private int exceptionResponse;

    private static final int TO_MILISECOND = 1000000;

    public ThreadReport() {

    }

    /**
     * create a report processing the collected statistics
     * 
     * @param clientId
     */
    public ThreadReport(int clientId, Statistics stats) {
        totalExecutionTime = (System.nanoTime() - stats.startExecution) / TO_MILISECOND;
        nTransactions = stats.executionTimes.size();
        ArrayList<Integer> executionTimes = stats.executionTimes;
        longest = 0;
        shortest = Double.MAX_VALUE;

        for (int i = 0; i < nTransactions; i++) {
            // failed requests time counts too
            double execTime = Math.abs(executionTimes.get(i) / TO_MILISECOND);

            totalResponseLatency += execTime;

            if (executionTimes.get(i) < 0) {
                failTransactions++;
                continue;
            }
            if (longest < execTime) {
                longest = execTime;
            }
            if (shortest > execTime) {
                shortest = execTime;
            }
        }

        successTransactions = nTransactions - failTransactions;
        if (successTransactions > 0)
            averageResponseTime = (totalResponseLatency / successTransactions);
        else
            averageResponseTime = 0;


        transactionRate = (((double) nTransactions) / (totalExecutionTime) * 1000);
        this.dataReceived = stats.dataReceived;
        this.exceptionResponse = stats.exceptionResponse;


        // set values for absolute
        for (int i = 0; i < executionTimes.size(); i++) {
            executionTimes.set(i, Math.abs(executionTimes.get(i)));
        }

        // calculate 90th and 95th percentil
        Collections.sort(executionTimes);
        this.percentil95 = calculatePercentil(executionTimes, 95) / TO_MILISECOND;
        this.percentil90 = calculatePercentil(executionTimes, 90) / TO_MILISECOND;
    }

    private double calculatePercentil(ArrayList<Integer> array, int percentil) {
        int n = array.size();
        double p = percentil / 100;
        int k = (int) ((n - 1) * p);
        double d = ((n - 1) * p);

        return array.get(k + 1) + d * (array.get(k + 2) - array.get(k + 1));
    }

    public static ThreadReport fromProtBuffer(ThreadReportMsg msg) {
        ThreadReport report = new ThreadReport();
        report.averageResponseTime = msg.getAverageResponseTime();
        report.dataReceived = msg.getDataReceived();
        report.failTransactions = msg.getFailTransactions();
        report.longest = (short) msg.getLongest();
        report.nTransactions = msg.getNTransactions();
        report.shortest = (short) msg.getShortest();
        report.successTransactions = msg.getSuccessTransactions();
        report.totalExecutionTime = msg.getTotalExecutionTime();
        report.totalResponseLatency = msg.getTotalTransferingTime();
        report.transactionRate = msg.getTransactionRate();
        report.exceptionResponse = msg.getExceptionResponse();
        report.percentil90 = msg.getPercentil90();
        report.percentil95 = msg.getPercentil95();
        return report;
    }

    public ThreadReportMsg toProtBuffer() {
        return ThreadReportMsg.newBuilder()
                              .setAverageResponseTime(averageResponseTime)
                              .setDataReceived(dataReceived)
                              .setFailTransactions(failTransactions)
                              .setLongest(longest)
                              .setNTransactions(nTransactions)
                              .setShortest(shortest)
                              .setExceptionResponse(exceptionResponse)
                              .setSuccessTransactions(successTransactions)
                              .setTotalExecutionTime(totalExecutionTime)
                              .setTotalTransferingTime(totalResponseLatency)
                              .setTransactionRate(transactionRate)
                              .setPercentil90(percentil90)
                              .setPercentil95(percentil95)
                              .build();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n Transactions: " + nTransactions + "\n");
        sb.append("Success: " + successTransactions + "\n");
        sb.append("Fail: " + failTransactions + "\n");
        sb.append("Success Rate: " + ((double) successTransactions / nTransactions) * 100 + "% \n");
        sb.append("Wront transactins (Exception): " + exceptionResponse + "\n\n");
        sb.append("Transaction Rate: " + String.format("%.2f", transactionRate) + " req/sec \n\n");
        sb.append("Total time: \n");
        sb.append("Transfering: " + totalResponseLatency + "ms \n");
        sb.append("Execution: " + totalExecutionTime + "ms \n");

        sb.append("\n");
        sb.append("Response latency:\n");
        sb.append("- Average: " + averageResponseTime + "ms \n");
        sb.append("- Longest " + longest + "ms \n");
        sb.append("- Shortest " + shortest + "ms \n");
        sb.append("- 95th " + percentil95 + " ms \n");
        sb.append("- 90th " + percentil90 + " ms \n");
        sb.append("\n");
        sb.append("Received: " + dataReceived + " bytes");

        return sb.toString();
    }

}
