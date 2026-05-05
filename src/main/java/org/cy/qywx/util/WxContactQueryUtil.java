package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpDepart;
import me.chanjar.weixin.cp.bean.WxCpUser;
import org.cy.qywx.vo.WxDepartmentVO;
import org.cy.qywx.vo.WxUserVO;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 类说明：contact查询util工具。
 *
 * @author cy
 * Copyright (c) CY
 */
public class WxContactQueryUtil {

    /**
     * 字段说明：企业微信企业微信service。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final WxCpService wxCpService;

    /**
     * 创建 contact查询util工具实例。
     *
     * @param wxCpService 企业微信企业微信service
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxContactQueryUtil(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
    }

    /**
     * 获取部门列表。
     *
     * @param departmentId 部门ID
     * @return 部门列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxDepartmentVO> getDepartments(Long departmentId) throws WxErrorException {
        List<WxCpDepart> departments = wxCpService.getDepartmentService().list(departmentId);
        return departments.stream()
                .map(WxContactConverter::from)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 获取全部部门列表。
     *
     * @return 全部部门列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxDepartmentVO> getAllDepartments() throws WxErrorException {
        return getDepartments(null);
    }

    /**
     * 获取部门。
     *
     * @param departmentId 部门ID
     * @return 部门
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxDepartmentVO getDepartment(Long departmentId) throws WxErrorException {
        return WxContactConverter.from(wxCpService.getDepartmentService().get(departmentId));
    }

    /**
     * 获取成员列表by部门。
     *
     * @param departmentId 部门ID
     * @param fetchChild 是否包含子部门成员
     * @param status 状态
     * @return 成员列表by部门
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxUserVO> getUsersByDepartment(Long departmentId, boolean fetchChild, Integer status) throws WxErrorException {
        List<WxCpUser> users = wxCpService.getUserService().listByDepartment(departmentId, fetchChild, status);
        List<WxUserVO> userVos = users.stream()
                .map(WxContactConverter::from)
                .filter(Objects::nonNull)
                .toList();
        return fillDepartmentInfo(userVos, getAllDepartments());
    }

    /**
     * 获取全部成员列表。
     *
     * @return 全部成员列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxUserVO> getAllUsers() throws WxErrorException {
        return getUsersByDepartment(1L, true, 0);
    }

    /**
     * 获取全部成员列表。
     *
     * @param status 状态
     * @return 全部成员列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxUserVO> getAllUsers(Integer status) throws WxErrorException {
        return getUsersByDepartment(1L, true, status);
    }

    /**
     * 获取成员列表simpleby部门。
     *
     * @param departmentId 部门ID
     * @param fetchChild 是否包含子部门成员
     * @param status 状态
     * @return 成员列表simpleby部门
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxUserVO> getUsersSimpleByDepartment(Long departmentId, boolean fetchChild, Integer status) throws WxErrorException {
        List<WxCpUser> users = wxCpService.getUserService().listSimpleByDepartment(departmentId, fetchChild, status);
        List<WxUserVO> userVos = users.stream()
                .map(WxContactConverter::from)
                .filter(Objects::nonNull)
                .toList();
        return fillDepartmentInfo(userVos, getAllDepartments());
    }

    /**
     * 获取全部成员列表simple。
     *
     * @return 全部成员列表simple
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxUserVO> getAllUsersSimple() throws WxErrorException {
        return getUsersSimpleByDepartment(1L, true, 0);
    }

    /**
     * 获取全部成员列表simple。
     *
     * @param status 状态
     * @return 全部成员列表simple
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxUserVO> getAllUsersSimple(Integer status) throws WxErrorException {
        return getUsersSimpleByDepartment(1L, true, status);
    }

    /**
     * 获取成员byID。
     *
     * @param userId 成员 userId
     * @return 成员byID
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxUserVO getUserById(String userId) throws WxErrorException {
        WxUserVO user = WxContactConverter.from(wxCpService.getUserService().getById(userId));
        if (user == null) {
            return null;
        }
        return fillDepartmentInfo(user, getAllDepartments());
    }

    /**
     * 执行 fillDepartmentInfo 相关逻辑。
     *
     * @param users 成员列表
     * @param departments 部门列表
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxUserVO> fillDepartmentInfo(List<WxUserVO> users, List<WxDepartmentVO> departments) {
        Map<Long, WxDepartmentVO> departmentMap = toDepartmentMap(departments);
        users.forEach(user -> fillDepartmentInfo(user, departmentMap));
        return users;
    }

    /**
     * 执行 fillDepartmentInfo 相关逻辑。
     *
     * @param user 成员
     * @param departments 部门列表
     * @return 成员业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxUserVO fillDepartmentInfo(WxUserVO user, List<WxDepartmentVO> departments) {
        fillDepartmentInfo(user, toDepartmentMap(departments));
        return user;
    }

    /**
     * 执行 fillDepartmentInfo 相关逻辑。
     *
     * @param user 成员
     * @param departmentMap 部门map
     *
     * @author cy
     * Copyright (c) CY
     */
    private void fillDepartmentInfo(WxUserVO user, Map<Long, WxDepartmentVO> departmentMap) {
        if (user == null) {
            return;
        }

        List<WxUserVO.DepartmentRef> departmentRefs = user.getDepartIds() == null
                ? Collections.emptyList()
                : user.getDepartIds().stream()
                .map(departmentMap::get)
                .filter(Objects::nonNull)
                .map(this::toDepartmentRef)
                .collect(Collectors.toList());

        user.setDepartments(departmentRefs);

        Long mainDepartmentId = user.getMainDepartmentId();
        if (mainDepartmentId == null && user.getMainDepartment() != null && !user.getMainDepartment().isBlank()) {
            try {
                mainDepartmentId = Long.parseLong(user.getMainDepartment());
                user.setMainDepartmentId(mainDepartmentId);
            } catch (NumberFormatException ignored) {
            }
        }

        if (mainDepartmentId != null) {
            WxDepartmentVO mainDepartment = departmentMap.get(mainDepartmentId);
            if (mainDepartment != null) {
                user.setMainDepartmentName(mainDepartment.getName());
            }
        }
    }

    /**
     * 转换为部门map。
     *
     * @param departments 部门列表
     * @return 部门vo>
     *
     * @author cy
     * Copyright (c) CY
     */
    private Map<Long, WxDepartmentVO> toDepartmentMap(List<WxDepartmentVO> departments) {
        if (departments == null || departments.isEmpty()) {
            return Collections.emptyMap();
        }
        return departments.stream()
                .filter(Objects::nonNull)
                .filter(department -> department.getId() != null)
                .collect(Collectors.toMap(
                        WxDepartmentVO::getId,
                        department -> department,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    /**
     * 转换为部门ref。
     *
     * @param department 部门
     * @return 成员vo.departmentref
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxUserVO.DepartmentRef toDepartmentRef(WxDepartmentVO department) {
        WxUserVO.DepartmentRef ref = new WxUserVO.DepartmentRef();
        ref.setId(department.getId());
        ref.setName(department.getName());
        return ref;
    }
}
