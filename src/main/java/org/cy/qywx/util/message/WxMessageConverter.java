package org.cy.qywx.util.message;

import me.chanjar.weixin.cp.bean.message.WxCpMessageSendResult;

/**
 * 类说明：消息推送结果转换工具。
 *
 * @author cy
 * Copyright (c) CY
 */
public final class WxMessageConverter {

    /**
     * 创建 消息推送结果转换工具实例。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxMessageConverter() {
    }

    /**
     * 将企业微信消息发送结果转换为业务视图对象。
     *
     * @param result 企业微信消息发送结果
     * @return 消息推送结果业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxMessageSendResultVO from(WxCpMessageSendResult result) {
        if (result == null) {
            return null;
        }
        WxMessageSendResultVO vo = new WxMessageSendResultVO();
        vo.setMsgId(result.getMsgId());
        vo.setResponseCode(result.getResponseCode());
        vo.setInvalidUser(result.getInvalidUser());
        vo.setInvalidParty(result.getInvalidParty());
        vo.setInvalidTag(result.getInvalidTag());
        vo.setUnlicensedUser(result.getUnlicensedUser());
        return vo;
    }
}
