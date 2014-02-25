package inesc.slave.reports;

import inesc.shared.AppEvaluationProtos.ThreadReportMsg;

/**
 * Thread execution statistics
 * 
 * @author darionascimento
 */
public class ThreadReport {
    public int nTransactions;
    public int successTransactions;
    public int averageResponseTime;
    public double transactionRate;
    public short longest;
    public short shortest;
    public int totalTransferingTime;
    public int failTransactions;
    public String report;
    public long totalExecutionTime;
    public long dataReceived;
    public int clientId;

    public ThreadReport(short[] historyCounter, int clientId) {
        nTransactions = 0;
        for (int i = 0; i < historyCounter.length; i++) {
            nTransactions += historyCounter[i];
        }
        this.clientId = clientId;
    }


    public ThreadReport() {
        // Empty constructor for casts
    }


    public void afterExecution(
            short[] executionTimes,
                long totalExecutionTime,
                String reportString,
                long dataReceived) {
        this.report = reportString;
        this.dataReceived = dataReceived;
        this.totalExecutionTime = totalExecutionTime;
        longest = executionTimes[0];
        shortest = executionTimes[0];
        totalTransferingTime = 0;
        failTransactions = 0;

        for (int i = 0; i < nTransactions; i++) {
            if (executionTimes[i] < 0) {
                failTransactions++;
                continue;
            }
            if (longest < executionTimes[i]) {
                longest = executionTimes[i];
            }
            if (shortest > executionTimes[i]) {
                shortest = executionTimes[i];
            }
            totalTransferingTime += executionTimes[i];
        }
        successTransactions = nTransactions - failTransactions;
        averageResponseTime = totalTransferingTime / successTransactions;
        transactionRate = ((double) nTransactions) / (totalExecutionTime / 1000);
    }


    public static ThreadReport fromProtBuffer(ThreadReportMsg msg) {
        ThreadReport report = new ThreadReport();
        report.averageResponseTime = msg.getAverageResponseTime();
        report.dataReceived = msg.getDataReceived();
        report.failTransactions = msg.getFailTransactions();
        report.longest = (short) msg.getLongest();
        report.nTransactions = msg.getNTransactions();
        report.report = msg.getReport();
        report.shortest = (short) msg.getShortest();
        report.successTransactions = msg.getSuccessTransactions();
        report.totalExecutionTime = msg.getTotalExecutionTime();
        report.totalTransferingTime = msg.getTotalTransferingTime();
        report.transactionRate = msg.getTransactionRate();
        return report;
    }

    public ThreadReportMsg toProtBuffer() {
        return ThreadReportMsg.newBuilder()
                              .setAverageResponseTime(averageResponseTime)
                              .setDataReceived(dataReceived)
                              .setFailTransactions(failTransactions)
                              .setLongest(longest)
                              .setNTransactions(nTransactions)
                              .setReport(report)
                              .setShortest(shortest)
                              .setSuccessTransactions(successTransactions)
                              .setTotalExecutionTime(totalExecutionTime)
                              .setTotalTransferingTime(totalTransferingTime)
                              .setTransactionRate(transactionRate)
                              .build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n Transactions: " + nTransactions + "\n");
        sb.append("Success: " + successTransactions + "\n");
        sb.append("Fail: " + failTransactions + "\n");
        sb.append("Success Rate: " + (successTransactions / nTransactions) * 100 + "% \n");

        sb.append("Transaction Rate: " + String.format("%.2f", transactionRate)
                + " req/sec \n\n");
        sb.append("Total time: \n");
        sb.append("Transfering: " + totalTransferingTime + "ms \n");
        sb.append("Execution: " + totalExecutionTime + "ms \n");

        sb.append("\n");
        sb.append("Request time:\n");
        sb.append("- Average: " + averageResponseTime + "ms \n");
        sb.append("- Longest " + longest + "ms \n");
        sb.append("- Shortest " + shortest + "ms \n");
        sb.append("\n");
        sb.append("Received: " + dataReceived + " bytes");

        return sb.toString();
    }
}
