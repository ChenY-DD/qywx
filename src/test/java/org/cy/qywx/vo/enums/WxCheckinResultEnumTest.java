package org.cy.qywx.vo.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WxCheckinResultEnumTest {

    @Test
    void fromText_recognisedValues() {
        assertThat(WxCheckinResultEnum.fromText("正常")).isEqualTo(WxCheckinResultEnum.NORMAL);
        assertThat(WxCheckinResultEnum.fromText("时间异常")).isEqualTo(WxCheckinResultEnum.TIME_EXCEPTION);
        assertThat(WxCheckinResultEnum.fromText("地点异常")).isEqualTo(WxCheckinResultEnum.LOCATION_EXCEPTION);
        assertThat(WxCheckinResultEnum.fromText("未打卡")).isEqualTo(WxCheckinResultEnum.NOT_CHECKED_IN);
        assertThat(WxCheckinResultEnum.fromText("无效")).isEqualTo(WxCheckinResultEnum.INVALID);
    }

    @Test
    void fromText_handlesSemicolonSeparated() {
        assertThat(WxCheckinResultEnum.fromText("时间异常;地点异常"))
                .isEqualTo(WxCheckinResultEnum.TIME_EXCEPTION);
    }

    @Test
    void fromText_unrecognisedReturnsUnknown() {
        assertThat(WxCheckinResultEnum.fromText("外星人异常")).isEqualTo(WxCheckinResultEnum.UNKNOWN);
        assertThat(WxCheckinResultEnum.fromText(null)).isEqualTo(WxCheckinResultEnum.UNKNOWN);
        assertThat(WxCheckinResultEnum.fromText("")).isEqualTo(WxCheckinResultEnum.UNKNOWN);
    }
}
