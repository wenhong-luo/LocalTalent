package cn.localtalent.backend.candidatecenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class CandidateCenterDtos {

    private CandidateCenterDtos() {
    }

    public record FeatureResponse(
            @JsonProperty("candidate_closure_enabled") boolean candidateClosureEnabled,
            @JsonProperty("resume_attachment_upload_enabled") boolean resumeAttachmentUploadEnabled,
            @JsonProperty("resume_ai_assist_enabled") boolean resumeAiAssistEnabled
    ) {
    }

    public record OnboardingResponse(
            @JsonProperty("onboarding_required") boolean onboardingRequired,
            @JsonProperty("onboarding_step") String onboardingStep,
            @JsonProperty("publish_status") String publishStatus
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
            @JsonProperty("work_experience") List<WorkExperienceResponse> workExperience,
            @JsonProperty("education_experience") List<EducationExperienceResponse> educationExperience,
            @JsonProperty("self_description") String selfDescription,
            @JsonProperty("has_attachment") boolean hasAttachment
    ) {
    }

    public record AttachmentResponse(
            @JsonProperty("has_attachment") boolean hasAttachment,
            @JsonProperty("attachment_status") String attachmentStatus,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("content_type") String contentType,
            @JsonProperty("size_bytes") Long sizeBytes,
            @JsonProperty("uploaded_at") String uploadedAt
    ) {
        public static AttachmentResponse empty() {
            return new AttachmentResponse(false, "empty", "", "", null, "");
        }
    }

    public record AttachmentDownload(
            String fileName,
            String contentType,
            byte[] content
    ) {
    }

    public record AiSuggestionTaskResponse(
            @JsonProperty("task_id") Long taskId,
            @JsonProperty("task_status") String taskStatus,
            @JsonProperty("suggestion_count") int suggestionCount,
            @JsonProperty("applied_count") int appliedCount,
            @JsonProperty("dismissed_count") int dismissedCount,
            @JsonProperty("generated_at") String generatedAt,
            List<AiSuggestionItemResponse> items
    ) {
        public static AiSuggestionTaskResponse empty() {
            return new AiSuggestionTaskResponse(null, "empty", 0, 0, 0, "", List.of());
        }
    }

    public record AiSuggestionItemResponse(
            @JsonProperty("suggestion_id") long suggestionId,
            @JsonProperty("suggestion_type") String suggestionType,
            @JsonProperty("target_field") String targetField,
            String title,
            @JsonProperty("reason_summary") String reasonSummary,
            @JsonProperty("before_preview") String beforePreview,
            @JsonProperty("suggested_value") String suggestedValue,
            @JsonProperty("can_apply") boolean canApply,
            @JsonProperty("apply_status") String applyStatus
    ) {
    }

    public record BaseProfileResponse(
            @JsonProperty("display_name") String displayName,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("category_code") String categoryCode,
            @JsonProperty("experience_years") Integer experienceYears,
            @JsonProperty("summary") String summary,
            String gender,
            @JsonProperty("birth_date") String birthDate,
            @JsonProperty("highest_education") String highestEducation,
            @JsonProperty("start_work_date") String startWorkDate,
            @JsonProperty("no_experience") boolean noExperience,
            @JsonProperty("contact_phone") String contactPhone,
            @JsonProperty("contact_wechat") String contactWechat,
            @JsonProperty("wechat_same_as_phone") boolean wechatSameAsPhone,
            @JsonProperty("expected_positions") List<String> expectedPositions,
            @JsonProperty("expected_salary") String expectedSalary,
            @JsonProperty("expected_cities") List<String> expectedCities,
            @JsonProperty("job_status") String jobStatus
    ) {
    }

    public record ResumeSaveRequest(
            @JsonProperty("resume_name") String resumeName,
            @JsonProperty("base_profile") BaseProfileRequest baseProfile,
            List<String> education,
            List<String> experience,
            List<String> skills,
            @JsonProperty("work_experience") List<WorkExperienceRequest> workExperience,
            @JsonProperty("education_experience") List<EducationExperienceRequest> educationExperience,
            @JsonProperty("self_description") String selfDescription
    ) {
    }

    public record BaseProfileRequest(
            @JsonProperty("display_name") String displayName,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("category_code") String categoryCode,
            @JsonProperty("experience_years") Integer experienceYears,
            @JsonProperty("summary") String summary,
            String gender,
            @JsonProperty("birth_date") String birthDate,
            @JsonProperty("highest_education") String highestEducation,
            @JsonProperty("start_work_date") String startWorkDate,
            @JsonProperty("no_experience") Boolean noExperience,
            @JsonProperty("contact_phone") String contactPhone,
            @JsonProperty("contact_wechat") String contactWechat,
            @JsonProperty("wechat_same_as_phone") Boolean wechatSameAsPhone,
            @JsonProperty("expected_positions") List<String> expectedPositions,
            @JsonProperty("expected_salary") String expectedSalary,
            @JsonProperty("expected_cities") List<String> expectedCities,
            @JsonProperty("job_status") String jobStatus
    ) {
    }

    public record WorkExperienceResponse(
            @JsonProperty("company_name") String companyName,
            @JsonProperty("position_name") String positionName,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate,
            boolean ongoing,
            @JsonProperty("responsibility") String responsibility
    ) {
    }

    public record WorkExperienceRequest(
            @JsonProperty("company_name") String companyName,
            @JsonProperty("position_name") String positionName,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate,
            Boolean ongoing,
            @JsonProperty("responsibility") String responsibility
    ) {
    }

    public record EducationExperienceResponse(
            @JsonProperty("school_name") String schoolName,
            @JsonProperty("major_name") String majorName,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate,
            boolean ongoing,
            String degree
    ) {
    }

    public record EducationExperienceRequest(
            @JsonProperty("school_name") String schoolName,
            @JsonProperty("major_name") String majorName,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate,
            Boolean ongoing,
            String degree
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
