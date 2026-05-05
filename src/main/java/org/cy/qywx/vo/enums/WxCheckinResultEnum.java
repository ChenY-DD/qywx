package org.cy.qywx.vo.enums;

/**
 * 枚举说明：考勤打卡结果??。
 *
 * @author cy
 * Copyright (c) CY
 */
public enum WxCheckinResultEnum {

    NORMAL("正常"),
    TIME_EXCEPTION("时间异常"),
    LOCATION_EXCEPTION("地点异常"),
    NOT_CHECKED_IN("未打卡"),
    INVALID("无效"),
    UNKNOWN("未知");

    /**
     * 字段说明：文本。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final String text;

    WxCheckinResultEnum(String text) {
        this.text = text;
    }

    /**
     * 获取文本。
     *
     * @return 文本
     *
     * @author cy
     * Copyright (c) CY
     */
    public String getText() {
        return text;
    }

    /**
     * 执行 fromText 相关逻辑。
     *
     * @param text 文本
     * @return 考勤打卡结果??
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxCheckinResultEnum fromText(String text) {
        if (text == null || text.isBlank()) {
            return UNKNOWN;
        }
        String trimmed = text.trim();
        int sep = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == ';' || c == '；') {
                sep = i;
                break;
            }
        }
        String first = sep >= 0 ? trimmed.substring(0, sep).trim() : trimmed;
        for (WxCheckinResultEnum e : values()) {
            if (e.text.equals(first)) {
                return e;
            }
        }
        return UNKNOWN;
    }
}
