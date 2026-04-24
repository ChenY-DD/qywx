package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WxDepartmentVO implements Serializable {

    private Long id;
    private String name;
    private String enName;
    private Long parentId;
    private Long order;
    private List<String> departmentLeader;
}
