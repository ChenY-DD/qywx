package org.cy.qywx.util;

import me.chanjar.weixin.cp.api.WxCpOaService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class WxCheckinQueryUtilTest {

    WxCpService cpService;
    WxCpOaService oaService;
    WxCheckinQueryUtil util;

    @BeforeEach
    void setUp() {
        cpService = Mockito.mock(WxCpService.class);
        oaService = Mockito.mock(WxCpOaService.class);
        when(cpService.getOaService()).thenReturn(oaService);
        util = new WxCheckinQueryUtil(
                cpService,
                Executors.newFixedThreadPool(2),
                new WxCheckinQueryOptions(30, 100, 3, 1L, 0D));
    }

    @Test
    void getCheckinGroups_returnsConvertedList() throws Exception {
        WxCpCropCheckinOption option = new WxCpCropCheckinOption();
        option.setGroupId(7L);
        option.setGroupName("HQ");
        when(oaService.getCropCheckinOption()).thenReturn(List.of(option));

        List<WxCheckinGroupVO> result = util.getCheckinGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGroupId()).isEqualTo(7);
        assertThat(result.get(0).getGroupName()).isEqualTo("HQ");
    }

    @Test
    void getCheckinGroups_emptyResponseReturnsEmpty() throws Exception {
        when(oaService.getCropCheckinOption()).thenReturn(List.of());

        assertThat(util.getCheckinGroups()).isEmpty();
    }
}
