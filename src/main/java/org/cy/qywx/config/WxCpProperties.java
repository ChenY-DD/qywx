package org.cy.qywx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wx.cp")
public class WxCpProperties {

    private String corpId;
    private String corpSecret;
    private Integer agentId;
    private ApprovalQuery approval = new ApprovalQuery();
    private Hr hr = new Hr();
    private Checkin checkin = new Checkin();

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

    public Hr getHr() {
        return hr;
    }

    public void setHr(Hr hr) {
        this.hr = hr;
    }

    public Checkin getCheckin() {
        return checkin;
    }

    public void setCheckin(Checkin checkin) {
        this.checkin = checkin;
    }

    /**
     * 打卡 / 考勤查询调优参数。复用顶层 {@code corpSecret}，因此本嵌套类没有 {@code secret} 字段。
     *
     * @author CY
     * Copyright (c) CY
     */
    public static class Checkin {
        /** 单段最大天数（API 上限 30）。 */
        private int segmentDays = 30;
        /** 单批最大 userId 数（API 上限 100）。 */
        private int userBatchSize = 100;
        /** 单批次最大重试次数。 */
        private int maxRetryAttempts = 3;
        /** 重试初始退避毫秒。 */
        private long retryBackoffMillis = 500L;
        /** 全局每秒请求数（&le;0 不限流）。 */
        private double requestsPerSecond = 0D;
        /** {@code qywxCheckinExecutor} 线程数。 */
        private int executorThreads = 8;

        public int getSegmentDays() {
            return segmentDays;
        }

        public void setSegmentDays(int segmentDays) {
            this.segmentDays = segmentDays;
        }

        public int getUserBatchSize() {
            return userBatchSize;
        }

        public void setUserBatchSize(int userBatchSize) {
            this.userBatchSize = userBatchSize;
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

    /**
     * 「智慧人事 / 人事助手」花名册接口配置。
     * <p>
     * 该应用使用独立的 secret，与通讯录 / 自建应用 secret 不互通。
     * 在「企业微信管理后台 → 应用管理 → 智慧人事 → 接口同步」中获取。
     */
    public static class Hr {
        private String secret;
        private Integer agentId;
        private int maxRetryAttempts = 5;
        private long retryBackoffMillis = 1000L;
        private double requestsPerSecond = 10D;
        private int executorThreads = 8;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Integer getAgentId() {
            return agentId;
        }

        public void setAgentId(Integer agentId) {
            this.agentId = agentId;
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
