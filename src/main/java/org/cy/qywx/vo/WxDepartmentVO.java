package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类说明：部门业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxDepartmentVO implements Serializable {

    /**
     * 字段说明：ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long id;
    /**
     * 字段说明：名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String name;
    /**
     * 字段说明：en名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String enName;
    /**
     * 字段说明：父级ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long parentId;
    /**
     * 字段说明：排序。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long order;
    /**
     * 字段说明：部门负责人。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<String> departmentLeader;
}
