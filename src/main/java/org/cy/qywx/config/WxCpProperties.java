package org.cy.qywx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 类说明：企业微信 Starter 配置属性。
 *
 * @author cy
 * Copyright (c) CY
 */
@ConfigurationProperties(prefix = "wx.cp")
public class WxCpProperties {

    /**
     * 字段说明：企业微信企业 ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String corpId;
    /**
     * 字段说明：企业微信应用 Secret。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String corpSecret;
    /**
     * 字段说明：企业微信应用 AgentId。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer agentId;
    /**
     * 字段说明：审批查询配置。
     *
     * @author cy
     * Copyright (c) CY
     */
    private ApprovalQuery approval = new ApprovalQuery();
    /**
     * 字段说明：HR 花名册查询配置。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Hr hr = new Hr();
    /**
     * 字段说明：考勤打卡查询配置。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Checkin checkin = new Checkin();

    /**
     * 获取企业ID。
     *
     * @return 企业ID
     *
     * @author cy
     * Copyright (c) CY
     */
    public String getCorpId() {
        return corpId;
    }

    /**
     * 设置企业ID。
     *
     * @param corpId 企业微信企业 ID
     *
     * @author cy
     * Copyright (c) CY
     */
    public void setCorpId(String corpId) {
        this.corpId = corpId;
    }

    /**
     * 获取企业密钥。
     *
     * @return 企业密钥
     *
     * @author cy
     * Copyright (c) CY
     */
    public String getCorpSecret() {
        return corpSecret;
    }

    /**
     * 设置企业密钥。
     *
     * @param corpSecret 企业微信应用 Secret
     *
     * @author cy
     * Copyright (c) CY
     */
    public void setCorpSecret(String corpSecret) {
        this.corpSecret = corpSecret;
    }

    /**
     * 获取应用ID。
     *
     * @return 应用ID
     *
     * @author cy
     * Copyright (c) CY
     */
    public Integer getAgentId() {
        return agentId;
    }

    /**
     * 设置应用ID。
     *
     * @param agentId 企业微信应用 AgentId
     *
     * @author cy
     * Copyright (c) CY
     */
    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    /**
     * 获取审批。
     *
     * @return 审批
     *
     * @author cy
     * Copyright (c) CY
     */
    public ApprovalQuery getApproval() {
        return approval;
    }

    /**
     * 设置审批。
     *
     * @param approval 审批查询配置
     *
     * @author cy
     * Copyright (c) CY
     */
    public void setApproval(ApprovalQuery approval) {
        this.approval = approval;
    }

    /**
     * 获取HR。
     *
     * @return HR
     *
     * @author cy
     * Copyright (c) CY
     */
    public Hr getHr() {
        return hr;
    }

    /**
     * 设置HR。
     *
     * @param hr HR 花名册查询配置
     *
     * @author cy
     * Copyright (c) CY
     */
    public void setHr(Hr hr) {
        this.hr = hr;
    }

    /**
     * 获取考勤打卡。
     *
     * @return 考勤打卡
     *
     * @author cy
     * Copyright (c) CY
     */
    public Checkin getCheckin() {
        return checkin;
    }

    /**
     * 设置考勤打卡。
     *
     * @param checkin 考勤打卡查询配置
     *
     * @author cy
     * Copyright (c) CY
     */
    public void setCheckin(Checkin checkin) {
        this.checkin = checkin;
    }

    /**
     * 类说明：考勤打卡。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static class Checkin {
        /**
         * 字段说明：分段天数。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int segmentDays = 30;
        /**
         * 字段说明：成员批次大小。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int userBatchSize = 100;
        /**
         * 字段说明：最大重试次数。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int maxRetryAttempts = 3;
        /**
         * 字段说明：重试退避毫秒。
         *
         * @author cy
         * Copyright (c) CY
         */
        private long retryBackoffMillis = 500L;
        /**
         * 字段说明：请求每秒。
         *
         * @author cy
         * Copyright (c) CY
         */
        private double requestsPerSecond = 0D;
        /**
         * 字段说明：执行器线程数。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int executorThreads = 8;

        /**
         * 获取分段天数。
         *
         * @return 分段天数
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getSegmentDays() {
            return segmentDays;
        }

        /**
         * 设置分段天数。
         *
         * @param segmentDays 分段天数
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setSegmentDays(int segmentDays) {
            this.segmentDays = segmentDays;
        }

        /**
         * 获取成员批次大小。
         *
         * @return 成员批次大小
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getUserBatchSize() {
            return userBatchSize;
        }

        /**
         * 设置成员批次大小。
         *
         * @param userBatchSize 成员批次大小
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setUserBatchSize(int userBatchSize) {
            this.userBatchSize = userBatchSize;
        }

        /**
         * 获取最大重试次数。
         *
         * @return 最大重试次数
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        /**
         * 设置最大重试次数。
         *
         * @param maxRetryAttempts 最大重试次数
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }

        /**
         * 获取重试退避毫秒。
         *
         * @return 重试退避毫秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public long getRetryBackoffMillis() {
            return retryBackoffMillis;
        }

        /**
         * 设置重试退避毫秒。
         *
         * @param retryBackoffMillis 重试退避毫秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setRetryBackoffMillis(long retryBackoffMillis) {
            this.retryBackoffMillis = retryBackoffMillis;
        }

        /**
         * 获取请求每秒。
         *
         * @return 请求每秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public double getRequestsPerSecond() {
            return requestsPerSecond;
        }

        /**
         * 设置请求每秒。
         *
         * @param requestsPerSecond 请求每秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setRequestsPerSecond(double requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        /**
         * 获取执行器线程数。
         *
         * @return 执行器线程数
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getExecutorThreads() {
            return executorThreads;
        }

        /**
         * 设置执行器线程数。
         *
         * @param executorThreads 执行器线程数
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setExecutorThreads(int executorThreads) {
            this.executorThreads = executorThreads;
        }
    }

    /**
     * 类说明：HR。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static class Hr {
        /**
         * 字段说明：密钥。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String secret;
        /**
         * 字段说明：企业微信应用 AgentId。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Integer agentId;
        /**
         * 字段说明：最大重试次数。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int maxRetryAttempts = 5;
        /**
         * 字段说明：重试退避毫秒。
         *
         * @author cy
         * Copyright (c) CY
         */
        private long retryBackoffMillis = 1000L;
        /**
         * 字段说明：请求每秒。
         *
         * @author cy
         * Copyright (c) CY
         */
        private double requestsPerSecond = 10D;
        /**
         * 字段说明：执行器线程数。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int executorThreads = 8;

        /**
         * 获取密钥。
         *
         * @return 密钥
         *
         * @author cy
         * Copyright (c) CY
         */
        public String getSecret() {
            return secret;
        }

