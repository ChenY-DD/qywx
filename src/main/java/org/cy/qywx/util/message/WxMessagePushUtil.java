package org.cy.qywx.util.message;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import me.chanjar.weixin.cp.bean.message.WxCpMessageSendResult;
import org.cy.qywx.exception.QywxApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类说明：企业微信应用消息推送工具。
 *
 * @author cy
 * Copyright (c) CY
 */
public class WxMessagePushUtil {

    /**
     * 字段说明：日志。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final Logger log = LoggerFactory.getLogger(WxMessagePushUtil.class);

    /**
     * 字段说明：企业微信 service。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final WxCpService wxCpService;

    /**
     * 创建 企业微信应用消息推送工具实例。
     *
     * @param wxCpService 企业微信 service
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxMessagePushUtil(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
        log.info("WxMessagePushUtil initialized");
    }

    /**
     * 发送应用消息。
     *
     * @param message 企业微信消息对象
     * @return 消息推送结果业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxMessageSendResultVO send(WxCpMessage message) {
        try {
            WxCpMessageSendResult result = wxCpService.getMessageService().send(message);
            return WxMessageConverter.from(result);
        } catch (WxErrorException e) {
            log.error("send message failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 发送文本消息。
     *
     * @param toUser  接收成员的 userId，多个以竖线分隔
     * @param content 文本内容
     * @return 消息推送结果业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxMessageSendResultVO sendText(String toUser, String content) {
        return send(WxCpMessage.TEXT().toUser(toUser).content(content).build());
    }

    /**
     * 发送 Markdown 消息。
     *
     * @param toUser  接收成员的 userId，多个以竖线分隔
     * @param content Markdown 内容
     * @return 消息推送结果业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxMessageSendResultVO sendMarkdown(String toUser, String content) {
        return send(WxCpMessage.MARKDOWN().toUser(toUser).content(content).build());
    }

    /**
     * 发送文本卡片消息。
     *
     * @param toUser      接收成员的 userId，多个以竖线分隔
     * @param title       标题
     * @param description 描述
     * @param url         点击后跳转的链接
     * @param btnTxt      按钮文字
     * @return 消息推送结果业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxMessageSendResultVO sendTextCard(String toUser, String title, String description, String url, String btnTxt) {
        return send(WxCpMessage.TEXTCARD().toUser(toUser).title(title).description(description).url(url).btnTxt(btnTxt).build());
    }

    /**
     * 撤回应用消息。
     *
     * @param msgId 消息 ID
     *
     * @author cy
     * Copyright (c) CY
     */
    public void recall(String msgId) {
        try {
            wxCpService.getMessageService().recall(msgId);
        } catch (WxErrorException e) {
            log.error("recall message failed: msgId={}, error={}", msgId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }
}
