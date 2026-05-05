package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：审批模板业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxApprovalTemplateVO implements Serializable {

    /**
     * 字段说明：审批模板 ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String templateId;
    /**
     * 字段说明：模板名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String templateName;
}
