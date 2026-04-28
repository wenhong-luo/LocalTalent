package cn.localtalent.backend.candidatecenter.domain;

public record CandidateSigninSummary(
        String latestStatus,
        String latestTime
) {

    public static CandidateSigninSummary empty() {
        return new CandidateSigninSummary("暂无签到", "");
    }
}
