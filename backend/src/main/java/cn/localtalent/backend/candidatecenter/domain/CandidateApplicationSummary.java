package cn.localtalent.backend.candidatecenter.domain;

public record CandidateApplicationSummary(
        long total,
        Integer latestStatus,
        String latestJobTitle
) {

    public static CandidateApplicationSummary empty() {
        return new CandidateApplicationSummary(0, null, "暂无");
    }
}