        /**
         * 设置密钥。
         *
         * @param secret 密钥
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setSecret(String secret) {
            this.secret = secret;
        }

        /**
         * 获取应用ID。
         *
         * @return 应用ID
         *
         * @author cy
         * Copyright (c) CY
         */
        public Integer getAgentId() {
            return agentId;
        }

        /**
         * 设置应用ID。
         *
         * @param agentId 企业微信应用 AgentId
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setAgentId(Integer agentId) {
            this.agentId = agentId;
        }

        /**
         * 获取最大重试次数。
         *
         * @return 最大重试次数
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        /**
         * 设置最大重试次数。
         *
         * @param maxRetryAttempts 最大重试次数
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }

        /**
         * 获取重试退避毫秒。
         *
         * @return 重试退避毫秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public long getRetryBackoffMillis() {
            return retryBackoffMillis;
        }

        /**
         * 设置重试退避毫秒。
         *
         * @param retryBackoffMillis 重试退避毫秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setRetryBackoffMillis(long retryBackoffMillis) {
            this.retryBackoffMillis = retryBackoffMillis;
        }

        /**
         * 获取请求每秒。
         *
         * @return 请求每秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public double getRequestsPerSecond() {
            return requestsPerSecond;
        }

        /**
         * 设置请求每秒。
         *
         * @param requestsPerSecond 请求每秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setRequestsPerSecond(double requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        /**
         * 获取执行器线程数。
         *
         * @return 执行器线程数
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getExecutorThreads() {
            return executorThreads;
        }

        /**
         * 设置执行器线程数。
         *
         * @param executorThreads 执行器线程数
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setExecutorThreads(int executorThreads) {
            this.executorThreads = executorThreads;
        }
    }

    /**
     * 类说明：审批查询。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static class ApprovalQuery {
        /**
         * 字段说明：分段天数。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int segmentDays = 29;
        /**
         * 字段说明：分页大小。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int pageSize = 100;
        /**
         * 字段说明：最大重试次数。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int maxRetryAttempts = 3;
        /**
         * 字段说明：重试退避毫秒。
         *
         * @author cy
         * Copyright (c) CY
         */
        private long retryBackoffMillis = 300L;
        /**
         * 字段说明：请求每秒。
         *
         * @author cy
         * Copyright (c) CY
         */
        private double requestsPerSecond = 0D;
        /**
         * 字段说明：执行器线程数。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int executorThreads = 8;

        /**
         * 获取分段天数。
         *
         * @return 分段天数
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getSegmentDays() {
            return segmentDays;
        }

        /**
         * 设置分段天数。
         *
         * @param segmentDays 分段天数
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setSegmentDays(int segmentDays) {
            this.segmentDays = segmentDays;
        }

        /**
         * 获取分页大小。
         *
         * @return 分页大小
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getPageSize() {
            return pageSize;
        }

        /**
         * 设置分页大小。
         *
         * @param pageSize 分页大小
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        /**
         * 获取最大重试次数。
         *
         * @return 最大重试次数
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        /**
         * 设置最大重试次数。
         *
         * @param maxRetryAttempts 最大重试次数
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }

        /**
         * 获取重试退避毫秒。
         *
         * @return 重试退避毫秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public long getRetryBackoffMillis() {
            return retryBackoffMillis;
        }

        /**
         * 设置重试退避毫秒。
         *
         * @param retryBackoffMillis 重试退避毫秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setRetryBackoffMillis(long retryBackoffMillis) {
            this.retryBackoffMillis = retryBackoffMillis;
        }

        /**
         * 获取请求每秒。
         *
         * @return 请求每秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public double getRequestsPerSecond() {
            return requestsPerSecond;
        }

        /**
         * 设置请求每秒。
         *
         * @param requestsPerSecond 请求每秒
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setRequestsPerSecond(double requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        /**
         * 获取执行器线程数。
         *
         * @return 执行器线程数
         *
         * @author cy
         * Copyright (c) CY
         */
        public int getExecutorThreads() {
            return executorThreads;
        }

        /**
         * 设置执行器线程数。
         *
         * @param executorThreads 执行器线程数
         *
         * @author cy
         * Copyright (c) CY
         */
        public void setExecutorThreads(int executorThreads) {
            this.executorThreads = executorThreads;
        }
    }
}
