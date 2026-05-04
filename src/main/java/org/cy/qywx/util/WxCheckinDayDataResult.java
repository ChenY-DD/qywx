package org.cy.qywx.util;

import org.cy.qywx.vo.WxCheckinDayDataVO;

import java.util.List;

/**
 * 打卡日报查询结果，包含成功列表与失败批次列表。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param dayDataList 成功获取的日报数据
 * @param failures    失败的批次
 */
public record WxCheckinDayDataResult(
        List<WxCheckinDayDataVO> dayDataList,
        List<WxCheckinFetchFailure> failures
) {
    /**
     * 返回空结果。
     *
     * @return 空结果
     */
    public static WxCheckinDayDataResult empty() {
        return new WxCheckinDayDataResult(List.of(), List.of());
    }
}
