package cn.localtalent.backend.company.domain;

import java.time.LocalDateTime;

public record PortalCompanyJobRow(
        long jobId,
        String title,
        String categoryCode,
        String cityCode,
        Integer salaryMin,
        Integer salaryMax,
        LocalDateTime updatedAt
) {
}
