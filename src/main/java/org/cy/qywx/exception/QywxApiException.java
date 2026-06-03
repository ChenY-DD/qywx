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

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 企业微信错误码 */
    private final Integer errCode;
    /** 企业微信错误信息 */
    private final String errMsg;

    /**
     * 构造异常。
     *
     * @param message 异常消息
     * @param errCode 错误码
     * @param errMsg  错误信息
     */
    public QywxApiException(String message, Integer errCode, String errMsg) {
        this(message, errCode, errMsg, null);
    }

    /**
     * 构造异常（保留原始异常的 cause 链）。
     *
     * @param message 异常消息
     * @param errCode 错误码
     * @param errMsg  错误信息
     * @param cause   原始异常
     */
    public QywxApiException(String message, Integer errCode, String errMsg, Throwable cause) {
        super(message, cause);
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    /**
     * 由 WxErrorException 转换为本异常。
     *
     * @param e WxJava 受检异常
     * @return 包装后的非受检异常
     */
    public static QywxApiException from(WxErrorException e) {
        WxError error = e.getError();
        Integer code = error == null ? null : error.getErrorCode();
        String msg = error == null ? e.getMessage() : error.getErrorMsg();
        return new QywxApiException("WeCom API error: errcode=" + code + ", errmsg=" + msg, code, msg, e);
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public Integer getErrCode() {
        return errCode;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息
     */
    public String getErrMsg() {
        return errMsg;
    }
}
