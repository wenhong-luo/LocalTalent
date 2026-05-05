package cn.localtalent.backend.candidatecenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class CandidateCenterDtos {

    private CandidateCenterDtos() {
    }

    public record FeatureResponse(
            @JsonProperty("candidate_closure_enabled") boolean candidateClosureEnabled
    ) {
    }

    public record PrivateStatsResponse(
            @JsonProperty("favorite_count") long favoriteCount,
            @JsonProperty("subscription_count") long subscriptionCount,
            @JsonProperty("unread_notification_count") long unreadNotificationCount
    ) {
    }

    public record ResumeResponse(
            @JsonProperty("resume_id") Long resumeId,
            @JsonProperty("resume_status") String resumeStatus,
            @JsonProperty("completion_percent") int completionPercent,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("resume_name") String resumeName,
            @JsonProperty("base_profile") BaseProfileResponse baseProfile,
            List<String> education,
            List<String> experience,
            List<String> skills,
            @JsonProperty("has_attachment") boolean hasAttachment
    ) {
    }

    public record BaseProfileResponse(
            @JsonProperty("display_name") String displayName,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("category_code") String categoryCode,
            @JsonProperty("experience_years") Integer experienceYears,
            @JsonProperty("summary") String summary
    ) {
    }

    public record ResumeSaveRequest(
            @JsonProperty("resume_name") String resumeName,
            @JsonProperty("base_profile") BaseProfileRequest baseProfile,
            List<String> education,
            List<String> experience,
            List<String> skills
    ) {
    }

    public record BaseProfileRequest(
            @JsonProperty("display_name") String displayName,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("category_code") String categoryCode,
            @JsonProperty("experience_years") Integer experienceYears,
            @JsonProperty("summary") String summary
    ) {
    }

    public record ApplicationItemResponse(
            @JsonProperty("application_id") long applicationId,
            @JsonProperty("job_id") long jobId,
            @JsonProperty("job_title") String jobTitle,
            @JsonProperty("company_name") String companyName,
            @JsonProperty("application_status") int applicationStatus,
            @JsonProperty("status_label") String statusLabel,
            @JsonProperty("apply_time") String applyTime
    ) {
    }

    public record ApplicationPageResponse(
            @JsonProperty("application_list") List<ApplicationItemResponse> applicationList,
            long total
    ) {
    }

    public record FavoriteCreateRequest(
            @JsonProperty("job_id") Long jobId
    ) {
    }

    public record FavoriteItemResponse(
            @JsonProperty("favorite_id") long favoriteId,
            @JsonProperty("job_id") long jobId,
            @JsonProperty("job_title") String jobTitle,
            @JsonProperty("company_name") String companyName,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("category_code") String categoryCode,
            @JsonProperty("favorite_status") String favoriteStatus,
            @JsonProperty("created_at") String createdAt
    ) {
    }

    public record FavoritePageResponse(
            @JsonProperty("favorite_list") List<FavoriteItemResponse> favoriteList,
            long total
    ) {
    }

    public record SubscriptionCreateRequest(
            @JsonProperty("subscription_name") String subscriptionName,
            String keyword,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("category_code") String categoryCode,
            @JsonProperty("salary_min") Integer salaryMin,
            @JsonProperty("salary_max") Integer salaryMax
    ) {
    }

    public record SubscriptionItemResponse(
            @JsonProperty("subscription_id") long subscriptionId,
            @JsonProperty("subscription_name") String subscriptionName,
            String keyword,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("category_code") String categoryCode,
            @JsonProperty("salary_min") Integer salaryMin,
            @JsonProperty("salary_max") Integer salaryMax,
            @JsonProperty("subscription_status") String subscriptionStatus,
            @JsonProperty("updated_at") String updatedAt
    ) {
    }

    public record SubscriptionPageResponse(
            @JsonProperty("subscription_list") List<SubscriptionItemResponse> subscriptionList,
            long total
    ) {
    }

    public record NotificationItemResponse(
            @JsonProperty("notification_id") long notificationId,
            @JsonProperty("notification_type") String notificationType,
            String title,
            @JsonProperty("content_summary") String contentSummary,
            @JsonProperty("read_status") String readStatus,
            @JsonProperty("created_at") String createdAt
    ) {
    }

    public record NotificationPageResponse(
            @JsonProperty("notification_list") List<NotificationItemResponse> notificationList,
            long total
    ) {
    }
}
