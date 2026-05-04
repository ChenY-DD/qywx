package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.vo.enums.WxCheckinResultEnum;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 单条打卡记录 VO（来源 getcheckindata）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 考勤组名称 */
    private String groupName;

    /** 考勤组 ID */
    private Integer groupId;

    /** 班次 ID */
    private Integer scheduleId;

    /** 时段 ID */
    private Integer timelineId;

    /** 打卡类型（上班打卡 / 下班打卡 / 外出打卡，企业微信原始字符串） */
    private String checkinType;

    /** 打卡时间 */
    private Date checkinTime;

    /** 应打卡时间（仅排班 / 固定时段） */
    private Date schCheckinTime;

    /** 打卡地点标题 */
    private String locationTitle;

    /** 打卡地点详情 */
    private String locationDetail;

    /** Wi-Fi 名称 */
    private String wifiName;

    /** Wi-Fi MAC */
    private String wifiMac;

    /** 备注 */
    private String notes;

    /** 附件 mediaId 列表 */
    private List<String> mediaIds;

    /** 设备 ID */
    private String deviceId;

    /** 经度（实际值的 1_000_000 倍） */
    private Integer lng;

    /** 纬度（实际值的 1_000_000 倍） */
    private Integer lat;

    /** 异常结果（首个匹配） */
    private WxCheckinResultEnum exceptionResult;

    /** 原始异常字符串（企业微信原文，多值分号分隔） */
    private String rawExceptionType;
}
