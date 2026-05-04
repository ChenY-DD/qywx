package org.cy.qywx.util;

import org.cy.qywx.vo.WxCheckinRecordVO;

import java.util.List;

/**
 * 打卡记录查询结果，包含成功列表与失败批次列表。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param records  成功获取的打卡记录
 * @param failures 失败的批次
 */
public record WxCheckinRecordResult(
        List<WxCheckinRecordVO> records,
        List<WxCheckinFetchFailure> failures
) {
    /**
     * 返回空结果。
     *
     * @return 空结果
     */
    public static WxCheckinRecordResult empty() {
        return new WxCheckinRecordResult(List.of(), List.of());
    }
}
