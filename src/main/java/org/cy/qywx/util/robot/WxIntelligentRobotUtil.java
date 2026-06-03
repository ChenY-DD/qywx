package org.cy.qywx.util.robot;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotChatRequest;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotCreateRequest;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotSendMessageRequest;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotUpdateRequest;
import org.cy.qywx.exception.QywxApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类说明：企业微信智能机器人工具（增删改查 + 对话 / 发送消息 / 重置会话）。
 *
 * @author cy
 * Copyright (c) CY
 */
public class WxIntelligentRobotUtil {

    /**
     * 字段说明：日志。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final Logger log = LoggerFactory.getLogger(WxIntelligentRobotUtil.class);

    /**
     * 字段说明：企业微信 service。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final WxCpService wxCpService;

    /**
     * 创建 企业微信智能机器人工具实例。
     *
     * @param wxCpService 企业微信 service
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxIntelligentRobotUtil(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
        log.info("WxIntelligentRobotUtil initialized");
    }

    /**
     * 创建智能机器人。
     *
     * @param req 创建请求
     * @return 新建机器人 ID
     *
     * @author cy
     * Copyright (c) CY
     */
    public String createRobot(WxCpIntelligentRobotCreateRequest req) {
        try {
            return wxCpService.getIntelligentRobotService().createRobot(req).getRobotId();
        } catch (WxErrorException e) {
            log.error("createRobot failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 更新智能机器人。
     *
     * @param req 更新请求
     *
     * @author cy
     * Copyright (c) CY
     */
    public void updateRobot(WxCpIntelligentRobotUpdateRequest req) {
        try {
            wxCpService.getIntelligentRobotService().updateRobot(req);
        } catch (WxErrorException e) {
            log.error("updateRobot failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 删除智能机器人。
     *
     * @param robotId 机器人 ID
     *
     * @author cy
     * Copyright (c) CY
     */
    public void deleteRobot(String robotId) {
        try {
            wxCpService.getIntelligentRobotService().deleteRobot(robotId);
        } catch (WxErrorException e) {
            log.error("deleteRobot failed: robotId={}, error={}", robotId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 获取智能机器人详情。
     *
     * @param robotId 机器人 ID
     * @return 智能机器人业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxRobotVO getRobot(String robotId) {
        try {
            return WxRobotConverter.from(wxCpService.getIntelligentRobotService().getRobot(robotId));
        } catch (WxErrorException e) {
            log.error("getRobot failed: robotId={}, error={}", robotId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 与智能机器人对话。
     *
     * @param req 对话请求
     * @return 智能机器人对话结果业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxRobotChatVO chat(WxCpIntelligentRobotChatRequest req) {
        try {
            return WxRobotConverter.from(wxCpService.getIntelligentRobotService().chat(req));
        } catch (WxErrorException e) {
            log.error("robot chat failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 通过智能机器人发送消息。
     *
     * @param req 发送消息请求
     * @return 智能机器人消息发送结果业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxRobotSendResultVO sendMessage(WxCpIntelligentRobotSendMessageRequest req) {
        try {
            return WxRobotConverter.from(wxCpService.getIntelligentRobotService().sendMessage(req));
        } catch (WxErrorException e) {
            log.error("robot sendMessage failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 重置智能机器人会话。
     *
     * @param robotId  机器人 ID
     * @param chatType 会话类型
     * @param chatId   会话 ID
     *
     * @author cy
     * Copyright (c) CY
     */
    public void resetSession(String robotId, String chatType, String chatId) {
        try {
            wxCpService.getIntelligentRobotService().resetSession(robotId, chatType, chatId);
        } catch (WxErrorException e) {
            log.error("robot resetSession failed: robotId={}, error={}", robotId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }
}
