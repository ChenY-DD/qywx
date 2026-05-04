package org.cy.qywx.vo.enums;

/**
 * 打卡记录类型枚举。
 * <p>对应企业微信 {@code getcheckindata} 接口的 {@code opencheckindatatype} 请求参数：
 * 1=上下班打卡、2=外出打卡、3=全部。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
public enum WxCheckinTypeEnum {

    NORMAL(1, "上下班打卡"),
    OUTSIDE(2, "外出打卡"),
    ALL(3, "全部");

    private final int code;
    private final String description;

    WxCheckinTypeEnum(int code, String description) {
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
     * 根据企业微信返回的数值反查枚举。
     *
     * @param code 企业微信数值
     * @return 对应枚举；未匹配返回 {@code null}
     */
    public static WxCheckinTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WxCheckinTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
