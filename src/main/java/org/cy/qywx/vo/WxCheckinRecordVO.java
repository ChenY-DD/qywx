package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.vo.enums.WxCheckinResultEnum;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 类说明：考勤打卡记录业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinRecordVO implements Serializable {

    /**
     * 字段说明：序列化版本号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：成员 userId。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String userId;

    /**
     * 字段说明：考勤组名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String groupName;

    /**
     * 字段说明：考勤组ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer groupId;

    /**
     * 字段说明：排班ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer scheduleId;

    /**
     * 字段说明：时间轴ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer timelineId;

    /**
     * 字段说明：考勤打卡类型。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String checkinType;

    /**
     * 字段说明：实际打卡时间。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Date checkinTime;

    /**
     * 字段说明：排班应打卡时间。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Date schCheckinTime;

    /**
     * 字段说明：地点标题。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String locationTitle;

    /**
     * 字段说明：地点详情。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String locationDetail;

    /**
     * 字段说明：Wi-Fi名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String wifiName;

    /**
     * 字段说明：Wi-FiMAC。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String wifiMac;

    /**
     * 字段说明：备注。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String notes;

    /**
     * 字段说明：媒体ID列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<String> mediaIds;

    /**
     * 字段说明：设备ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String deviceId;

    /**
     * 字段说明：经度。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer lng;

    /**
     * 字段说明：纬度。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer lat;

    /**
     * 字段说明：异常结果。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxCheckinResultEnum exceptionResult;

    /**
     * 字段说明：raw异常类型。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String rawExceptionType;
}
