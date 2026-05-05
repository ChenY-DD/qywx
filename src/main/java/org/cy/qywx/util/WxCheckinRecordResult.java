package org.cy.qywx.util;

import org.cy.qywx.vo.WxCheckinRecordVO;

import java.util.List;

/**
 * 记录说明：考勤打卡记录结果。
 *
 * @param records 记录列表
 * @param failures 批量查询失败记录列表
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxCheckinRecordResult(
        List<WxCheckinRecordVO> records,
        List<WxCheckinFetchFailure> failures
) {
    /**
     * 创建不含任何成功和失败数据的空结果。
     *
     * @return 考勤打卡记录结果
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxCheckinRecordResult empty() {
        return new WxCheckinRecordResult(List.of(), List.of());
    }
}
