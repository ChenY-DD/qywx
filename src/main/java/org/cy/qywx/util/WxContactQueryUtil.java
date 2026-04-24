package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpDepart;
import me.chanjar.weixin.cp.bean.WxCpUser;
import org.cy.qywx.vo.WxDepartmentVO;
import org.cy.qywx.vo.WxUserVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class WxContactQueryUtil {

    private final WxCpService wxCpService;

    public WxContactQueryUtil(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
    }

    public List<WxDepartmentVO> getDepartments(Long departmentId) throws WxErrorException {
        List<WxCpDepart> departments = wxCpService.getDepartmentService().list(departmentId);
        return departments.stream()
                .map(WxContactConverter::from)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<WxDepartmentVO> getAllDepartments() throws WxErrorException {
        return getDepartments(null);
    }

    public WxDepartmentVO getDepartment(Long departmentId) throws WxErrorException {
        return WxContactConverter.from(wxCpService.getDepartmentService().get(departmentId));
    }

    public List<WxUserVO> getUsersByDepartment(Long departmentId, boolean fetchChild, Integer status) throws WxErrorException {
        List<WxCpUser> users = wxCpService.getUserService().listByDepartment(departmentId, fetchChild, status);
        List<WxUserVO> userVos = users.stream()
                .map(WxContactConverter::from)
                .filter(Objects::nonNull)
                .toList();
        return fillDepartmentInfo(userVos, getAllDepartments());
    }

    public List<WxUserVO> getAllUsers() throws WxErrorException {
        return getUsersByDepartment(1L, true, 0);
    }

    public List<WxUserVO> getAllUsers(Integer status) throws WxErrorException {
        return getUsersByDepartment(1L, true, status);
    }

    public List<WxUserVO> getUsersSimpleByDepartment(Long departmentId, boolean fetchChild, Integer status) throws WxErrorException {
        List<WxCpUser> users = wxCpService.getUserService().listSimpleByDepartment(departmentId, fetchChild, status);
        List<WxUserVO> userVos = users.stream()
                .map(WxContactConverter::from)
                .filter(Objects::nonNull)
                .toList();
        return fillDepartmentInfo(userVos, getAllDepartments());
    }

    public List<WxUserVO> getAllUsersSimple() throws WxErrorException {
        return getUsersSimpleByDepartment(1L, true, 0);
    }

    public List<WxUserVO> getAllUsersSimple(Integer status) throws WxErrorException {
        return getUsersSimpleByDepartment(1L, true, status);
    }

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
