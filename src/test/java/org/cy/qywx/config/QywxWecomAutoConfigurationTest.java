package org.cy.qywx.config;

import me.chanjar.weixin.cp.api.WxCpService;
import org.cy.qywx.util.WxApprovalQueryUtil;
import org.cy.qywx.util.WxCheckinQueryUtil;
import org.cy.qywx.util.WxContactQueryUtil;
import org.cy.qywx.util.WxHrRosterQueryUtil;
import org.cy.qywx.util.export.WxExportUtil;
import org.cy.qywx.util.message.WxMessagePushUtil;
import org.cy.qywx.util.oauth2.WxOauth2Util;
import org.cy.qywx.util.robot.WxIntelligentRobotUtil;
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

    private final ApplicationContextRunner contextRunnerWithoutCpService = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(QywxWecomAutoConfiguration.class));

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

    @Test
    void checkinBeansRegisteredWhenCpServicePresent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WxCheckinQueryUtil.class);
            assertThat(context).hasBean(QywxWecomAutoConfiguration.CHECKIN_EXECUTOR_BEAN_NAME);
        });
    }

    @Test
    void checkinUtilAbsentWhenCpServiceMissing() {
        contextRunnerWithoutCpService.run(context -> {
            assertThat(context).doesNotHaveBean(WxCheckinQueryUtil.class);
        });
    }

    @Test
    void checkinPropertiesPropagated() {
        contextRunner
                .withPropertyValues(
                        "wx.cp.checkin.segment-days=15",
                        "wx.cp.checkin.user-batch-size=50",
                        "wx.cp.checkin.requests-per-second=2"
                )
                .run(context -> {
                    WxCpProperties props = context.getBean(WxCpProperties.class);
                    assertThat(props.getCheckin().getSegmentDays()).isEqualTo(15);
                    assertThat(props.getCheckin().getUserBatchSize()).isEqualTo(50);
                    assertThat(props.getCheckin().getRequestsPerSecond()).isEqualTo(2D);
                });
    }

    @Test
    void shouldStartWhenBothContactAndHrConfigured() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(QywxWecomAutoConfiguration.class))
                .withPropertyValues(
                        "wx.cp.corp-id=test-corp-id",
                        "wx.cp.corp-secret=test-secret",
                        "wx.cp.agent-id=1000002",
                        "wx.cp.hr.secret=test-hr-secret"
                )
                .run(context -> {
                    // 两个 WxCpService（主 + qywxHrCpService）共存时，注入主 service 的工具不应再有歧义
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(WxCpService.class)).hasSize(2);
                    assertThat(context).hasSingleBean(WxContactQueryUtil.class);
                    assertThat(context).hasSingleBean(WxHrRosterQueryUtil.class);
                });
    }

    @Test
    void newModuleBeansRegisteredWhenCpServicePresent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WxMessagePushUtil.class);
            assertThat(context).hasSingleBean(WxOauth2Util.class);
            assertThat(context).hasSingleBean(WxIntelligentRobotUtil.class);
            assertThat(context).hasSingleBean(WxExportUtil.class);
        });
    }
}
