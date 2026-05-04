package org.cy.qywx.config;

import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.cy.qywx.util.WxApiClient;
import org.cy.qywx.util.WxApprovalQueryOptions;
import org.cy.qywx.util.WxApprovalQueryUtil;
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

@AutoConfiguration
@ConditionalOnClass(WxCpService.class)
@EnableConfigurationProperties(WxCpProperties.class)
public class QywxWecomAutoConfiguration {

    public static final String APPROVAL_EXECUTOR_BEAN_NAME = "qywxApprovalQueryExecutor";
    public static final String HR_EXECUTOR_BEAN_NAME = "qywxHrRosterExecutor";
    public static final String HR_CP_SERVICE_BEAN_NAME = "qywxHrCpService";

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

    @Bean(name = APPROVAL_EXECUTOR_BEAN_NAME, destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = APPROVAL_EXECUTOR_BEAN_NAME)
    public ExecutorService qywxApprovalQueryExecutor(WxCpProperties properties) {
        int threads = Math.max(1, properties.getApproval().getExecutorThreads());
        return Executors.newFixedThreadPool(threads);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxContactQueryUtil wxContactQueryUtil(WxCpService wxCpService) {
        return new WxContactQueryUtil(wxCpService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxApiClient wxApiClient(WxCpService wxCpService) {
        return new WxApiClient(wxCpService);
    }

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

    @Bean(name = HR_EXECUTOR_BEAN_NAME, destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = HR_EXECUTOR_BEAN_NAME)
    @ConditionalOnProperty(prefix = "wx.cp", name = {"corp-id", "hr.secret"})
    public ExecutorService qywxHrRosterExecutor(WxCpProperties properties) {
        int threads = Math.max(1, properties.getHr().getExecutorThreads());
        return Executors.newFixedThreadPool(threads);
    }

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
}
