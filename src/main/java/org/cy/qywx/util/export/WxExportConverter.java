package org.cy.qywx.util.export;

import me.chanjar.weixin.cp.bean.export.WxCpExportResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类说明：企业微信异步导出结果转换器（WxJava bean → 本地 VO）。
 *
 * @author cy
 * Copyright (c) CY
 */
public final class WxExportConverter {

    /**
     * 私有构造，禁止实例化工具类。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxExportConverter() {
    }

    /**
     * 将 WxJava 导出结果转换为本地视图对象。
     *
     * @param result WxJava 导出结果
     * @return 导出结果视图对象，入参为 null 时返回 null
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxExportResultVO from(WxCpExportResult result) {
        if (result == null) {
            return null;
        }
        WxExportResultVO vo = new WxExportResultVO();
        vo.setStatus(result.getStatus());
        List<WxCpExportResult.ExportData> data = result.getDataList();
        vo.setDataList(data == null ? Collections.emptyList()
                : data.stream().map(WxExportConverter::fromData).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 将单个导出文件数据转换为本地视图对象。
     *
     * @param data WxJava 导出文件数据
     * @return 导出文件数据视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    private static WxExportDataVO fromData(WxCpExportResult.ExportData data) {
        WxExportDataVO vo = new WxExportDataVO();
        vo.setUrl(data.getUrl());
        vo.setSize(data.getSize());
        vo.setMd5(data.getMd5());
        return vo;
    }
}
