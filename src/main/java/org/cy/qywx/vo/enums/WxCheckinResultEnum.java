package org.cy.qywx.vo.enums;

/**
 * 单次打卡结果枚举。
 * <p>对应企业微信 {@code getcheckindata} 接口返回的 {@code exception_type} 字段（中文字符串）。
 * 该字段企业微信文档允许多值分号分隔，本工具按第一个匹配项映射；未识别的字符串归到 {@link #UNKNOWN}。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
public enum WxCheckinResultEnum {

    NORMAL("正常"),
    TIME_EXCEPTION("时间异常"),
    LOCATION_EXCEPTION("地点异常"),
    NOT_CHECKED_IN("未打卡"),
    INVALID("无效"),
    UNKNOWN("未知");

    private final String text;

    WxCheckinResultEnum(String text) {
        this.text = text;
    }

    /**
     * 获取枚举对应的企业微信 API 中文文本。
     *
     * @return 中文文本
     */
    public String getText() {
        return text;
    }

    /**
     * 根据企业微信返回的中文字符串反查枚举。
     *
     * @param text 中文字符串，可能含分号分隔的多个值
     * @return 第一个匹配的枚举；空 / null / 未匹配返回 {@link #UNKNOWN}
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
