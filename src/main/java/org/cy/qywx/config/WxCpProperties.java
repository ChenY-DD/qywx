package org.cy.qywx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wx.cp")
public class WxCpProperties {

    private String corpId;
    private String corpSecret;
    private Integer agentId;
    private ApprovalQuery approval = new ApprovalQuery();

    public String getCorpId() {
        return corpId;
    }

    public void setCorpId(String corpId) {
        this.corpId = corpId;
    }

    public String getCorpSecret() {
        return corpSecret;
    }

    public void setCorpSecret(String corpSecret) {
        this.corpSecret = corpSecret;
    }

    public Integer getAgentId() {
        return agentId;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public ApprovalQuery getApproval() {
        return approval;
    }

    public void setApproval(ApprovalQuery approval) {
        this.approval = approval;
    }

    public static class ApprovalQuery {
        private int segmentDays = 29;
        private int pageSize = 100;
        private int maxRetryAttempts = 3;
        private long retryBackoffMillis = 300L;
        private double requestsPerSecond = 0D;
        private int executorThreads = 8;

        public int getSegmentDays() {
            return segmentDays;
        }

        public void setSegmentDays(int segmentDays) {
            this.segmentDays = segmentDays;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }

        public long getRetryBackoffMillis() {
            return retryBackoffMillis;
        }

        public void setRetryBackoffMillis(long retryBackoffMillis) {
            this.retryBackoffMillis = retryBackoffMillis;
        }

        public double getRequestsPerSecond() {
            return requestsPerSecond;
        }

        public void setRequestsPerSecond(double requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        public int getExecutorThreads() {
            return executorThreads;
        }

        public void setExecutorThreads(int executorThreads) {
            this.executorThreads = executorThreads;
        }
    }
}
