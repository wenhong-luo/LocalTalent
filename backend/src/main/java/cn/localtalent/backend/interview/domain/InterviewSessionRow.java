package cn.localtalent.backend.interview.domain;

import java.time.LocalDateTime;

public record InterviewSessionRow(
        long sessionId,
        Long applicationId,
        long jobId,
        long companyId,
        Long candidateId,
        String sessionName,
        LocalDateTime sessionTime,
        String location,
        String signinCodeHash,
        LocalDateTime signinCodeExpiresAt,
        LocalDateTime signinCodeUsedAt,
        int status,
        Integer applicationStatus
) {
}
