# 企业微信消息/网页授权/智能机器人/异步导出 封装 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `util` 下新增 message/oauth2/robot/export 四个子包,把对应 WxJava 能力封装为可注入的工具 Bean。

**Architecture:** 混合风格——读取类转本地 VO、写入类用 WxJava 入参;统一捕获 `WxErrorException` 转抛非受检 `QywxApiException`;4 个 Util 均注入 primary `WxCpService`;导出提供底层方法 + 一站式轮询。

**Tech Stack:** Java 21, Spring Boot 3.5, WxJava `weixin-java-cp` 4.8.2.B, Lombok, JUnit 5 + Mockito + AssertJ。

---

## 代码风格约定（所有任务遵守）

- **javadoc**:本仓库每个 public 类/字段/方法都带中文 javadoc(`类说明：`/`字段说明：`/方法描述 + `@author cy` + `Copyright (c) CY`)。下面代码块为节省篇幅省略了逐字段 javadoc，**实现时必须按现有风格补齐**(参照 `WxUserVO`、`WxContactConverter`)。
- **VO**:`@Data` + `implements Serializable`,字段全用包装类型。
- **Converter**:`public final class` + 私有构造 + `public static from(...)`,入参为 null 返回 null。
- **Util**:`private static final Logger log = LoggerFactory.getLogger(X.class)`;构造末尾 `log.info("X initialized")`;每个对外方法 try-catch `WxErrorException` 转抛 `QywxApiException`。
- **QueryOptions**:`record` + 静态 `defaults()`(参照 `WxCheckinQueryOptions`)。
- 每个任务结尾 `mvn -B test` 必须全绿后再提交。

---

## Task 1: 统一异常 `QywxApiException`

**Files:**
- Create: `src/main/java/org/cy/qywx/exception/QywxApiException.java`
- Test: `src/test/java/org/cy/qywx/exception/QywxApiExceptionTest.java`

- [ ] **Step 1: 写失败测试**

```java
package org.cy.qywx.exception;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QywxApiExceptionTest {

    @Test
    void fromExtractsErrCodeAndMsg() {
        WxError error = WxError.builder().errorCode(40014).errorMsg("invalid access_token").build();
        QywxApiException ex = QywxApiException.from(new WxErrorException(error));

        assertThat(ex.getErrCode()).isEqualTo(40014);
        assertThat(ex.getErrMsg()).isEqualTo("invalid access_token");
        assertThat(ex.getMessage()).contains("40014").contains("invalid access_token");
    }

    @Test
    void fromHandlesNullError() {
        QywxApiException ex = QywxApiException.from(new WxErrorException("boom"));
        assertThat(ex.getErrCode()).isNull();
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
```

- [ ] **Step 2: 运行验证失败**

Run: `mvn -B test -Dtest=QywxApiExceptionTest`
Expected: 编译失败 / FAIL（`QywxApiException` 不存在）

- [ ] **Step 3: 实现**

```java
package org.cy.qywx.exception;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;

/**
 * 类说明：企业微信 API 调用异常（非受检，包装 WxErrorException）。
 *
 * @author cy
 * Copyright (c) CY
 */
public class QywxApiException extends RuntimeException {

    private final Integer errCode;
    private final String errMsg;

    public QywxApiException(String message, Integer errCode, String errMsg) {
        super(message);
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public static QywxApiException from(WxErrorException e) {
        WxError error = e.getError();
        Integer code = error == null ? null : error.getErrorCode();
        String msg = error == null ? e.getMessage() : error.getErrorMsg();
        return new QywxApiException("WeCom API error: errcode=" + code + ", errmsg=" + msg, code, msg);
    }

    public Integer getErrCode() {
        return errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }
}
```

- [ ] **Step 4: 运行验证通过**

Run: `mvn -B test -Dtest=QywxApiExceptionTest`
Expected: PASS（2 tests）

- [ ] **Step 5: 提交**

```bash
git add src/main/java/org/cy/qywx/exception/QywxApiException.java src/test/java/org/cy/qywx/exception/QywxApiExceptionTest.java
git commit -m "feat: add QywxApiException for unified WeCom API error wrapping"
```

---

## Task 2: message 模块（消息推送）

**Files:**
- Create: `src/main/java/org/cy/qywx/util/message/WxMessageSendResultVO.java`
- Create: `src/main/java/org/cy/qywx/util/message/WxMessageConverter.java`
- Create: `src/main/java/org/cy/qywx/util/message/WxMessagePushUtil.java`
- Test: `src/test/java/org/cy/qywx/util/message/WxMessagePushUtilTest.java`

- [ ] **Step 1: 写失败测试**

