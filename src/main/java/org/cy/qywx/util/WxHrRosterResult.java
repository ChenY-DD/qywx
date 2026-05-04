package org.cy.qywx.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * 花名册批量查询结果。
 *
 * @param staffInfo 成功项，key 为 userId，value 为花名册原始 JSON
 * @param failures 失败项列表
 */
public record WxHrRosterResult(
        Map<String, JsonNode> staffInfo,
        List<WxHrRosterFetchFailure> failures
) {

    public static WxHrRosterResult empty() {
        return new WxHrRosterResult(Map.of(), List.of());
    }
}
