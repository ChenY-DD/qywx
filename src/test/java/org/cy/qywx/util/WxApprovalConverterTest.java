package org.cy.qywx.util;

import me.chanjar.weixin.cp.bean.oa.WxCpApprovalApplyData;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalComment;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalRecord;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalRecordDetail;
import me.chanjar.weixin.cp.bean.oa.WxCpOperator;
import me.chanjar.weixin.cp.bean.oa.WxCpSpStatus;
import me.chanjar.weixin.cp.bean.oa.applydata.ApplyDataContent;
import me.chanjar.weixin.cp.bean.oa.applydata.ContentTitle;
import me.chanjar.weixin.cp.bean.oa.applydata.ContentValue;
import org.cy.qywx.vo.WxApprovalDetailVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WxApprovalConverterTest {

    @Test
    void shouldFillClosedLoopFieldsForClosedApproval() {
        WxCpApprovalDetailResult detailResult = new WxCpApprovalDetailResult();
        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo("sp-1");
        detail.setSpName("请假");
        detail.setSpStatus(WxCpSpStatus.PASSED);
        detail.setTemplateId("temp-1");
        detail.setApplyTime(1_000L);

        WxCpApprovalRecord record = new WxCpApprovalRecord();
        WxCpApprovalRecordDetail recordDetail = new WxCpApprovalRecordDetail();
        recordDetail.setSpTime(3_600L);
        record.setDetails(List.of(recordDetail));
        detail.setSpRecords(new WxCpApprovalRecord[]{record});

        WxCpApprovalComment comment = new WxCpApprovalComment();
        comment.setCommentTime(4_000L);
        WxCpOperator operator = new WxCpOperator();
        operator.setUserId("u1");
        comment.setCommentUserInfo(operator);
        detail.setComments(List.of(comment));

        detailResult.setInfo(detail);

        WxApprovalDetailVO vo = WxApprovalConverter.from(detailResult, 200_000L);

        assertTrue(Boolean.TRUE.equals(vo.getClosed()));
        assertEquals(4_000L, vo.getCloseTime());
        assertEquals(3_000L, vo.getCloseLoopDurationSeconds());
        assertEquals(199_000L, vo.getCurrentDurationSeconds());
        assertFalse(Boolean.TRUE.equals(vo.getCreatedToday()));
        assertTrue(Boolean.TRUE.equals(vo.getOverdueOneDay()));
        assertEquals("sp-1", vo.getSpNo());
    }

    @Test
    void shouldFillRunningFieldsForAuditingApproval() {
        WxCpApprovalDetailResult detailResult = new WxCpApprovalDetailResult();
        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo("sp-2");
        detail.setSpName("报销");
        detail.setSpStatus(WxCpSpStatus.AUDITING);
        detail.setTemplateId("temp-2");
        detail.setApplyTime(10L);
        detailResult.setInfo(detail);

        WxApprovalDetailVO vo = WxApprovalConverter.from(detailResult, 100_000L);

        assertFalse(Boolean.TRUE.equals(vo.getClosed()));
        assertEquals(null, vo.getCloseTime());
        assertEquals(null, vo.getCloseLoopDurationSeconds());
        assertEquals(99_990L, vo.getCurrentDurationSeconds());
        assertTrue(Boolean.TRUE.equals(vo.getOverdueOneDay()));
    }

    @Test
    void shouldResolveAttendanceDurationValue() {
        ContentValue.Attendance.DataRange range = new ContentValue.Attendance.DataRange();
        range.setBegin(1_700_000_000L);
        range.setEnd(1_700_003_600L);
        range.setDuration(3_600L);
        ContentValue.Attendance attendance = new ContentValue.Attendance();
        attendance.setType(1);
        attendance.setDateRange(range);

        String value = resolveSingleFormValue("Attendance", new ContentValue().setAttendance(attendance));

        assertNotNull(value, "假勤控件（出差/外出/加班）的值不应为空");
        assertTrue(value.contains("3600"), "应包含时长秒数，实际: " + value);
    }

    @Test
    void shouldResolveVacationValue() {
        ContentTitle vacationType = new ContentTitle();
        vacationType.setText("年假");
        ContentValue.Selector.Option option = new ContentValue.Selector.Option();
        option.setValues(List.of(vacationType));
        ContentValue.Selector selector = new ContentValue.Selector();
        selector.setOptions(List.of(option));

        ContentValue.Attendance.DataRange range = new ContentValue.Attendance.DataRange();
        range.setDuration(28_800L);
        ContentValue.Attendance attendance = new ContentValue.Attendance();
        attendance.setDateRange(range);

        ContentValue.Vacation vacation = new ContentValue.Vacation();
        vacation.setSelector(selector);
        vacation.setAttendance(attendance);

        String value = resolveSingleFormValue("Vacation", new ContentValue().setVacation(vacation));

        assertNotNull(value, "请假控件的值不应为空");
        assertTrue(value.contains("年假"), "应包含假期类型，实际: " + value);
    }

    @Test
    void shouldResolveDateRangeValue() {
        ContentValue.Attendance.DataRange range = new ContentValue.Attendance.DataRange();
        range.setBegin(1_700_000_000L);
        range.setEnd(1_700_086_400L);
        range.setDuration(86_400L);

        String value = resolveSingleFormValue("DateRange", new ContentValue().setDateRange(range));

        assertNotNull(value, "时长控件的值不应为空");
        assertTrue(value.contains("86400"), "应包含时长秒数，实际: " + value);
    }

    @Test
    void shouldResolveLocationValue() {
        ContentValue.Location location = new ContentValue.Location();
        location.setTitle("总部");
        location.setAddress("北京市朝阳区");
        location.setLatitude(new BigDecimal("39.9"));
        location.setLongitude(new BigDecimal("116.4"));

        String value = resolveSingleFormValue("Location", new ContentValue().setLocation(location));

        assertNotNull(value, "位置控件的值不应为空");
        assertTrue(value.contains("北京市朝阳区"), "应包含位置地址，实际: " + value);
    }

    @Test
    void shouldResolveFormulaValue() {
        ContentValue.Formula formula = new ContentValue.Formula();
        formula.setValue("12345.67");

        String value = resolveSingleFormValue("Formula", new ContentValue().setFormula(formula));

        assertEquals("12345.67", value, "公式控件应返回计算结果");
    }

    @Test
    void shouldResolveBankAccountValue() {
        ContentValue.BankAccount bankAccount = new ContentValue.BankAccount();
        bankAccount.setAccountName("张三");
        bankAccount.setAccountNumber("6222021234567890");

        String value = resolveSingleFormValue("BankAccount", new ContentValue().setBankAccount(bankAccount));

        assertNotNull(value, "银行账户控件的值不应为空");
        assertTrue(value.contains("6222021234567890"), "应包含银行账号，实际: " + value);
    }

    @Test
    void shouldResolvePunchCorrectionValue() {
        ContentValue.PunchCorrection punchCorrection = new ContentValue.PunchCorrection();
        punchCorrection.setState("上班");
        punchCorrection.setTime(1_700_000_000L);

        String value = resolveSingleFormValue("PunchCorrection", new ContentValue().setPunchCorrection(punchCorrection));

        assertNotNull(value, "补卡控件的值不应为空");
        assertTrue(value.contains("上班"), "应包含补卡状态，实际: " + value);
    }

    @Test
    void shouldResolveRelatedApprovalValue() {
        ContentValue.TemplateName templateName = new ContentValue.TemplateName();
        templateName.setText("请假申请");
        ContentValue.RelatedApproval relatedApproval = new ContentValue.RelatedApproval();
        relatedApproval.setSpNo("202600010001");
        relatedApproval.setTemplateNames(List.of(templateName));

        String value = resolveSingleFormValue("RelatedApproval",
                new ContentValue().setRelatedApproval(List.of(relatedApproval)));

        assertNotNull(value, "关联审批单控件的值不应为空");
        assertTrue(value.contains("202600010001"), "应包含关联审批单号，实际: " + value);
    }

    @Test
    void shouldExpandTableChildrenValue() {
        ContentTitle childTitle = new ContentTitle();
        childTitle.setText("商品名称");
        ApplyDataContent childContent = new ApplyDataContent();
        childContent.setControl("Text");
        childContent.setTitles(List.of(childTitle));
        childContent.setValue(new ContentValue().setText("笔记本电脑"));

        ContentValue.Child child = new ContentValue.Child();
        child.setList(List.of(childContent));

        String value = resolveSingleFormValue("Table", new ContentValue().setChildren(List.of(child)));

        assertNotNull(value, "明细表控件的值不应为空");
        assertTrue(value.contains("商品名称"), "明细表应展开子控件标题，实际: " + value);
        assertTrue(value.contains("笔记本电脑"), "明细表应展开子控件的值，实际: " + value);
    }

    @Test
    void shouldMergeResignationContainerFromRawJson() {
        ContentTitle title = new ContentTitle();
        title.setText("离职");
        title.setLang("zh_CN");
        ApplyDataContent content = new ApplyDataContent();
        content.setControl("Resignation");
        content.setId("Resignation-1");
        content.setTitles(List.of(title));
        content.setValue(new ContentValue());

        WxCpApprovalApplyData applyData = new WxCpApprovalApplyData();
        applyData.setContents(List.of(content));
        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo("sp-resign");
        detail.setApplyTime(1L);
        detail.setApplyData(applyData);
        WxCpApprovalDetailResult result = new WxCpApprovalDetailResult();
        result.setInfo(detail);

        String rawJson = """
                {"errcode":0,"info":{"apply_data":{"contents":[
                  {"control":"Resignation","id":"Resignation-1","title":[{"text":"离职","lang":"zh_CN"}],
                   "value":{
                     "date":{"control":"Date","id":"d","title":[{"text":"Offboarding Date","lang":"en"},{"text":"离职日期","lang":"zh_CN"}],"value":{"date":{"type":"day","s_timestamp":"1782748800"}}},
                     "reason":{"control":"Textarea","id":"r","title":[{"text":"Reason","lang":"en"},{"text":"离职原因","lang":"zh_CN"}],"value":{"text":"个人发展规划"}},
                     "remark":{"control":"Textarea","id":"m","title":[{"text":"Remarks","lang":"en"},{"text":"离职备注","lang":"zh_CN"}],"value":{"text":"无"}},
                     "docs":[],"wedrive_files":[]
                   }}
                ]}}}
                """;

        WxApprovalDetailVO vo = WxApprovalConverter.from(result, rawJson);

        assertEquals(1, vo.getFormItems().size(), "离职容器仍应是一个表单项");
        String value = vo.getFormItems().get(0).getValue();
        assertNotNull(value, "离职容器控件的值不应为空");
        assertTrue(value.contains("离职原因"), "应包含离职原因标题，实际: " + value);
        assertTrue(value.contains("个人发展规划"), "应包含离职原因内容，实际: " + value);
        assertTrue(value.contains("离职日期"), "应包含离职日期标题，实际: " + value);
        assertTrue(value.contains("离职备注"), "应包含离职备注标题，实际: " + value);
        assertFalse(value.contains("Reason"), "应优先取中文标题而非英文，实际: " + value);
    }

    @Test
    void shouldPreferSimplifiedChineseTitle() {
        ContentTitle en = new ContentTitle();
        en.setText("Offboarding");
        en.setLang("en");
        ContentTitle zh = new ContentTitle();
        zh.setText("离职");
        zh.setLang("zh_CN");
        ApplyDataContent content = new ApplyDataContent();
        content.setControl("Text");
        content.setTitles(List.of(en, zh));
        content.setValue(new ContentValue().setText("x"));

        WxCpApprovalApplyData applyData = new WxCpApprovalApplyData();
        applyData.setContents(List.of(content));
        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo("sp-title");
        detail.setApplyTime(1L);
        detail.setApplyData(applyData);
        WxCpApprovalDetailResult result = new WxCpApprovalDetailResult();
        result.setInfo(detail);

        WxApprovalDetailVO vo = WxApprovalConverter.from(result, 100L);

        assertEquals("离职", vo.getFormItems().get(0).getTitle(), "标题应优先取简体中文而非英文");
    }

    @Test
    void shouldParseProcessListNodesIncludingCcAndHandler() {
        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo("202606010001");
        detail.setApplyTime(1L);
        WxCpApprovalDetailResult result = new WxCpApprovalDetailResult();
        result.setInfo(detail);

        String rawJson = """
                {"errcode":0,"info":{"sp_no":"202606010001","process_list":{"node_list":[
                  {"node_type":2,"sub_node_list":[{"userid":"LiBiJun","media_ids":[]},{"userid":"RenShiHangZhengBu","media_ids":[]},{"userid":"WengYuYang","media_ids":[]}]},
                  {"node_type":1,"sp_status":1,"apv_rel":3,"sub_node_list":[{"userid":"JiaoJun","speech":"","sp_yj":1,"sptime":0,"media_ids":[]},{"userid":"KongLuXing","speech":"","sp_yj":1,"sptime":0,"media_ids":[]}]},
                  {"node_type":1,"sp_status":1,"apv_rel":2,"sub_node_list":[{"userid":"LiBiJun","speech":"","sp_yj":1,"sptime":0,"media_ids":[]}]},
                  {"node_type":1,"sp_status":1,"apv_rel":2,"sub_node_list":[{"userid":"WengYuYang","speech":"","sp_yj":1,"sptime":0,"media_ids":[]}]},
                  {"node_type":1,"sp_status":1,"apv_rel":2,"sub_node_list":[{"userid":"Wengsir","speech":"","sp_yj":1,"sptime":0,"media_ids":[]}]},
                  {"node_type":3,"sp_status":1,"apv_rel":2,"sub_node_list":[{"userid":"HuYuanLing","speech":"","sp_yj":1,"sptime":0,"media_ids":[]},{"userid":"LiBiJun","speech":"","sp_yj":1,"sptime":0,"media_ids":[]},{"userid":"RenShiHangZhengBu","speech":"","sp_yj":1,"sptime":0,"media_ids":[]}]},
                  {"node_type":2,"sub_node_list":[{"userid":"HuYuanLing","media_ids":[]},{"userid":"LiBiJun","media_ids":[]},{"userid":"RenShiHangZhengBu","media_ids":[]},{"userid":"WuJiaYi","media_ids":[]}]}
                ]}}}
                """;

        WxApprovalDetailVO vo = WxApprovalConverter.from(result, rawJson);

        assertEquals(7, vo.getNodes().size(), "应解析出 process_list.node_list 的全部 7 个节点");

        WxApprovalDetailVO.Node cc = vo.getNodes().get(0);
        assertEquals(2, cc.getNodeType(), "第 1 个节点应为抄送（node_type=2）");
        assertEquals(3, cc.getDetails().size(), "第 1 个抄送节点应有 3 个抄送人");
        assertEquals("LiBiJun", cc.getDetails().get(0).getApproverUserId(), "应保留抄送人 userId");

        WxApprovalDetailVO.Node approval = vo.getNodes().get(1);
        assertEquals(1, approval.getNodeType(), "第 2 个节点应为审批（node_type=1）");
        assertEquals(3, approval.getApvRel(), "应保留审批方式 apv_rel");
        assertEquals(2, approval.getDetails().size(), "会签节点应有 2 个审批人");
        assertEquals(1, approval.getDetails().get(0).getSpYj(), "应保留子节点审批意见类型 sp_yj");

        WxApprovalDetailVO.Node handler = vo.getNodes().get(5);
        assertEquals(3, handler.getNodeType(), "第 6 个节点应为办理（node_type=3），sp_record 中缺失");
        assertTrue(handler.getDetails().stream().anyMatch(d -> "HuYuanLing".equals(d.getApproverUserId())),
                "第 6 个节点应包含 sp_record 漏掉的 HuYuanLing");
    }

    @Test
    void shouldMergeNestedResignationContainerFromRawJson() {
        ContentTitle title = new ContentTitle();
        title.setText("离职");
        title.setLang("zh_CN");
        ApplyDataContent content = new ApplyDataContent();
        content.setControl("Resignation");
        content.setId("Resignation-1652025600");
        content.setTitles(List.of(title));
        content.setValue(new ContentValue());

        WxCpApprovalApplyData applyData = new WxCpApprovalApplyData();
        applyData.setContents(List.of(content));
        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo("sp-resign-nested");
        detail.setApplyTime(1L);
        detail.setApplyData(applyData);
        WxCpApprovalDetailResult result = new WxCpApprovalDetailResult();
        result.setInfo(detail);

        // 真实离职数据：子控件包在 value.resignation.* 这一层之下（而非直接挂在 value 下）
        String rawJson = """
                {"errcode":0,"info":{"apply_data":{"contents":[
                  {"control":"Resignation","id":"Resignation-1652025600","title":[{"text":"离职","lang":"zh_CN"}],
                   "value":{"tips":[],"members":[],"departments":[],"resignation":{
                     "date":{"control":"Date","id":"Date-1","title":[{"text":"离职日期","lang":"zh_CN"}],"value":{"date":{"type":"day","s_timestamp":"1782748800"}}},
                     "reason":{"control":"Textarea","id":"Textarea-1","title":[{"text":"离职原因","lang":"zh_CN"}],"value":{"text":"个人发展规划"}},
                     "remark":{"control":"Textarea","id":"Textarea-2","title":[{"text":"离职备注","lang":"zh_CN"}],"value":{"text":"无"}}
                   },"docs":[],"wedrive_files":[]}}
                ]}}}
                """;

        WxApprovalDetailVO vo = WxApprovalConverter.from(result, rawJson);

        assertEquals(1, vo.getFormItems().size(), "离职容器仍应是一个表单项");
        String value = vo.getFormItems().get(0).getValue();
        assertNotNull(value, "嵌套离职容器（value.resignation.*）的值不应为空");
        assertTrue(value.contains("离职原因"), "应递归进入 resignation 嵌套层解析子控件，实际: " + value);
        assertTrue(value.contains("个人发展规划"), "应包含离职原因内容，实际: " + value);
        assertTrue(value.contains("离职日期"), "应包含离职日期标题，实际: " + value);
        assertTrue(value.contains("离职备注"), "应包含离职备注标题，实际: " + value);
    }

    /**
     * 构造仅包含单个表单控件的审批详情，返回该表单项解析后的值。
     */
    private String resolveSingleFormValue(String control, ContentValue value) {
        ContentTitle title = new ContentTitle();
        title.setText("测试字段");

        ApplyDataContent content = new ApplyDataContent();
        content.setControl(control);
        content.setTitles(List.of(title));
        content.setValue(value);

        WxCpApprovalApplyData applyData = new WxCpApprovalApplyData();
        applyData.setContents(List.of(content));

        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo("sp-form");
        detail.setApplyTime(1L);
        detail.setApplyData(applyData);

        WxCpApprovalDetailResult result = new WxCpApprovalDetailResult();
        result.setInfo(detail);

        WxApprovalDetailVO vo = WxApprovalConverter.from(result, 100L);
        assertNotNull(vo, "转换结果不应为空");
        assertEquals(1, vo.getFormItems().size(), "应有且仅有一个表单项");
        return vo.getFormItems().get(0).getValue();
    }
}