```java
package org.cy.qywx.util.message;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpMessageService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import me.chanjar.weixin.cp.bean.message.WxCpMessageSendResult;
import org.cy.qywx.exception.QywxApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WxMessagePushUtilTest {

    private final WxCpService wxCpService = mock(WxCpService.class);
    private final WxCpMessageService messageService = mock(WxCpMessageService.class);

    private WxMessagePushUtil newUtil() {
        when(wxCpService.getMessageService()).thenReturn(messageService);
        return new WxMessagePushUtil(wxCpService);
    }

    @Test
    void sendTextBuildsTextMessageAndMapsResult() throws Exception {
        WxCpMessageSendResult sdk = new WxCpMessageSendResult();
        sdk.setMsgId("MSG1");
        sdk.setInvalidUser("u2");
        when(messageService.send(any(WxCpMessage.class))).thenReturn(sdk);

        WxMessageSendResultVO vo = newUtil().sendText("u1", "hello");

        assertThat(vo.getMsgId()).isEqualTo("MSG1");
        assertThat(vo.getInvalidUser()).isEqualTo("u2");

        ArgumentCaptor<WxCpMessage> captor = ArgumentCaptor.forClass(WxCpMessage.class);
        verify(messageService).send(captor.capture());
        assertThat(captor.getValue().getMsgType()).isEqualTo("text");
        assertThat(captor.getValue().getToUser()).isEqualTo("u1");
        assertThat(captor.getValue().getContent()).isEqualTo("hello");
    }

    @Test
    void sendWrapsWxErrorException() throws Exception {
        when(messageService.send(any(WxCpMessage.class)))
                .thenThrow(new WxErrorException(WxError.builder().errorCode(40014).errorMsg("bad").build()));

        WxMessagePushUtil util = newUtil();
        assertThatThrownBy(() -> util.send(new WxCpMessage()))
                .isInstanceOf(QywxApiException.class)
                .satisfies(e -> assertThat(((QywxApiException) e).getErrCode()).isEqualTo(40014));
    }

    @Test
    void recallWrapsWxErrorException() throws Exception {
        when(wxCpService.getMessageService()).thenReturn(messageService);
        org.mockito.Mockito.doThrow(new WxErrorException(WxError.builder().errorCode(50002).errorMsg("no").build()))
                .when(messageService).recall("m1");

        WxMessagePushUtil util = new WxMessagePushUtil(wxCpService);
        assertThatThrownBy(() -> util.recall("m1")).isInstanceOf(QywxApiException.class);
    }
}
```

- [ ] **Step 2: 运行验证失败**

Run: `mvn -B test -Dtest=WxMessagePushUtilTest`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 实现**

`WxMessageSendResultVO.java`:
```java
package org.cy.qywx.util.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class WxMessageSendResultVO implements Serializable {
    private String msgId;
    private String responseCode;
    private String invalidUser;
    private String invalidParty;
    private String invalidTag;
    private String unlicensedUser;
}
```

`WxMessageConverter.java`:
```java
package org.cy.qywx.util.message;

import me.chanjar.weixin.cp.bean.message.WxCpMessageSendResult;

public final class WxMessageConverter {

    private WxMessageConverter() {
    }

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
```

`WxMessagePushUtil.java`:
```java
package org.cy.qywx.util.message;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import me.chanjar.weixin.cp.bean.message.WxCpMessageSendResult;
import org.cy.qywx.exception.QywxApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WxMessagePushUtil {

    private static final Logger log = LoggerFactory.getLogger(WxMessagePushUtil.class);

    private final WxCpService wxCpService;

    public WxMessagePushUtil(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
        log.info("WxMessagePushUtil initialized");
    }

    public WxMessageSendResultVO send(WxCpMessage message) {
        try {
            WxCpMessageSendResult result = wxCpService.getMessageService().send(message);
            return WxMessageConverter.from(result);
        } catch (WxErrorException e) {
            log.error("send message failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    public WxMessageSendResultVO sendText(String toUser, String content) {
        return send(WxCpMessage.TEXT().toUser(toUser).content(content).build());
    }

    public WxMessageSendResultVO sendMarkdown(String toUser, String content) {
        return send(WxCpMessage.MARKDOWN().toUser(toUser).content(content).build());
    }

    public WxMessageSendResultVO sendTextCard(String toUser, String title, String description, String url, String btnTxt) {
        return send(WxCpMessage.TEXTCARD().toUser(toUser).title(title).description(description).url(url).btnTxt(btnTxt).build());
    }

    public void recall(String msgId) {
        try {
            wxCpService.getMessageService().recall(msgId);
        } catch (WxErrorException e) {
            log.error("recall message failed: msgId={}, error={}", msgId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }
}
```

- [ ] **Step 4: 运行验证通过**

Run: `mvn -B test -Dtest=WxMessagePushUtilTest`
Expected: PASS（3 tests）

- [ ] **Step 5: 提交**

```bash
git add src/main/java/org/cy/qywx/util/message src/test/java/org/cy/qywx/util/message
git commit -m "feat: add message push module (send/recall + text/markdown/textcard helpers)"
```

---

## Task 3: oauth2 模块（网页授权登录）

