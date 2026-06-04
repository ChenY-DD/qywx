package org.cy.qywx.util.export;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.export.WxCpExportRequest;
import me.chanjar.weixin.cp.bean.export.WxCpExportResult;
import org.cy.qywx.exception.QywxApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.concurrent.locks.LockSupport;

/**
 * 类说明：企业微信异步批量导出工具（提交任务、查询结果、一站式提交+轮询到完成/超时）。
 * 边界止于拿到加密文件下载链接（url/size/md5），不负责下载与解密。
 *
 * @author cy
 * Copyright (c) CY
 */
public class WxExportUtil {

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(WxExportUtil.class);

    /** WeCom 导出任务完成状态：1=处理中，2=完成，3=异常。 */
    private static final int STATUS_FINISHED = 2;

    /** WeCom 导出任务异常状态：3=异常（失败终态）。 */
    private static final int STATUS_FAILED = 3;

    /** EncodingAESKey 字符集（a-z / A-Z / 0-9，共 62 个）。 */
    private static final String AES_KEY_CHARSET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** EncodingAESKey 长度（企业微信固定要求 43 位）。 */
    private static final int ENCODING_AES_KEY_LENGTH = 43;

    /** 生成 EncodingAESKey 用的安全随机源（线程安全，可复用）。 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 企业微信服务（联系人/导出 scope） */
    private final WxCpService wxCpService;

    /** 导出查询配置 */
    private final WxExportQueryOptions options;

    /**
     * 构造导出工具。
     *
     * @param wxCpService 企业微信服务
     * @param options     导出查询配置
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxExportUtil(WxCpService wxCpService, WxExportQueryOptions options) {
        this.wxCpService = wxCpService;
        this.options = options;
        log.info("WxExportUtil initialized: pollIntervalMillis={}, pollTimeoutMillis={}, maxPollAttempts={}",
                options.pollIntervalMillis(), options.pollTimeoutMillis(), options.maxPollAttempts());
    }

    /**
     * 生成一个企业微信异步导出用的 43 位 EncodingAESKey。
     * <p>由调用方生成并妥善保存：企业微信用它加密导出文件，下载后需用同一把 key 解密，
     * 因此请生成一次后固定保存复用。字符取自 a-z / A-Z / 0-9；
     * {@code Base64.getDecoder().decode(key + "=")} 还原为 32 字节 AES-256 密钥。
     *
     * @return 43 位 EncodingAESKey
     *
     * @author cy
     * Copyright (c) CY
     */
    public static String generateEncodingAesKey() {
        StringBuilder sb = new StringBuilder(ENCODING_AES_KEY_LENGTH);
        for (int i = 0; i < ENCODING_AES_KEY_LENGTH; i++) {
            sb.append(AES_KEY_CHARSET.charAt(SECURE_RANDOM.nextInt(AES_KEY_CHARSET.length())));
        }
        return sb.toString();
    }

    /**
     * 提交导出成员（简易信息）任务。
     *
     * @param req 导出请求
     * @return 任务 jobId
     *
     * @author cy
     * Copyright (c) CY
     */
    public String exportSimpleUser(WxCpExportRequest req) {
        return submit("simpleUser", () -> wxCpService.getExportService().simpleUser(req));
    }

    /**
     * 提交导出成员（详细信息）任务。
     *
     * @param req 导出请求
     * @return 任务 jobId
     *
     * @author cy
     * Copyright (c) CY
     */
    public String exportUser(WxCpExportRequest req) {
        return submit("user", () -> wxCpService.getExportService().user(req));
    }

    /**
     * 提交导出部门任务。
     *
     * @param req 导出请求
     * @return 任务 jobId
     *
     * @author cy
     * Copyright (c) CY
     */
    public String exportDepartment(WxCpExportRequest req) {
        return submit("department", () -> wxCpService.getExportService().department(req));
    }

    /**
     * 提交导出标签成员任务。
     *
     * @param req 导出请求
     * @return 任务 jobId
     *
     * @author cy
     * Copyright (c) CY
     */
    public String exportTagUser(WxCpExportRequest req) {
        return submit("tagUser", () -> wxCpService.getExportService().tagUser(req));
    }

    /**
     * 按 jobId 查询导出结果。
     *
     * @param jobId 任务 jobId
     * @return 导出结果视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxExportResultVO getResult(String jobId) {
        try {
            WxCpExportResult result = wxCpService.getExportService().getResult(jobId);
            return WxExportConverter.from(result);
        } catch (WxErrorException e) {
            log.error("export getResult failed: jobId={}, error={}", jobId, e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 一站式提交导出任务并轮询至完成或超时。
     *
     * @param type 导出任务类型
     * @param req  导出请求
     * @return 完成状态的导出结果视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
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
            if (result.getStatus() != null && result.getStatus() == STATUS_FAILED) {
                throw new QywxApiException("export job failed: jobId=" + jobId + ", status=" + STATUS_FAILED, null, "export job failed");
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

    /**
     * 提交导出任务的通用包装，统一异常转换。
     *
     * @param name 任务名称（日志用）
     * @param call 实际提交调用
     * @return 任务 jobId
     *
     * @author cy
     * Copyright (c) CY
     */
    private String submit(String name, ExportCall call) {
        try {
            return call.invoke();
        } catch (WxErrorException e) {
            log.error("export {} failed: {}", name, e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 函数式接口：封装一次受检的导出提交调用。
     *
     * @author cy
     * Copyright (c) CY
     */
    @FunctionalInterface
    private interface ExportCall {
        /**
         * 执行导出提交调用。
         *
         * @return 任务 jobId
         * @throws WxErrorException WxJava 受检异常
         */
        String invoke() throws WxErrorException;
    }
}
