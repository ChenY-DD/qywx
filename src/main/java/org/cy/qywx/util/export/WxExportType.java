package org.cy.qywx.util.export;

/**
 * 枚举说明：企业微信异步导出任务类型。
 *
 * @author cy
 * Copyright (c) CY
 */
public enum WxExportType {

    /** 导出成员（简易信息）。 */
    SIMPLE_USER,

    /** 导出成员（详细信息）。 */
    USER,

    /** 导出部门。 */
    DEPARTMENT,

    /** 导出标签成员。 */
    TAG_USER
}
