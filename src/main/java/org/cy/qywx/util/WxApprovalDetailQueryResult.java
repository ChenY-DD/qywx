package org.cy.qywx.util;

import org.cy.qywx.vo.WxApprovalDetailVO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记录说明：审批详情查询结果。
 *
 * @param details 详情列表
 * @param failures 批量查询失败记录列表
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxApprovalDetailQueryResult(
        List<WxApprovalDetailVO> details,
        List<WxApprovalDetailFetchFailure> failures
) {
    /**
     * 创建不含任何成功和失败数据的空结果。
     *
     * @return 审批详情查询结果
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxApprovalDetailQueryResult empty() {
        return new WxApprovalDetailQueryResult(List.of(), List.of());
    }

    /**
     * 执行 groupByTemplateId 相关逻辑。
     *
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    public Map<String, List<WxApprovalDetailVO>> groupByTemplateId() {
        return details.stream()
                .collect(Collectors.groupingBy(
                        detail -> detail.getTemplateId() == null ? "UNKNOWN_TEMPLATE" : detail.getTemplateId()
                ));
    }
}
