package org.cy.qywx.vo.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WxCheckinExceptionTypeEnumTest {

    @Test
    void fromCode_returnsMatchingEnum() {
        assertThat(WxCheckinExceptionTypeEnum.fromCode(1)).isEqualTo(WxCheckinExceptionTypeEnum.LATE);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(2)).isEqualTo(WxCheckinExceptionTypeEnum.EARLY_LEAVE);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(3)).isEqualTo(WxCheckinExceptionTypeEnum.MISSING_CARD);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(4)).isEqualTo(WxCheckinExceptionTypeEnum.ABSENT);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(5)).isEqualTo(WxCheckinExceptionTypeEnum.LOCATION_EXCEPTION);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(6)).isEqualTo(WxCheckinExceptionTypeEnum.DEVICE_EXCEPTION);
    }

    @Test
    void fromCode_unknownReturnsNull() {
        assertThat(WxCheckinExceptionTypeEnum.fromCode(0)).isNull();
        assertThat(WxCheckinExceptionTypeEnum.fromCode(null)).isNull();
    }
}
