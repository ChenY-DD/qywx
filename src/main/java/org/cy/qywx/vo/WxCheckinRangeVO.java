package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 考勤组可用范围 VO（部门 / 标签 / userId 白名单）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinRangeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 部门 ID 列表 */
    private List<Integer> partyIds;

    /** 用户 ID 列表 */
    private List<String> userIds;

    /** 标签 ID 列表 */
    private List<Integer> tagIds;
}
