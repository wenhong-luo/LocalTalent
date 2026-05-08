package cn.localtalent.backend.candidatecenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CandidateCenterOverviewResponse(
        CandidateResumeSummaryResponse resume,
        CandidateApplicationSummaryResponse applications,
        CandidateSigninSummaryResponse signin,
        CandidateConsentSummaryResponse consent,
        CandidateCenterDtos.PrivateStatsResponse stats,
        CandidateCenterDtos.FeatureResponse features,
        CandidateCenterDtos.OnboardingResponse onboarding
) {

    public record CandidateResumeSummaryResponse(
            @JsonProperty("completion_percent") int completionPercent,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("skills_summary") String skillsSummary
    ) {
    }

    public record CandidateApplicationSummaryResponse(
            long total,
            @JsonProperty("latest_status") String latestStatus,
            @JsonProperty("latest_job_title") String latestJobTitle
    ) {
    }

    public record CandidateSigninSummaryResponse(
            @JsonProperty("latest_status") String latestStatus,
            @JsonProperty("latest_time") String latestTime
    ) {
    }

    public record CandidateConsentSummaryResponse(
            @JsonProperty("consent_id") Long consentId,
            @JsonProperty("publish_status") String publishStatus,
            @JsonProperty("publishable_flag") int publishableFlag,
            @JsonProperty("status_label") String statusLabel,
            String reason,
            @JsonProperty("updated_at") String updatedAt
    ) {
    }
}
