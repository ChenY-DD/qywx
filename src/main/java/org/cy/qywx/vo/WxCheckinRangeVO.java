package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类说明：考勤打卡范围业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinRangeVO implements Serializable {

    /**
     * 字段说明：序列化版本号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：partyID列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<String> partyIds;

    /**
     * 字段说明：成员 userId 列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<String> userIds;

    /**
     * 字段说明：tagID列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<Integer> tagIds;
}