**Files:**
- Create: `src/main/java/org/cy/qywx/util/oauth2/WxOauth2UserVO.java`
- Create: `src/main/java/org/cy/qywx/util/oauth2/WxOauth2UserDetailVO.java`
- Create: `src/main/java/org/cy/qywx/util/oauth2/WxOauth2Converter.java`
- Create: `src/main/java/org/cy/qywx/util/oauth2/WxOauth2Util.java`
- Test: `src/test/java/org/cy/qywx/util/oauth2/WxOauth2UtilTest.java`

- [ ] **Step 1: 写失败测试**

```java
package org.cy.qywx.util.oauth2;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpOAuth2Service;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpOauth2UserInfo;
import me.chanjar.weixin.cp.bean.WxCpUserDetail;
import org.cy.qywx.exception.QywxApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WxOauth2UtilTest {

    private final WxCpService wxCpService = mock(WxCpService.class);
    private final WxCpOAuth2Service oauth2Service = mock(WxCpOAuth2Service.class);

    private WxOauth2Util newUtil() {
        when(wxCpService.getOauth2Service()).thenReturn(oauth2Service);
        return new WxOauth2Util(wxCpService);
    }

    @Test
    void buildAuthorizationUrlDelegates() {
        when(oauth2Service.buildAuthorizationUrl("https://cb", "snsapi_base", "st")).thenReturn("https://auth");
        assertThat(newUtil().buildAuthorizationUrl("https://cb", "snsapi_base", "st")).isEqualTo("https://auth");
    }

    @Test
    void getUserInfoMapsToVo() throws Exception {
        WxCpOauth2UserInfo info = new WxCpOauth2UserInfo();
        info.setUserId("u1");
        info.setUserTicket("t1");
        when(oauth2Service.getUserInfo("code1")).thenReturn(info);

        WxOauth2UserVO vo = newUtil().getUserInfo("code1");
        assertThat(vo.getUserId()).isEqualTo("u1");
        assertThat(vo.getUserTicket()).isEqualTo("t1");
    }

    @Test
    void getUserDetailMapsToVo() throws Exception {
        WxCpUserDetail detail = new WxCpUserDetail();
        detail.setUserId("u1");
        detail.setName("Alice");
        detail.setMobile("123");
        when(oauth2Service.getUserDetail("ticket1")).thenReturn(detail);

        WxOauth2UserDetailVO vo = newUtil().getUserDetail("ticket1");
        assertThat(vo.getName()).isEqualTo("Alice");
        assertThat(vo.getMobile()).isEqualTo("123");
    }

    @Test
    void getUserInfoWrapsWxErrorException() throws Exception {
        when(oauth2Service.getUserInfo("bad"))
                .thenThrow(new WxErrorException(WxError.builder().errorCode(40029).errorMsg("invalid code").build()));
        WxOauth2Util util = newUtil();
        assertThatThrownBy(() -> util.getUserInfo("bad")).isInstanceOf(QywxApiException.class);
    }
}
```

- [ ] **Step 2: 运行验证失败**

Run: `mvn -B test -Dtest=WxOauth2UtilTest`
Expected: 编译失败

- [ ] **Step 3: 实现**

`WxOauth2UserVO.java`:
```java
package org.cy.qywx.util.oauth2;

import lombok.Data;

import java.io.Serializable;

@Data
public class WxOauth2UserVO implements Serializable {
    private String userId;
    private String openId;
    private String userTicket;
    private String deviceId;
    private String externalUserId;
    private String expiresIn;
}
```

`WxOauth2UserDetailVO.java`:
```java
package org.cy.qywx.util.oauth2;

import lombok.Data;

import java.io.Serializable;

@Data
public class WxOauth2UserDetailVO implements Serializable {
    private String userId;
    private String name;
    private String mobile;
    private String gender;
    private String email;
    private String avatar;
    private String qrCode;
    private String address;
    private String bizMail;
}
```

`WxOauth2Converter.java`:
```java
package org.cy.qywx.util.oauth2;

import me.chanjar.weixin.cp.bean.WxCpOauth2UserInfo;
import me.chanjar.weixin.cp.bean.WxCpUserDetail;

public final class WxOauth2Converter {

    private WxOauth2Converter() {
    }

    public static WxOauth2UserVO from(WxCpOauth2UserInfo info) {
        if (info == null) {
            return null;
        }
        WxOauth2UserVO vo = new WxOauth2UserVO();
        vo.setUserId(info.getUserId());
        vo.setOpenId(info.getOpenId());
        vo.setUserTicket(info.getUserTicket());
        vo.setDeviceId(info.getDeviceId());
        vo.setExternalUserId(info.getExternalUserId());
        vo.setExpiresIn(info.getExpiresIn());
        return vo;
    }

    public static WxOauth2UserDetailVO from(WxCpUserDetail detail) {
        if (detail == null) {
            return null;
        }
        WxOauth2UserDetailVO vo = new WxOauth2UserDetailVO();
        vo.setUserId(detail.getUserId());
        vo.setName(detail.getName());
        vo.setMobile(detail.getMobile());
        vo.setGender(detail.getGender());
        vo.setEmail(detail.getEmail());
        vo.setAvatar(detail.getAvatar());
        vo.setQrCode(detail.getQrCode());
        vo.setAddress(detail.getAddress());
        vo.setBizMail(detail.getBizMail());
        return vo;
    }
}
```

