package org.cy.qywx.util;

import org.cy.qywx.vo.WxCheckinMonthDataVO;

import java.util.List;

/**
 * 打卡月报查询结果，包含成功列表与失败批次列表。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param monthDataList 成功获取的月报数据
 * @param failures      失败的批次
 */
public record WxCheckinMonthDataResult(
        List<WxCheckinMonthDataVO> monthDataList,
        List<WxCheckinFetchFailure> failures
) {
    /**
     * 返回空结果。
     *
     * @return 空结果
     */
    public static WxCheckinMonthDataResult empty() {
        return new WxCheckinMonthDataResult(List.of(), List.of());
    }
}
