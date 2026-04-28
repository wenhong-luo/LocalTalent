package cn.localtalent.backend.candidatecenter.domain;

public record CandidateConsentSummary(
        Long consentId,
        String publishStatus,
        int publishableFlag,
        String statusLabel,
        String reason,
        String updatedAt
) {
}