`WxOauth2Util.java`:
```java
package org.cy.qywx.util.oauth2;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import org.cy.qywx.exception.QywxApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WxOauth2Util {

    private static final Logger log = LoggerFactory.getLogger(WxOauth2Util.class);

    private final WxCpService wxCpService;

    public WxOauth2Util(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
        log.info("WxOauth2Util initialized");
    }

    public String buildAuthorizationUrl(String redirectUri, String scope, String state) {
        return wxCpService.getOauth2Service().buildAuthorizationUrl(redirectUri, scope, state);
    }

    public WxOauth2UserVO getUserInfo(String code) {
        try {
            return WxOauth2Converter.from(wxCpService.getOauth2Service().getUserInfo(code));
        } catch (WxErrorException e) {
            log.error("oauth2 getUserInfo failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    public WxOauth2UserDetailVO getUserDetail(String userTicket) {
        try {
            return WxOauth2Converter.from(wxCpService.getOauth2Service().getUserDetail(userTicket));
        } catch (WxErrorException e) {
            log.error("oauth2 getUserDetail failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }
}
```

- [ ] **Step 4: 运行验证通过**

Run: `mvn -B test -Dtest=WxOauth2UtilTest`
Expected: PASS（4 tests）

- [ ] **Step 5: 提交**

```bash
git add src/main/java/org/cy/qywx/util/oauth2 src/test/java/org/cy/qywx/util/oauth2
git commit -m "feat: add oauth2 module (authorization url + user info/detail to VO)"
```

---

## Task 4: robot 模块（智能机器人）

**Files:**
- Create: `src/main/java/org/cy/qywx/util/robot/WxRobotVO.java`
- Create: `src/main/java/org/cy/qywx/util/robot/WxRobotChatVO.java`
- Create: `src/main/java/org/cy/qywx/util/robot/WxRobotSendResultVO.java`
- Create: `src/main/java/org/cy/qywx/util/robot/WxRobotConverter.java`
- Create: `src/main/java/org/cy/qywx/util/robot/WxIntelligentRobotUtil.java`
- Test: `src/test/java/org/cy/qywx/util/robot/WxIntelligentRobotUtilTest.java`

- [ ] **Step 1: 写失败测试**

```java
package org.cy.qywx.util.robot;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpIntelligentRobotService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobot;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotChatRequest;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotChatResponse;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotCreateRequest;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotCreateResponse;
import org.cy.qywx.exception.QywxApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WxIntelligentRobotUtilTest {

    private final WxCpService wxCpService = mock(WxCpService.class);
    private final WxCpIntelligentRobotService robotService = mock(WxCpIntelligentRobotService.class);

    private WxIntelligentRobotUtil newUtil() {
        when(wxCpService.getIntelligentRobotService()).thenReturn(robotService);
        return new WxIntelligentRobotUtil(wxCpService);
    }

    @Test
    void createRobotReturnsRobotId() throws Exception {
        WxCpIntelligentRobotCreateResponse resp = new WxCpIntelligentRobotCreateResponse();
        resp.setRobotId("r1");
        when(robotService.createRobot(any(WxCpIntelligentRobotCreateRequest.class))).thenReturn(resp);

        assertThat(newUtil().createRobot(new WxCpIntelligentRobotCreateRequest())).isEqualTo("r1");
    }

    @Test
    void getRobotMapsToVo() throws Exception {
        WxCpIntelligentRobot robot = new WxCpIntelligentRobot();
        robot.setRobotId("r1");
        robot.setName("bot");
        robot.setStatus(1);
        when(robotService.getRobot("r1")).thenReturn(robot);

        WxRobotVO vo = newUtil().getRobot("r1");
        assertThat(vo.getRobotId()).isEqualTo("r1");
        assertThat(vo.getName()).isEqualTo("bot");
        assertThat(vo.getStatus()).isEqualTo(1);
    }

    @Test
    void chatMapsToVo() throws Exception {
        WxCpIntelligentRobotChatResponse resp = new WxCpIntelligentRobotChatResponse();
        resp.setReply("hi");
        resp.setSessionId("s1");
        when(robotService.chat(any(WxCpIntelligentRobotChatRequest.class))).thenReturn(resp);

        WxRobotChatVO vo = newUtil().chat(new WxCpIntelligentRobotChatRequest());
        assertThat(vo.getReply()).isEqualTo("hi");
        assertThat(vo.getSessionId()).isEqualTo("s1");
    }

    @Test
    void getRobotWrapsWxErrorException() throws Exception {
        when(robotService.getRobot("bad"))
                .thenThrow(new WxErrorException(WxError.builder().errorCode(60020).errorMsg("no").build()));
        WxIntelligentRobotUtil util = newUtil();
        assertThatThrownBy(() -> util.getRobot("bad")).isInstanceOf(QywxApiException.class);
    }
}
```

