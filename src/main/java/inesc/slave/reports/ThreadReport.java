package inesc.slave.reports;

public class ThreadReport {
    public int nTransactions;
    public int successTransactions;
    public int averageResponseTime;
    public int transactionRate;
    public short longest;
    public short shortest;
    public int totalTransferingTime;
    public int failTransactions;
    public String report;
    public long totalExecutionTime;
    public long dataReceived;

    public int preExecution(short[] historyCounter) {
        nTransactions = 0;
        for (int i = 0; i < historyCounter.length; i++) {
            nTransactions += historyCounter[i];
        }
        return nTransactions;
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
        transactionRate = (int) (nTransactions / totalExecutionTime);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Transactions: " + nTransactions + "\n");
        sb.append("Success: " + successTransactions + "\n");
        sb.append("Fail: " + failTransactions + "\n");
        sb.append("Success Rate: " + (successTransactions / nTransactions) * 100 + "% \n");

        sb.append("\n");
        sb.append("Transaction Rate" + transactionRate + "\n");
        sb.append("Total Transfering time:" + totalTransferingTime + "\n");
        sb.append("Total Exec Time:" + totalExecutionTime + "\n");

        sb.append("\n");
        sb.append("Average: " + averageResponseTime + "\n");
        sb.append("Longest " + longest + "\n");
        sb.append("Shortest " + shortest + "\n");
        sb.append("\n");
        sb.append("Received: " + dataReceived + " bytes");

        return sb.toString();
    }
}
