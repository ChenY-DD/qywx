package org.cy.qywx.util.export;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：企业微信异步导出单个文件数据视图对象（仅映射加密文件下载链接信息）。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxExportDataVO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 字段说明：加密文件下载链接。 */
    private String url;

    /** 字段说明：文件字节大小。 */
    private Integer size;

    /** 字段说明：文件 MD5 摘要。 */
    private String md5;
}
