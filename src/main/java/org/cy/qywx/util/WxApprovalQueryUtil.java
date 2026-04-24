package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalInfo;
import org.cy.qywx.vo.WxApprovalDetailVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
public class WxApprovalQueryUtil {

    private static final long SEGMENT_DAYS_MILLIS = 29L * 24 * 60 * 60 * 1000;

    private final WxCpService wxCpService;

    public WxApprovalQueryUtil(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
    }

    public List<String> getApprovalSpNos(Date startTime, Date endTime) throws WxErrorException {
        validateTimeRange(startTime, endTime);

        Set<String> spNos = new LinkedHashSet<>();
        Date segmentStartTime = startTime;

        while (!segmentStartTime.after(endTime)) {
            Date segmentEndTime = new Date(Math.min(
                    segmentStartTime.getTime() + SEGMENT_DAYS_MILLIS,
                    endTime.getTime()
            ));

            Integer cursor = 0;
            while (true) {
                WxCpApprovalInfo approvalInfo = wxCpService.getOaService()
                        .getApprovalInfo(segmentStartTime, segmentEndTime, cursor, 100, null);

                if (approvalInfo.getSpNoList() != null && !approvalInfo.getSpNoList().isEmpty()) {
                    spNos.addAll(approvalInfo.getSpNoList());
                }

                Integer nextCursor = approvalInfo.getNextCursor();
                if (nextCursor == null || nextCursor.equals(cursor)) {
                    break;
                }
                cursor = nextCursor;
            }

            if (segmentEndTime.equals(endTime)) {
                break;
            }
            segmentStartTime = new Date(segmentEndTime.getTime() + 1000);
        }

        return new ArrayList<>(spNos);
    }

    public List<WxApprovalDetailVO> getApprovalDetails(Date startTime, Date endTime) throws WxErrorException {
        List<String> spNos = getApprovalSpNos(startTime, endTime);
        if (spNos.isEmpty()) {
            return List.of();
        }

        int threadCount = Math.min(8, Math.max(2, spNos.size()));
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<CompletableFuture<WxApprovalDetailVO>> futures = spNos.stream()
                    .map(spNo -> CompletableFuture.supplyAsync(() -> fetchApprovalDetail(spNo), executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        } finally {
            executor.shutdown();
        }
    }

    public Map<String, List<WxApprovalDetailVO>> getApprovalDetailsGroupByTemplateId(Date startTime, Date endTime)
            throws WxErrorException {
        List<WxApprovalDetailVO> details = getApprovalDetails(startTime, endTime);
        return details.stream()
                .collect(Collectors.groupingBy(
                        detail -> detail.getTemplateId() == null ? "UNKNOWN_TEMPLATE" : detail.getTemplateId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private WxApprovalDetailVO fetchApprovalDetail(String spNo) {
        try {
            WxCpApprovalDetailResult detail = wxCpService.getOaService().getApprovalDetail(spNo);
            return WxApprovalConverter.from(detail);
        } catch (WxErrorException e) {
            throw new CompletionException(e);
        }
    }

    private void validateTimeRange(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime 和 endTime 不能为空");
        }
        if (!startTime.before(endTime)) {
            throw new IllegalArgumentException("startTime 必须早于 endTime");
        }
    }
}