- [ ] **Step 2: 运行验证失败**

Run: `mvn -B test -Dtest=WxIntelligentRobotUtilTest`
Expected: 编译失败

- [ ] **Step 3: 实现**

`WxRobotVO.java`:
```java
package org.cy.qywx.util.robot;

import lombok.Data;

import java.io.Serializable;

@Data
public class WxRobotVO implements Serializable {
    private String robotId;
    private String name;
    private String description;
    private String avatar;
    private Integer status;
    private Long createTime;
    private Long updateTime;
}
```

`WxRobotChatVO.java`:
```java
package org.cy.qywx.util.robot;

import lombok.Data;

import java.io.Serializable;

@Data
public class WxRobotChatVO implements Serializable {
    private String reply;
    private String sessionId;
    private String msgId;
}
```

`WxRobotSendResultVO.java`:
```java
package org.cy.qywx.util.robot;

import lombok.Data;

import java.io.Serializable;

@Data
public class WxRobotSendResultVO implements Serializable {
    private String msgId;
    private String sessionId;
}
```

`WxRobotConverter.java`:
```java
package org.cy.qywx.util.robot;

import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobot;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotChatResponse;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotSendMessageResponse;

public final class WxRobotConverter {

    private WxRobotConverter() {
    }

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
```

`WxIntelligentRobotUtil.java`:
```java
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

public class WxIntelligentRobotUtil {

    private static final Logger log = LoggerFactory.getLogger(WxIntelligentRobotUtil.class);

    private final WxCpService wxCpService;

    public WxIntelligentRobotUtil(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
        log.info("WxIntelligentRobotUtil initialized");
    }

    public String createRobot(WxCpIntelligentRobotCreateRequest req) {
        try {
            return wxCpService.getIntelligentRobotService().createRobot(req).getRobotId();
        } catch (WxErrorException e) {
            log.error("createRobot failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    public void updateRobot(WxCpIntelligentRobotUpdateRequest req) {
        try {
            wxCpService.getIntelligentRobotService().updateRobot(req);
        } catch (WxErrorException e) {
            log.error("updateRobot failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    public void deleteRobot(String robotId) {
        try {
            wxCpService.getIntelligentRobotService().deleteRobot(robotId);
        } catch (WxErrorException e) {
            log.error("deleteRobot failed: robotId={}, error={}", robotId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    public WxRobotVO getRobot(String robotId) {
        try {
            return WxRobotConverter.from(wxCpService.getIntelligentRobotService().getRobot(robotId));
        } catch (WxErrorException e) {
            log.error("getRobot failed: robotId={}, error={}", robotId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    public WxRobotChatVO chat(WxCpIntelligentRobotChatRequest req) {
        try {
            return WxRobotConverter.from(wxCpService.getIntelligentRobotService().chat(req));
        } catch (WxErrorException e) {
            log.error("robot chat failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    public WxRobotSendResultVO sendMessage(WxCpIntelligentRobotSendMessageRequest req) {
        try {
            return WxRobotConverter.from(wxCpService.getIntelligentRobotService().sendMessage(req));
        } catch (WxErrorException e) {
            log.error("robot sendMessage failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    public void resetSession(String robotId, String chatType, String chatId) {
        try {
            wxCpService.getIntelligentRobotService().resetSession(robotId, chatType, chatId);
        } catch (WxErrorException e) {
            log.error("robot resetSession failed: robotId={}, error={}", robotId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }
}
```

> 注：`resetSession` 三个 String 参数按 WxJava `resetSession(String, String, String)` 顺序透传；实现时核对 WxJava javadoc 的参数语义，保持透传顺序一致即可。

- [ ] **Step 4: 运行验证通过**

Run: `mvn -B test -Dtest=WxIntelligentRobotUtilTest`
Expected: PASS（4 tests）

- [ ] **Step 5: 提交**

```bash
git add src/main/java/org/cy/qywx/util/robot src/test/java/org/cy/qywx/util/robot
git commit -m "feat: add intelligent robot module (CRUD + chat/sendMessage/resetSession)"
```

---

## Task 5: export 模块（异步批量导出）

**Files:**
- Create: `src/main/java/org/cy/qywx/util/export/WxExportDataVO.java`
- Create: `src/main/java/org/cy/qywx/util/export/WxExportResultVO.java`
- Create: `src/main/java/org/cy/qywx/util/export/WxExportType.java`
- Create: `src/main/java/org/cy/qywx/util/export/WxExportQueryOptions.java`
- Create: `src/main/java/org/cy/qywx/util/export/WxExportConverter.java`
- Create: `src/main/java/org/cy/qywx/util/export/WxExportUtil.java`
- Test: `src/test/java/org/cy/qywx/util/export/WxExportUtilTest.java`

- [ ] **Step 1: 写失败测试**

