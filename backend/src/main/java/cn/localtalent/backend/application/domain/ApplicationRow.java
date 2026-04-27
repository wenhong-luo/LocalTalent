package cn.localtalent.backend.application.domain;

import java.time.LocalDateTime;

public record ApplicationRow(
        long applicationId,
        long jobId,
        long companyId,
        long candidateId,
        Long resumeId,
        int sourceType,
        int status,
        LocalDateTime applyTime,
        LocalDateTime updatedAt,
        String jobTitle,
        String companyName
) {
}
