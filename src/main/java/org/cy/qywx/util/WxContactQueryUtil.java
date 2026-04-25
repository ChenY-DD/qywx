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
 * 企业微信通讯录查询工具，封装部门、成员及部门信息补全相关能力。
 */
public class WxContactQueryUtil {

    private final WxCpService wxCpService;

    public WxContactQueryUtil(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
    }

    /**
     * 查询指定部门下的部门列表。
     *
     * @param departmentId 部门 ID，为 null 时查询根部门下的部门
     * @return 部门列表
     */
    public List<WxDepartmentVO> getDepartments(Long departmentId) throws WxErrorException {
        List<WxCpDepart> departments = wxCpService.getDepartmentService().list(departmentId);
        return departments.stream()
                .map(WxContactConverter::from)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 查询企业全部部门。
     *
     * @return 部门列表
     */
    public List<WxDepartmentVO> getAllDepartments() throws WxErrorException {
        return getDepartments(null);
    }

    /**
     * 查询单个部门详情。
     *
     * @param departmentId 部门 ID
     * @return 部门详情
     */
    public WxDepartmentVO getDepartment(Long departmentId) throws WxErrorException {
        return WxContactConverter.from(wxCpService.getDepartmentService().get(departmentId));
    }

    /**
     * 按部门查询成员，并自动补全成员所属部门与主部门名称。
     *
     * @param departmentId 部门 ID
     * @param fetchChild 是否递归查询子部门
     * @param status 成员状态
     * @return 成员列表
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
     * 查询全部在职成员。
     *
     * @return 成员列表
     */
    public List<WxUserVO> getAllUsers() throws WxErrorException {
        return getUsersByDepartment(1L, true, 0);
    }

    /**
     * 按状态查询全部成员。
     *
     * @param status 成员状态
     * @return 成员列表
     */
    public List<WxUserVO> getAllUsers(Integer status) throws WxErrorException {
        return getUsersByDepartment(1L, true, status);
    }

    /**
     * 按部门查询简化成员信息，并自动补全成员所属部门与主部门名称。
     *
     * @param departmentId 部门 ID
     * @param fetchChild 是否递归查询子部门
     * @param status 成员状态
     * @return 成员列表
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
     * 查询全部在职成员的简化信息。
     *
     * @return 成员列表
     */
    public List<WxUserVO> getAllUsersSimple() throws WxErrorException {
        return getUsersSimpleByDepartment(1L, true, 0);
    }

    /**
     * 按状态查询全部成员的简化信息。
     *
     * @param status 成员状态
     * @return 成员列表
     */
    public List<WxUserVO> getAllUsersSimple(Integer status) throws WxErrorException {
        return getUsersSimpleByDepartment(1L, true, status);
    }

    /**
     * 根据用户 ID 查询成员详情，并补全主部门与所属部门信息。
     *
     * @param userId 成员 userId
     * @return 成员详情
     */
    public WxUserVO getUserById(String userId) throws WxErrorException {
        WxUserVO user = WxContactConverter.from(wxCpService.getUserService().getById(userId));
        if (user == null) {
            return null;
        }
        return fillDepartmentInfo(user, getAllDepartments());
    }

    private List<WxUserVO> fillDepartmentInfo(List<WxUserVO> users, List<WxDepartmentVO> departments) {
        Map<Long, WxDepartmentVO> departmentMap = toDepartmentMap(departments);
        users.forEach(user -> fillDepartmentInfo(user, departmentMap));
        return users;
    }

    private WxUserVO fillDepartmentInfo(WxUserVO user, List<WxDepartmentVO> departments) {
        fillDepartmentInfo(user, toDepartmentMap(departments));
        return user;
    }

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

    private WxUserVO.DepartmentRef toDepartmentRef(WxDepartmentVO department) {
        WxUserVO.DepartmentRef ref = new WxUserVO.DepartmentRef();
        ref.setId(department.getId());
        ref.setName(department.getName());
        return ref;
    }
}
