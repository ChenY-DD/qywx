package org.cy.qywx.util;

import org.cy.qywx.vo.WxCheckinDayDataVO;

import java.util.List;

/**
 * 记录说明：考勤打卡天数据结果。
 *
 * @param dayDataList 天数据list
 * @param failures 批量查询失败记录列表
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxCheckinDayDataResult(
        List<WxCheckinDayDataVO> dayDataList,
        List<WxCheckinFetchFailure> failures
) {
    /**
     * 创建不含任何成功和失败数据的空结果。
     *
     * @return 考勤打卡天数据结果
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxCheckinDayDataResult empty() {
        return new WxCheckinDayDataResult(List.of(), List.of());
    }
}
