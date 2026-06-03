package org.cy.qywx.util.robot;

import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobot;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotChatResponse;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotSendMessageResponse;

/**
 * 类说明：智能机器人结果转换工具。
 *
 * @author cy
 * Copyright (c) CY
 */
public final class WxRobotConverter {

    /**
     * 创建 智能机器人结果转换工具实例。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxRobotConverter() {
    }

    /**
     * 将企业微信智能机器人对象转换为业务视图对象。
     *
     * @param robot 企业微信智能机器人对象
     * @return 智能机器人业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxRobotVO from(WxCpIntelligentRobot robot) {
        if (robot == null) {
            return null;
        }
        WxRobotVO vo = new WxRobotVO();
        vo.setRobotId(robot.getRobotId());
        vo.setName(robot.getName());
        vo.setDescription(robot.getDescription());
        vo.setAvatar(robot.getAvatar());
        vo.setStatus(robot.getStatus());
        vo.setCreateTime(robot.getCreateTime());
        vo.setUpdateTime(robot.getUpdateTime());
        return vo;
    }

    /**
     * 将企业微信智能机器人对话响应转换为业务视图对象。
     *
     * @param resp 企业微信智能机器人对话响应
     * @return 智能机器人对话结果业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxRobotChatVO from(WxCpIntelligentRobotChatResponse resp) {
        if (resp == null) {
            return null;
        }
        WxRobotChatVO vo = new WxRobotChatVO();
        vo.setReply(resp.getReply());
        vo.setSessionId(resp.getSessionId());
        vo.setMsgId(resp.getMsgId());
        return vo;
    }

    /**
     * 将企业微信智能机器人消息发送响应转换为业务视图对象。
     *
     * @param resp 企业微信智能机器人消息发送响应
     * @return 智能机器人消息发送结果业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxRobotSendResultVO from(WxCpIntelligentRobotSendMessageResponse resp) {
        if (resp == null) {
            return null;
        }
        WxRobotSendResultVO vo = new WxRobotSendResultVO();
        vo.setMsgId(resp.getMsgId());
        vo.setSessionId(resp.getSessionId());
        return vo;
    }
}
