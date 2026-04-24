package org.cy.qywx.util;

import org.cy.qywx.vo.WxApprovalDetailVO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record WxApprovalDetailQueryResult(
        List<WxApprovalDetailVO> details,
        List<WxApprovalDetailFetchFailure> failures
) {
    public static WxApprovalDetailQueryResult empty() {
        return new WxApprovalDetailQueryResult(List.of(), List.of());
    }

    public Map<String, List<WxApprovalDetailVO>> groupByTemplateId() {
        return details.stream()
                .collect(Collectors.groupingBy(
                        detail -> detail.getTemplateId() == null ? "UNKNOWN_TEMPLATE" : detail.getTemplateId()
                ));
    }
}
