package org.cy.qywx.util;

import me.chanjar.weixin.cp.bean.WxCpDepart;
import me.chanjar.weixin.cp.bean.WxCpUser;
import org.cy.qywx.vo.WxDepartmentVO;
import org.cy.qywx.vo.WxUserVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class WxContactConverter {

    private WxContactConverter() {
    }

    public static WxDepartmentVO from(WxCpDepart depart) {
        if (depart == null) {
            return null;
        }

        WxDepartmentVO vo = new WxDepartmentVO();
        vo.setId(depart.getId());
        vo.setName(depart.getName());
        vo.setEnName(depart.getEnName());
        vo.setParentId(depart.getParentId());
        vo.setOrder(depart.getOrder());
        vo.setDepartmentLeader(toStringList(depart.getDepartmentLeader()));
        return vo;
    }

    public static WxUserVO from(WxCpUser user) {
        if (user == null) {
            return null;
        }

        WxUserVO vo = new WxUserVO();
        vo.setUserId(user.getUserId());
        vo.setName(user.getName());
        vo.setDepartIds(toLongList(user.getDepartIds()));
        vo.setPosition(user.getPosition());
        vo.setPositions(toStringList(user.getPositions()));
        vo.setMobile(user.getMobile());
        vo.setGender(user.getGender() == null ? null : user.getGender().name());
        vo.setEmail(user.getEmail());
        vo.setBizMail(user.getBizMail());
        vo.setAvatar(user.getAvatar());
        vo.setThumbAvatar(user.getThumbAvatar());
        vo.setMainDepartment(user.getMainDepartment());
        vo.setMainDepartmentId(parseLong(user.getMainDepartment()));
        vo.setStatus(user.getStatus());
        vo.setEnable(user.getEnable());
        vo.setAlias(user.getAlias());
        vo.setEnglishName(user.getEnglishName());
        vo.setTelephone(user.getTelephone());
        vo.setAddress(user.getAddress());
        vo.setQrCode(user.getQrCode());
        vo.setDirectLeader(toStringList(user.getDirectLeader()));
        return vo;
    }

    private static List<String> toStringList(String[] array) {
        return array == null || array.length == 0 ? Collections.emptyList() : Arrays.asList(array);
    }

    private static List<Long> toLongList(Long[] array) {
        return array == null || array.length == 0 ? Collections.emptyList() : Arrays.asList(array);
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
