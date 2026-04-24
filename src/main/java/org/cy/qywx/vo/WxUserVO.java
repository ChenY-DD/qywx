package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WxUserVO implements Serializable {

    @Data
    public static class DepartmentRef implements Serializable {
        private Long id;
        private String name;
    }

    private String userId;
    private String name;
    private List<Long> departIds;
    private List<DepartmentRef> departments;
    private String position;
    private List<String> positions;
    private String mobile;
    private String gender;
    private String email;
    private String bizMail;
    private String avatar;
    private String thumbAvatar;
    private String mainDepartment;
    private Long mainDepartmentId;
    private String mainDepartmentName;
    private Integer status;
    private Integer enable;
    private String alias;
    private String englishName;
    private String telephone;
    private String address;
    private String qrCode;
    private List<String> directLeader;
}
