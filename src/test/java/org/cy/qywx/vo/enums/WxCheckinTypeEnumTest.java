package org.cy.qywx.vo.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WxCheckinTypeEnumTest {

    @Test
    void fromCode_returnsMatchingEnum() {
        assertThat(WxCheckinTypeEnum.fromCode(1)).isEqualTo(WxCheckinTypeEnum.NORMAL);
        assertThat(WxCheckinTypeEnum.fromCode(2)).isEqualTo(WxCheckinTypeEnum.OUTSIDE);
        assertThat(WxCheckinTypeEnum.fromCode(3)).isEqualTo(WxCheckinTypeEnum.ALL);
    }

    @Test
    void fromCode_unknownReturnsNull() {
        assertThat(WxCheckinTypeEnum.fromCode(99)).isNull();
        assertThat(WxCheckinTypeEnum.fromCode(null)).isNull();
    }

    @Test
    void getCodeAndDescription() {
        assertThat(WxCheckinTypeEnum.NORMAL.getCode()).isEqualTo(1);
        assertThat(WxCheckinTypeEnum.NORMAL.getDescription()).isEqualTo("上下班打卡");
    }
}