```java
package org.cy.qywx.util.export;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpExportService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.export.WxCpExportRequest;
import me.chanjar.weixin.cp.bean.export.WxCpExportResult;
import org.cy.qywx.exception.QywxApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WxExportUtilTest {

    private final WxCpService wxCpService = mock(WxCpService.class);
    private final WxCpExportService exportService = mock(WxCpExportService.class);

    private WxExportUtil newUtil(WxExportQueryOptions options) {
        when(wxCpService.getExportService()).thenReturn(exportService);
        return new WxExportUtil(wxCpService, options);
    }

    private static WxCpExportResult result(int status) {
        WxCpExportResult r = new WxCpExportResult();
        r.setStatus(status);
        return r;
    }

    @Test
    void exportUserReturnsJobId() throws Exception {
        when(exportService.user(any(WxCpExportRequest.class))).thenReturn("JOB1");
        assertThat(newUtil(WxExportQueryOptions.defaults()).exportUser(new WxCpExportRequest())).isEqualTo("JOB1");
    }

    @Test
    void exportAndWaitPollsUntilFinished() throws Exception {
        when(exportService.user(any(WxCpExportRequest.class))).thenReturn("JOB1");
        when(exportService.getResult("JOB1")).thenReturn(result(1), result(2));

        WxExportUtil util = newUtil(new WxExportQueryOptions(0L, 60_000L, 5));
        WxExportResultVO vo = util.exportAndWait(WxExportType.USER, new WxCpExportRequest());

        assertThat(vo.getStatus()).isEqualTo(2);
        verify(exportService, times(2)).getResult("JOB1");
    }

    @Test
    void exportAndWaitThrowsOnTimeout() throws Exception {
        when(exportService.user(any(WxCpExportRequest.class))).thenReturn("JOB1");
        when(exportService.getResult("JOB1")).thenReturn(result(1));

        WxExportUtil util = newUtil(new WxExportQueryOptions(0L, 60_000L, 3));
        assertThatThrownBy(() -> util.exportAndWait(WxExportType.USER, new WxCpExportRequest()))
                .isInstanceOf(QywxApiException.class);
        verify(exportService, times(3)).getResult("JOB1");
    }

    @Test
    void getResultWrapsWxErrorException() throws Exception {
        when(exportService.getResult("bad"))
                .thenThrow(new WxErrorException(WxError.builder().errorCode(60020).errorMsg("no").build()));
        WxExportUtil util = newUtil(WxExportQueryOptions.defaults());
        assertThatThrownBy(() -> util.getResult("bad")).isInstanceOf(QywxApiException.class);
    }
}
```

- [ ] **Step 2: 运行验证失败**

Run: `mvn -B test -Dtest=WxExportUtilTest`
Expected: 编译失败

- [ ] **Step 3: 实现**

`WxExportDataVO.java`:
```java
package org.cy.qywx.util.export;

import lombok.Data;

import java.io.Serializable;

@Data
public class WxExportDataVO implements Serializable {
    private String url;
    private Integer size;
    private String md5;
}
```

`WxExportResultVO.java`:
```java
package org.cy.qywx.util.export;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WxExportResultVO implements Serializable {
    private Integer status;
    private List<WxExportDataVO> dataList;
}
```

`WxExportType.java`:
```java
package org.cy.qywx.util.export;

public enum WxExportType {
    SIMPLE_USER,
    USER,
    DEPARTMENT,
    TAG_USER
}
```

`WxExportQueryOptions.java`:
```java
package org.cy.qywx.util.export;

/**
 * 记录说明：异步导出轮询配置选项。
 *
 * @param pollIntervalMillis 轮询间隔毫秒
 * @param pollTimeoutMillis 轮询总超时毫秒
 * @param maxPollAttempts 最大轮询次数
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxExportQueryOptions(
        long pollIntervalMillis,
        long pollTimeoutMillis,
        int maxPollAttempts
) {
    public static WxExportQueryOptions defaults() {
        return new WxExportQueryOptions(2000L, 60000L, 30);
    }
}
```

`WxExportConverter.java`:
```java
package org.cy.qywx.util.export;

import me.chanjar.weixin.cp.bean.export.WxCpExportResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class WxExportConverter {

    private WxExportConverter() {
    }

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

    private static WxExportDataVO fromData(WxCpExportResult.ExportData data) {
        WxExportDataVO vo = new WxExportDataVO();
        vo.setUrl(data.getUrl());
        vo.setSize(data.getSize());
        vo.setMd5(data.getMd5());
        return vo;
    }
}
```

