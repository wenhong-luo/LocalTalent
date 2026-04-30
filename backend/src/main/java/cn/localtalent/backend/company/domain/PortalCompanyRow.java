package cn.localtalent.backend.company.domain;

import java.time.LocalDateTime;

public record PortalCompanyRow(
        long companyId,
        String companyName,
        String cityCode,
        String industryCode,
        String natureCode,
        String scaleCode,
        String companyProfile,
        long openJobCount,
        LocalDateTime updatedAt
) {
}
