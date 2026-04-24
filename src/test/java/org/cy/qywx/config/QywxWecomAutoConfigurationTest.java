package org.cy.qywx.config;

import me.chanjar.weixin.cp.api.WxCpService;
import org.cy.qywx.util.WxApprovalQueryUtil;
import org.cy.qywx.util.WxContactQueryUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QywxWecomAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(QywxWecomAutoConfiguration.class))
            .withBean(WxCpService.class, () -> mock(WxCpService.class))
            .withPropertyValues(
                    "wx.cp.corp-id=test-corp-id",
                    "wx.cp.corp-secret=test-secret",
                    "wx.cp.agent-id=1000002"
            );

    @Test
    void shouldCreateStarterBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WxCpProperties.class);
            assertThat(context).hasSingleBean(WxCpService.class);
            assertThat(context).hasSingleBean(WxContactQueryUtil.class);
            assertThat(context).hasSingleBean(WxApprovalQueryUtil.class);
            assertThat(context).hasBean(QywxWecomAutoConfiguration.APPROVAL_EXECUTOR_BEAN_NAME);
        });
    }
}
