package cn.localtalent.backend.company.domain;

import java.time.LocalDateTime;

public record CompanyReviewRow(
        long companyId,
        String companyName,
        String licenseNo,
        String industryCode,
        String scaleCode,
        String cityCode,
        String address,
        String companyProfile,
        int authStatus,
        String authRejectReason,
        Long authReviewUserId,
        LocalDateTime authReviewTime,
        LocalDateTime authSubmitTime
) {
}
