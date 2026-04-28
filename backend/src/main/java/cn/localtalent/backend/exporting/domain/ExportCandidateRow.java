package cn.localtalent.backend.exporting.domain;

import java.time.LocalDateTime;

public record ExportCandidateRow(
        long applicationId,
        String jobTitle,
        int applicationStatus,
        LocalDateTime applyTime,
        String displayName,
        String contactMobile,
        String contactEmail,
        String skillsSummary
) {
}
