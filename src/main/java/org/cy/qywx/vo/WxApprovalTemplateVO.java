package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批模板简要信息。
 */
@Data
public class WxApprovalTemplateVO implements Serializable {

    private String templateId;
    private String templateName;
}