`WxExportUtil.java`:
```java
package org.cy.qywx.util.export;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.export.WxCpExportRequest;
import me.chanjar.weixin.cp.bean.export.WxCpExportResult;
import org.cy.qywx.exception.QywxApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;

public class WxExportUtil {

    private static final Logger log = LoggerFactory.getLogger(WxExportUtil.class);

    /** WeCom 导出任务完成状态：1=处理中，2=完成，3=异常。 */
    private static final int STATUS_FINISHED = 2;

    private final WxCpService wxCpService;
    private final WxExportQueryOptions options;

    public WxExportUtil(WxCpService wxCpService, WxExportQueryOptions options) {
        this.wxCpService = wxCpService;
        this.options = options;
        log.info("WxExportUtil initialized: pollIntervalMillis={}, pollTimeoutMillis={}, maxPollAttempts={}",
                options.pollIntervalMillis(), options.pollTimeoutMillis(), options.maxPollAttempts());
    }

    public String exportSimpleUser(WxCpExportRequest req) {
        return submit("simpleUser", () -> wxCpService.getExportService().simpleUser(req));
    }

    public String exportUser(WxCpExportRequest req) {
        return submit("user", () -> wxCpService.getExportService().user(req));
    }

    public String exportDepartment(WxCpExportRequest req) {
        return submit("department", () -> wxCpService.getExportService().department(req));
    }

    public String exportTagUser(WxCpExportRequest req) {
        return submit("tagUser", () -> wxCpService.getExportService().tagUser(req));
    }

    public WxExportResultVO getResult(String jobId) {
        try {
            WxCpExportResult result = wxCpService.getExportService().getResult(jobId);
            return WxExportConverter.from(result);
        } catch (WxErrorException e) {
            log.error("export getResult failed: jobId={}, error={}", jobId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    public WxExportResultVO exportAndWait(WxExportType type, WxCpExportRequest req) {
        String jobId = switch (type) {
            case SIMPLE_USER -> exportSimpleUser(req);
            case USER -> exportUser(req);
            case DEPARTMENT -> exportDepartment(req);
            case TAG_USER -> exportTagUser(req);
        };
        long deadlineNanos = System.nanoTime() + options.pollTimeoutMillis() * 1_000_000L;
        for (int attempt = 1; attempt <= options.maxPollAttempts(); attempt++) {
            WxExportResultVO result = getResult(jobId);
            if (result.getStatus() != null && result.getStatus() == STATUS_FINISHED) {
                return result;
            }
            if (System.nanoTime() >= deadlineNanos) {
                break;
            }
            if (options.pollIntervalMillis() > 0) {
                LockSupport.parkNanos(options.pollIntervalMillis() * 1_000_000L);
            }
        }
        throw new QywxApiException("export polling timed out: jobId=" + jobId, null, "export polling timed out");
    }

    private String submit(String name, ExportCall call) {
        try {
            return call.invoke();
        } catch (WxErrorException e) {
            log.error("export {} failed: {}", name, e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    @FunctionalInterface
    private interface ExportCall {
        String invoke() throws WxErrorException;
    }
}
```

- [ ] **Step 4: 运行验证通过**

Run: `mvn -B test -Dtest=WxExportUtilTest`
Expected: PASS（4 tests）

- [ ] **Step 5: 提交**

```bash
git add src/main/java/org/cy/qywx/util/export src/test/java/org/cy/qywx/util/export
git commit -m "feat: add export module (submit + getResult + exportAndWait polling)"
```

---

## Task 6: 配置扩展 + 自动装配

**Files:**
- Modify: `src/main/java/org/cy/qywx/config/WxCpProperties.java`（新增 `Export` 内部类 + `export` 字段 + getter/setter）
- Modify: `src/main/java/org/cy/qywx/config/QywxWecomAutoConfiguration.java`（新增 4 个 `@Bean`）
- Modify: `src/test/java/org/cy/qywx/config/QywxWecomAutoConfigurationTest.java`（断言 4 个新 Bean）

- [ ] **Step 1: 写失败测试**

在 `QywxWecomAutoConfigurationTest` 中新增测试方法（与现有 `ApplicationContextRunner` 风格一致；如已有 runner 字段则复用）：

```java
@Test
void registersNewModuleBeansWhenContactConfigured() {
    new org.springframework.boot.test.context.runner.ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(QywxWecomAutoConfiguration.class))
            .withPropertyValues("wx.cp.corp-id=cid", "wx.cp.corp-secret=sec", "wx.cp.agent-id=1")
            .run(context -> {
                assertThat(context).hasSingleBean(org.cy.qywx.util.message.WxMessagePushUtil.class);
                assertThat(context).hasSingleBean(org.cy.qywx.util.oauth2.WxOauth2Util.class);
                assertThat(context).hasSingleBean(org.cy.qywx.util.robot.WxIntelligentRobotUtil.class);
                assertThat(context).hasSingleBean(org.cy.qywx.util.export.WxExportUtil.class);
            });
}
```

- [ ] **Step 2: 运行验证失败**

Run: `mvn -B test -Dtest=QywxWecomAutoConfigurationTest#registersNewModuleBeansWhenContactConfigured`
Expected: FAIL（无对应 Bean）

- [ ] **Step 3a: 给 `WxCpProperties` 增加 `Export` 配置**

在 `WxCpProperties` 顶层字段区（`checkin` 字段之后）新增：

```java
    private Export export = new Export();

    public Export getExport() {
        return export;
    }

    public void setExport(Export export) {
        this.export = export;
    }
```

并在类内（与其他 static 内部类并列）新增：

