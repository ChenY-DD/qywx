package org.cy.qywx.vo.enums;

/**
 * 枚举说明：考勤打卡异常类型??。
 *
 * @author cy
 * Copyright (c) CY
 */
public enum WxCheckinExceptionTypeEnum {

    LATE(1, "迟到"),
    EARLY_LEAVE(2, "早退"),
    MISSING_CARD(3, "缺卡"),
    ABSENT(4, "旷工"),
    LOCATION_EXCEPTION(5, "地点异常"),
    DEVICE_EXCEPTION(6, "设备异常");

    /**
     * 字段说明：编码。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final int code;
    /**
     * 字段说明：描述。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final String description;

    WxCheckinExceptionTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取编码。
     *
     * @return 编码
     *
     * @author cy
     * Copyright (c) CY
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取描述。
     *
     * @return 描述
     *
     * @author cy
     * Copyright (c) CY
     */
    public String getDescription() {
        return description;
    }

    /**
     * 执行 fromCode 相关逻辑。
     *
     * @param code 编码
     * @return 考勤打卡异常类型??
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxCheckinExceptionTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WxCheckinExceptionTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
