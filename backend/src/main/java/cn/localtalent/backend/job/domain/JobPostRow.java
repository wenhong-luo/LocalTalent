package cn.localtalent.backend.job.domain;

import java.time.LocalDateTime;

public record JobPostRow(
        long jobId,
        long companyId,
        String companyName,
        int companyAuthStatus,
        String title,
        String categoryCode,
        String cityCode,
        Integer salaryMin,
        Integer salaryMax,
        String jobDesc,
        int status,
        int auditStatus,
        String reviewMemo,
        String rejectReason,
        Long reviewUserId,
        LocalDateTime reviewTime,
        LocalDateTime publishedAt,
        String offlineReason,
        LocalDateTime statusChangedAt,
        LocalDateTime updatedAt
) {
}
