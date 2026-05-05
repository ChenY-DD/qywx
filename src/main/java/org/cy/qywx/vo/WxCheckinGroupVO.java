package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 类说明：考勤打卡考勤组业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinGroupVO implements Serializable {

    /**
     * 字段说明：序列化版本号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：考勤组ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer groupId;

    /**
     * 字段说明：考勤组名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String groupName;

    /**
     * 字段说明：考勤组类型。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String groupType;

    /**
     * 字段说明：create时间。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Date createTime;

    /**
     * 字段说明：排班列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinScheduleVO> schedules;

    /**
     * 字段说明：范围。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxCheckinRangeVO range;

    /**
     * 字段说明：白名单成员列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<String> whiteUsers;

    /**
     * 字段说明：是否要求拍照打卡。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean needPhoto;

    /**
     * 字段说明：备注是否允许使用本地图片。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean noteCanUseLocalPic;

    /**
     * 字段说明：是否允许非工作日打卡。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean allowCheckinOffWorkday;

    /**
     * 字段说明：是否允许非工作日提交申请。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean allowApplyOffWorkday;

    /**
     * 字段说明：同步假期列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean syncHolidays;

    /**
     * 字段说明：特殊工作日日期列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<Date> speWorkdays;

    /**
     * 字段说明：特殊休息日日期列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<Date> speOffdays;
}
