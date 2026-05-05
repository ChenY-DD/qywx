package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类说明：成员业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxUserVO implements Serializable {

    /**
     * 类说明：部门ref。
     *
     * @author cy
     * Copyright (c) CY
     */
    @Data
    public static class DepartmentRef implements Serializable {
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
    }

    /**
     * 字段说明：成员 userId。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String userId;
    /**
     * 字段说明：名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String name;
    /**
     * 字段说明：部门ID列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<Long> departIds;
    /**
     * 字段说明：部门列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<DepartmentRef> departments;
    /**
     * 字段说明：职务。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String position;
    /**
     * 字段说明：职务列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<String> positions;
    /**
     * 字段说明：手机号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String mobile;
    /**
     * 字段说明：性别。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String gender;
    /**
     * 字段说明：邮箱。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String email;
    /**
     * 字段说明：biz邮箱。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String bizMail;
    /**
     * 字段说明：头像。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String avatar;
    /**
     * 字段说明：缩略头像。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String thumbAvatar;
    /**
     * 字段说明：主部门。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String mainDepartment;
    /**
     * 字段说明：主部门ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long mainDepartmentId;
    /**
     * 字段说明：主部门名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String mainDepartmentName;
    /**
     * 字段说明：状态。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer status;
    /**
     * 字段说明：启用状态。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer enable;
    /**
     * 字段说明：别名。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String alias;
    /**
     * 字段说明：英文名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String englishName;
    /**
     * 字段说明：座机。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String telephone;
    /**
     * 字段说明：地址。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String address;
    /**
     * 字段说明：二维码编码。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String qrCode;
    /**
     * 字段说明：直属负责人。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<String> directLeader;
}
