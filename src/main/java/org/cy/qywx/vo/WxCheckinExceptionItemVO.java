package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 业务语义方法（{@code getLatePersons} 等）返回的扁平异常行 VO。
 * <p>每行表示「某 userId 在某天发生了某种异常」，多次迟到的人会按日期粒度出现多行。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinExceptionItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 用户姓名（来自日报 base_info.name） */
    private String name;

    /** 用户所属部门（按分号拆分后的列表） */
    private List<String> departments;

    /** 异常发生的日期 */
    private Date date;

    /** 异常类型 */
    private WxCheckinExceptionTypeEnum exceptionType;

    /** 当日该类型的异常次数 */
    private Integer exceptionCount;

    /** 当日该类型的异常时长（分钟，由 WeCom 秒数 / 60 向下取整） */
    private Long exceptionDurationMinutes;
}
