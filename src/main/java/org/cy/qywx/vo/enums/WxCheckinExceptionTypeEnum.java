package org.cy.qywx.vo.enums;

/**
 * 打卡日报异常类型枚举。
 * <p>对应企业微信 {@code getcheckin_daydata} 接口返回的 {@code exception_infos[].exception} 字段：
 * 1=迟到、2=早退、3=缺卡、4=旷工、5=地点异常、6=设备异常。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
public enum WxCheckinExceptionTypeEnum {

    LATE(1, "迟到"),
    EARLY_LEAVE(2, "早退"),
    MISSING_CARD(3, "缺卡"),
    ABSENT(4, "旷工"),
    LOCATION_EXCEPTION(5, "地点异常"),
    DEVICE_EXCEPTION(6, "设备异常");

    private final int code;
    private final String description;

    WxCheckinExceptionTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取枚举对应的企业微信 API 数值。
     *
     * @return code
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取枚举的中文描述。
     *
     * @return 中文说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据企业微信数值反查枚举。
     *
     * @param code 企业微信 exception 数值
     * @return 对应枚举；未匹配返回 {@code null}
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
