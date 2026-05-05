package org.cy.qywx.vo.enums;

/**
 * 枚举说明：考勤打卡类型??。
 *
 * @author cy
 * Copyright (c) CY
 */
public enum WxCheckinTypeEnum {

    NORMAL(1, "上下班打卡"),
    OUTSIDE(2, "外出打卡"),
    ALL(3, "全部");

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

    WxCheckinTypeEnum(int code, String description) {
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
     * @return 考勤打卡类型??
     *
     * @author cy
     * Copyright (c) CY
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
