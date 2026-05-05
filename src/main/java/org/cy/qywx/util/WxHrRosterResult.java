package org.cy.qywx.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * 记录说明：HR花名册结果。
 *
 * @param staffInfo 员工信息
 * @param failures 批量查询失败记录列表
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxHrRosterResult(
        Map<String, JsonNode> staffInfo,
        List<WxHrRosterFetchFailure> failures
) {

    /**
     * 创建不含任何成功和失败数据的空结果。
     *
     * @return HR花名册结果
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxHrRosterResult empty() {
        return new WxHrRosterResult(Map.of(), List.of());
    }
}
