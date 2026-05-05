package org.cy.qywx.config;

import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.cy.qywx.util.WxApiClient;
import org.cy.qywx.util.WxApprovalQueryOptions;
import org.cy.qywx.util.WxApprovalQueryUtil;
import org.cy.qywx.util.WxCheckinQueryOptions;
import org.cy.qywx.util.WxCheckinQueryUtil;
import org.cy.qywx.util.WxContactQueryUtil;
import org.cy.qywx.util.WxHrRosterQueryUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 类说明：企业微信 Starter 自动配置。
 *
 * @author cy
 * Copyright (c) CY
 */
@AutoConfiguration
@ConditionalOnClass(WxCpService.class)
@EnableConfigurationProperties(WxCpProperties.class)
public class QywxWecomAutoConfiguration {

    /**
     * 字段说明：审批执行器bean名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static final String APPROVAL_EXECUTOR_BEAN_NAME = "qywxApprovalQueryExecutor";
    /**
     * 字段说明：HR执行器bean名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static final String HR_EXECUTOR_BEAN_NAME = "qywxHrRosterExecutor";
    /**
     * 字段说明：HR企业微信servicebean名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static final String HR_CP_SERVICE_BEAN_NAME = "qywxHrCpService";
    /**
     * 字段说明：考勤打卡执行器bean名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static final String CHECKIN_EXECUTOR_BEAN_NAME = "qywxCheckinExecutor";

    /**
     * 执行 wxCpService 相关逻辑。
     *
     * @param properties properties
     * @return 企业微信service
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "wx.cp", name = {"corp-id", "corp-secret", "agent-id"})
    public WxCpService wxCpService(WxCpProperties properties) {
        WxCpService service = new WxCpServiceImpl();
        WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
        config.setCorpId(properties.getCorpId());
        config.setCorpSecret(properties.getCorpSecret());
        config.setAgentId(properties.getAgentId());
        service.setWxCpConfigStorage(config);
        return service;
    }

    /**
     * 执行 qywxApprovalQueryExecutor 相关逻辑。
     *
     * @param properties properties
     * @return 执行器service
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean(name = APPROVAL_EXECUTOR_BEAN_NAME, destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = APPROVAL_EXECUTOR_BEAN_NAME)
    public ExecutorService qywxApprovalQueryExecutor(WxCpProperties properties) {
        int threads = Math.max(1, properties.getApproval().getExecutorThreads());
        return Executors.newFixedThreadPool(threads);
    }

    /**
     * 执行 wxContactQueryUtil 相关逻辑。
     *
     * @param wxCpService 企业微信企业微信service
     * @return contact查询util工具
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxContactQueryUtil wxContactQueryUtil(WxCpService wxCpService) {
        return new WxContactQueryUtil(wxCpService);
    }

    /**
     * 执行 wxApiClient 相关逻辑。
     *
     * @param wxCpService 企业微信企业微信service
     * @return API客户端
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxApiClient wxApiClient(WxCpService wxCpService) {
        return new WxApiClient(wxCpService);
    }

    /**
     * 执行 wxApprovalQueryUtil 相关逻辑。
     *
     * @param wxCpService 企业微信企业微信service
     * @param approvalExecutor 审批执行器
     * @param properties properties
     * @return 审批查询util工具
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxApprovalQueryUtil wxApprovalQueryUtil(
            WxCpService wxCpService,
            @Qualifier(APPROVAL_EXECUTOR_BEAN_NAME) ExecutorService approvalExecutor,
            WxCpProperties properties
    ) {
        WxCpProperties.ApprovalQuery approval = properties.getApproval();
        WxApprovalQueryOptions options = new WxApprovalQueryOptions(
                approval.getSegmentDays(),
                approval.getPageSize(),
                approval.getMaxRetryAttempts(),
                approval.getRetryBackoffMillis(),
                approval.getRequestsPerSecond()
        );
        return new WxApprovalQueryUtil(wxCpService, approvalExecutor, options);
    }

    /**
     * 执行 qywxHrCpService 相关逻辑。
     *
     * @param properties properties
     * @return 企业微信service
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean(name = HR_CP_SERVICE_BEAN_NAME)
    @ConditionalOnMissingBean(name = HR_CP_SERVICE_BEAN_NAME)
    @ConditionalOnProperty(prefix = "wx.cp", name = {"corp-id", "hr.secret"})
    public WxCpService qywxHrCpService(WxCpProperties properties) {
        WxCpService service = new WxCpServiceImpl();
        WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
        config.setCorpId(properties.getCorpId());
        config.setCorpSecret(properties.getHr().getSecret());
        if (properties.getHr().getAgentId() != null) {
            config.setAgentId(properties.getHr().getAgentId());
        }
        service.setWxCpConfigStorage(config);
        return service;
    }

    /**
     * 执行 qywxHrRosterExecutor 相关逻辑。
     *
     * @param properties properties
     * @return 执行器service
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean(name = HR_EXECUTOR_BEAN_NAME, destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = HR_EXECUTOR_BEAN_NAME)
    @ConditionalOnProperty(prefix = "wx.cp", name = {"corp-id", "hr.secret"})
    public ExecutorService qywxHrRosterExecutor(WxCpProperties properties) {
        int threads = Math.max(1, properties.getHr().getExecutorThreads());
        return Executors.newFixedThreadPool(threads);
    }

    /**
     * 执行 wxHrRosterQueryUtil 相关逻辑。
     *
     * @param hrCpService HR企业微信service
     * @param contactQueryUtil contact查询util
     * @param hrExecutor HR执行器
     * @param properties properties
     * @return HR花名册查询util工具
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(name = HR_CP_SERVICE_BEAN_NAME)
    public WxHrRosterQueryUtil wxHrRosterQueryUtil(
            @Qualifier(HR_CP_SERVICE_BEAN_NAME) WxCpService hrCpService,
            WxContactQueryUtil contactQueryUtil,
            @Qualifier(HR_EXECUTOR_BEAN_NAME) ExecutorService hrExecutor,
            WxCpProperties properties
    ) {
        WxCpProperties.Hr hr = properties.getHr();
        return new WxHrRosterQueryUtil(
                hrCpService,
                contactQueryUtil,
                hrExecutor,
                hr.getMaxRetryAttempts(),
                hr.getRetryBackoffMillis(),
                hr.getRequestsPerSecond()
        );
    }

    /**
     * 执行 qywxCheckinExecutor 相关逻辑。
     *
     * @param properties properties
     * @return 执行器service
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean(name = CHECKIN_EXECUTOR_BEAN_NAME, destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = CHECKIN_EXECUTOR_BEAN_NAME)
    public ExecutorService qywxCheckinExecutor(WxCpProperties properties) {
        int threads = Math.max(1, properties.getCheckin().getExecutorThreads());
        return Executors.newFixedThreadPool(threads);
    }

    /**
     * 执行 wxCheckinQueryUtil 相关逻辑。
     *
     * @param wxCpService 企业微信企业微信service
     * @param checkinExecutor 考勤打卡执行器
     * @param properties properties
     * @return 考勤打卡查询util工具
     *
     * @author cy
     * Copyright (c) CY
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxCheckinQueryUtil wxCheckinQueryUtil(
            WxCpService wxCpService,
            @Qualifier(CHECKIN_EXECUTOR_BEAN_NAME) ExecutorService checkinExecutor,
            WxCpProperties properties
    ) {
        WxCpProperties.Checkin c = properties.getCheckin();
        WxCheckinQueryOptions options = new WxCheckinQueryOptions(
                c.getSegmentDays(),
                c.getUserBatchSize(),
                c.getMaxRetryAttempts(),
                c.getRetryBackoffMillis(),
                c.getRequestsPerSecond()
        );
        return new WxCheckinQueryUtil(wxCpService, checkinExecutor, options);
    }
}
