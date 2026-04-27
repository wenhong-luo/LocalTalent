package cn.localtalent.backend.candidate.domain;

public record CandidateProfileRaw(
        long candidateId,
        String realName,
        boolean realnameVerified,
        String baseProfileJson,
        String skillsJson
) {
}
