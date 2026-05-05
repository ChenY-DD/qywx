package org.cy.qywx.util;

import org.cy.qywx.vo.WxCheckinMonthDataVO;

import java.util.List;

/**
 * 记录说明：考勤打卡月数据结果。
 *
 * @param monthDataList 月数据list
 * @param failures 批量查询失败记录列表
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxCheckinMonthDataResult(
        List<WxCheckinMonthDataVO> monthDataList,
        List<WxCheckinFetchFailure> failures
) {
    /**
     * 创建不含任何成功和失败数据的空结果。
     *
     * @return 考勤打卡月数据结果
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxCheckinMonthDataResult empty() {
        return new WxCheckinMonthDataResult(List.of(), List.of());
    }
}
