package cn.localtalent.backend.candidate.domain;

public record ConsentRecord(
        long id,
        long candidateId,
        int consentStatus,
        int revokeStatus
) {

    public boolean revoked() {
        return consentStatus == 2 || revokeStatus == 1;
    }
}
