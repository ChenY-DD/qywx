package org.cy.qywx.util.export;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.export.WxCpExportRequest;
import me.chanjar.weixin.cp.bean.export.WxCpExportResult;
import org.cy.qywx.exception.QywxApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * 类说明：企业微信异步批量导出工具（提交任务、查询结果、一站式提交+轮询到完成/超时、下载并解密导出文件）。
 * 提交/查询拿到加密文件下载链接（url/size/md5）后，用 {@link #downloadAndDecrypt} 下载、校验完整性
 * 并用 EncodingAESKey 解密还原原始文件（明文，通常是 JSON）。
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

    /** 下载导出文件用的 HTTP 客户端（下载链接自带 authkey，直接 GET，无需 access_token）。 */
    private final HttpClient httpClient;

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
        this(wxCpService, options,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    /**
     * 构造导出工具（可注入自定义 HttpClient，便于测试）。
     *
     * @param wxCpService 企业微信服务
     * @param options     导出查询配置
     * @param httpClient  下载导出文件用的 HTTP 客户端
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxExportUtil(WxCpService wxCpService, WxExportQueryOptions options, HttpClient httpClient) {
        this.wxCpService = wxCpService;
        this.options = options;
        this.httpClient = httpClient;
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

    // -------------------- 下载与解密 --------------------

    /**
     * 下载并解密整个导出结果的所有分片，返回各分片明文（顺序与 dataList 一致）。
     *
     * @param result         已完成（status=2）的导出结果
     * @param encodingAesKey 提交导出任务时使用的 43 位 EncodingAESKey
     * @return 各分片解密后的明文字节列表；dataList 为空时返回空列表
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<byte[]> downloadAndDecrypt(WxExportResultVO result, String encodingAesKey) {
        if (result == null || result.getDataList() == null || result.getDataList().isEmpty()) {
            return List.of();
        }
        List<byte[]> out = new ArrayList<>(result.getDataList().size());
        for (WxExportDataVO data : result.getDataList()) {
            out.add(downloadAndDecrypt(data, encodingAesKey));
        }
        return out;
    }

    /**
     * 下载单个导出分片，校验密文的 size/md5 后解密还原明文。
     *
     * @param data           导出文件信息（url/size/md5）
     * @param encodingAesKey 提交导出任务时使用的 43 位 EncodingAESKey
     * @return 解密后的明文字节（通常是 JSON）
     *
     * @author cy
     * Copyright (c) CY
     */
    public byte[] downloadAndDecrypt(WxExportDataVO data, String encodingAesKey) {
        if (data == null || data.getUrl() == null || data.getUrl().isBlank()) {
            throw new IllegalArgumentException("export data url must not be blank");
        }
        byte[] encrypted = download(data.getUrl());
        verifyIntegrity(encrypted, data);
        return decrypt(encrypted, encodingAesKey);
    }

    /**
     * 用 EncodingAESKey 解密企业微信导出文件密文，还原明文。
     * <p>算法：{@code AESKey = Base64Decode(encodingAesKey + "=")}（32 字节 AES-256），
     * AES/CBC/PKCS#7，IV 取 AESKey 前 16 字节。
     *
     * @param encrypted      加密文件的原始字节
     * @param encodingAesKey 提交导出任务时使用的 43 位 EncodingAESKey
     * @return 解密后的明文字节
     *
     * @author cy
     * Copyright (c) CY
     */
    public static byte[] decrypt(byte[] encrypted, String encodingAesKey) {
        if (encrypted == null || encrypted.length == 0) {
            throw new IllegalArgumentException("encrypted content must not be empty");
        }
        if (encodingAesKey == null || encodingAesKey.length() != ENCODING_AES_KEY_LENGTH) {
            throw new IllegalArgumentException("encodingAesKey must be " + ENCODING_AES_KEY_LENGTH + " chars");
        }
        try {
            byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new IvParameterSpec(Arrays.copyOf(aesKey, 16)));
            return cipher.doFinal(encrypted);
        } catch (GeneralSecurityException e) {
            throw new QywxApiException("export file decrypt failed: " + e.getMessage(), null, "decrypt failed", e);
        }
    }

    /**
     * 下载导出文件密文（下载链接自带 authkey，直接 GET，无需 access_token）。
     *
     * @param url 加密文件下载链接
     * @return 密文字节
     *
     * @author cy
     * Copyright (c) CY
     */
    private byte[] download(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new QywxApiException("export file download failed: HTTP " + response.statusCode(),
                        null, "download failed");
            }
            return response.body();
        } catch (IOException e) {
            throw new QywxApiException("export file download failed: " + e.getMessage(), null, "download failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QywxApiException("export file download interrupted", null, "download interrupted", e);
        }
    }

    /**
     * 校验下载到的密文与 WeCom 返回的 size/md5 是否一致（确认下载完整性，校验对象是密文）。
     *
     * @param encrypted 下载到的密文
     * @param data      导出文件信息（含 size/md5）
     *
     * @author cy
     * Copyright (c) CY
     */
    private static void verifyIntegrity(byte[] encrypted, WxExportDataVO data) {
        if (data.getSize() != null && encrypted.length != data.getSize()) {
            throw new QywxApiException(
                    "export file size mismatch: expected=" + data.getSize() + ", actual=" + encrypted.length,
                    null, "size mismatch");
        }
        if (data.getMd5() != null && !data.getMd5().isBlank()) {
            String actual = md5Hex(encrypted);
            if (!actual.equalsIgnoreCase(data.getMd5())) {
                throw new QywxApiException(
                        "export file md5 mismatch: expected=" + data.getMd5() + ", actual=" + actual,
                        null, "md5 mismatch");
            }
        }
    }

    /**
     * 计算字节数组的 MD5 十六进制摘要（小写）。
     *
     * @param bytes 字节
     * @return MD5 十六进制字符串
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String md5Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
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