```java
    /**
     * 类说明：异步导出。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static class Export {
        private long pollIntervalMillis = 2000L;
        private long pollTimeoutMillis = 60000L;
        private int maxPollAttempts = 30;

        public long getPollIntervalMillis() {
            return pollIntervalMillis;
        }

        public void setPollIntervalMillis(long pollIntervalMillis) {
            this.pollIntervalMillis = pollIntervalMillis;
        }

        public long getPollTimeoutMillis() {
            return pollTimeoutMillis;
        }

        public void setPollTimeoutMillis(long pollTimeoutMillis) {
            this.pollTimeoutMillis = pollTimeoutMillis;
        }

        public int getMaxPollAttempts() {
            return maxPollAttempts;
        }

        public void setMaxPollAttempts(int maxPollAttempts) {
            this.maxPollAttempts = maxPollAttempts;
        }
    }
```

- [ ] **Step 3b: 给 `QywxWecomAutoConfiguration` 增加 4 个 Bean**

新增 import：
```java
import org.cy.qywx.util.message.WxMessagePushUtil;
import org.cy.qywx.util.oauth2.WxOauth2Util;
import org.cy.qywx.util.robot.WxIntelligentRobotUtil;
import org.cy.qywx.util.export.WxExportUtil;
import org.cy.qywx.util.export.WxExportQueryOptions;
```

在类内新增（放在 `wxCheckinQueryUtil` 之后）：
```java
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxMessagePushUtil wxMessagePushUtil(WxCpService wxCpService) {
        return new WxMessagePushUtil(wxCpService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxOauth2Util wxOauth2Util(WxCpService wxCpService) {
        return new WxOauth2Util(wxCpService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxIntelligentRobotUtil wxIntelligentRobotUtil(WxCpService wxCpService) {
        return new WxIntelligentRobotUtil(wxCpService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WxCpService.class)
    public WxExportUtil wxExportUtil(WxCpService wxCpService, WxCpProperties properties) {
        WxCpProperties.Export e = properties.getExport();
        WxExportQueryOptions options = new WxExportQueryOptions(
                e.getPollIntervalMillis(),
                e.getPollTimeoutMillis(),
                e.getMaxPollAttempts()
        );
        return new WxExportUtil(wxCpService, options);
    }
```

- [ ] **Step 4: 运行验证通过**

Run: `mvn -B test -Dtest=QywxWecomAutoConfigurationTest`
Expected: PASS（含新方法）

- [ ] **Step 5: 提交**

```bash
git add src/main/java/org/cy/qywx/config src/test/java/org/cy/qywx/config
git commit -m "feat: wire message/oauth2/robot/export utils into auto-configuration"
```

---

## Task 7: 文档 + 全量回归

**Files:**
- Modify: `README.md`、`README.zh-CN.md`

- [ ] **Step 1: 在两份 README 的「功能/模块」章节增补**

中文（`README.zh-CN.md`）追加条目：
```markdown
- **消息推送** `WxMessagePushUtil`：`send(WxCpMessage)` / `sendText` / `sendMarkdown` / `sendTextCard` / `recall`
- **网页授权登录** `WxOauth2Util`：`buildAuthorizationUrl` / `getUserInfo` / `getUserDetail`
- **智能机器人** `WxIntelligentRobotUtil`：机器人 CRUD + `chat` / `sendMessage` / `resetSession`
- **异步批量导出** `WxExportUtil`：`exportSimpleUser/user/department/tagUser` + `getResult` + 一站式 `exportAndWait`
  - 轮询配置：`wx.cp.export.poll-interval-millis`（默认 2000）、`wx.cp.export.poll-timeout-millis`（默认 60000）、`wx.cp.export.max-poll-attempts`（默认 30）
```

英文（`README.md`）追加对应英文条目（同结构）。

- [ ] **Step 2: 全量回归**

Run: `mvn -B clean test`
Expected: BUILD SUCCESS，原有 76 + 新增（约 17）测试全部通过，0 失败。

- [ ] **Step 3: 提交**

```bash
git add README.md README.zh-CN.md
git commit -m "docs: document message/oauth2/robot/export modules and export config"
```

---

## Self-Review（写计划后自查结论）

- **Spec coverage**：4 模块方法、QywxApiException、export 轮询/边界、配置扩展、4 个 Bean、测试、README —— Task 1-7 全覆盖；导出"止于下载链接、不解密"由 `WxExportConverter` 仅映射 url/size/md5 体现。
- **Placeholder**：无 TBD/TODO；唯一外部不确定点 `resetSession` 参数语义与导出 `STATUS_FINISHED=2` 已以「透传 + 常量 + 注释」明确。
- **Type consistency**：`WxCpService` getter（`getMessageService/getOauth2Service/getIntelligentRobotService/getExportService`）、builder（`TEXT/MARKDOWN/TEXTCARD` + `toUser/content/title/description/url/btnTxt`）、`WxError.getErrorCode()/getErrorMsg()`、record 访问器（`options.pollIntervalMillis()`）、VO 字段名均与各任务一致。
