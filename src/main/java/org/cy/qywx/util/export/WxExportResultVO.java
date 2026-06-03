package org.cy.qywx.util.export;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类说明：企业微信异步导出结果视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxExportResultVO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 字段说明：任务状态（1=处理中，2=完成，3=异常）。 */
    private Integer status;

    /** 字段说明：导出文件列表。 */
    private List<WxExportDataVO> dataList;
}
