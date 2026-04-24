package org.cy.qywx.util;

import me.chanjar.weixin.cp.bean.WxCpDepart;
import me.chanjar.weixin.cp.bean.WxCpUser;
import org.cy.qywx.vo.WxDepartmentVO;
import org.cy.qywx.vo.WxUserVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WxContactConverterTest {

    @Test
    void shouldConvertDepartmentAndUser() {
        WxCpDepart depart = new WxCpDepart();
        depart.setId(100L);
        depart.setName("研发部");
        depart.setParentId(1L);
        depart.setDepartmentLeader(new String[]{"u1", "u2"});

        WxDepartmentVO departmentVO = WxContactConverter.from(depart);
        assertNotNull(departmentVO);
        assertEquals(100L, departmentVO.getId());
        assertEquals("研发部", departmentVO.getName());
        assertEquals(List.of("u1", "u2"), departmentVO.getDepartmentLeader());

        WxCpUser user = new WxCpUser();
        user.setUserId("zhangsan");
        user.setName("张三");
        user.setDepartIds(new Long[]{100L, 200L});
        user.setDirectLeader(new String[]{"leader1"});
        user.setMainDepartment("100");

        WxUserVO userVO = WxContactConverter.from(user);
        assertNotNull(userVO);
        assertEquals("zhangsan", userVO.getUserId());
        assertEquals(List.of(100L, 200L), userVO.getDepartIds());
        assertEquals(List.of("leader1"), userVO.getDirectLeader());
        assertEquals(100L, userVO.getMainDepartmentId());
    }
}
