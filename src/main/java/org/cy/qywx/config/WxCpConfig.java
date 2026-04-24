package org.cy.qywx.config;

import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WxCpConfig {

    @Value("${wx.cp.corp-id}")
    private String corpId;

    @Value("${wx.cp.corp-secret}")
    private String corpSecret;

    @Value("${wx.cp.agent-id}")
    private Integer agentId;

    @Bean
    public WxCpService wxCpService() {
        WxCpService service = new WxCpServiceImpl();
        WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
        config.setCorpId(corpId);
        config.setCorpSecret(corpSecret);  // 或具有审批权限的应用secret
        config.setAgentId(agentId);
        // 审批应用AgentId
        service.setWxCpConfigStorage(config);
        return service;
    }
}
