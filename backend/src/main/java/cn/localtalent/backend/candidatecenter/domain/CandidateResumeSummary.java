package cn.localtalent.backend.candidatecenter.domain;

public record CandidateResumeSummary(
        int completionPercent,
        String updatedAt,
        String skillsSummary
) {

    public static CandidateResumeSummary empty() {
        return new CandidateResumeSummary(0, "", "");
    }
}
