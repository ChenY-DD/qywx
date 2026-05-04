package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 考勤组 VO（来源 getcorpcheckinoption / WxJava 中实际方法名为 getCropCheckinOption）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考勤组 ID */
    private Integer groupId;

    /** 考勤组名称 */
    private String groupName;

    /** 考勤组类型描述 */
    private String groupType;

    /** 创建时间 */
    private Date createTime;

    /** 班次列表 */
    private List<WxCheckinScheduleVO> schedules;

    /** 可用范围（部门 / 标签 / userId） */
    private WxCheckinRangeVO range;

    /** 白名单（不需要打卡的 userId） */
    private List<String> whiteUsers;

    /** 是否需要拍照 */
    private Boolean needPhoto;

    /** 备注是否允许使用本地图片 */
    private Boolean noteCanUseLocalPic;

    /** 非工作日是否允许打卡 */
    private Boolean allowCheckinOffWorkday;

    /** 非工作日是否允许补卡 */
    private Boolean allowApplyOffWorkday;

    /** 是否自动同步法定节假日 */
    private Boolean syncHolidays;

    /** 特殊上班日 */
    private List<Date> speWorkdays;

    /** 特殊放假日 */
    private List<Date> speOffdays;
}
