package cn.localtalent.backend.company.workbench.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class CompanyWorkbenchDtos {

    private CompanyWorkbenchDtos() {
    }

    public record FeatureResponse(
            @JsonProperty("company_workbench_enabled") boolean companyWorkbenchEnabled,
            @JsonProperty("company_style_upload_enabled") boolean companyStyleUploadEnabled,
            @JsonProperty("company_logo_upload_enabled") boolean companyLogoUploadEnabled
    ) {
    }

    public record OverviewResponse(
            CompanyProfileResponse profile,
            WorkbenchStatsResponse stats,
            FeatureResponse features
    ) {
    }

    public record WorkbenchStatsResponse(
            @JsonProperty("job_total") long jobTotal,
            @JsonProperty("application_total") long applicationTotal,
            @JsonProperty("pending_application_total") long pendingApplicationTotal,
            @JsonProperty("interview_total") long interviewTotal,
            @JsonProperty("export_total") long exportTotal
    ) {
    }

    public record CompanyProfileResponse(
            @JsonProperty("company_id") long companyId,
            @JsonProperty("company_name") String companyName,
            @JsonProperty("industry_code") String industryCode,
            @JsonProperty("nature_code") String natureCode,
            @JsonProperty("scale_code") String scaleCode,
            @JsonProperty("city_code") String cityCode,
            String address,
            @JsonProperty("company_profile") String companyProfile,
            @JsonProperty("registered_capital_amount") String registeredCapitalAmount,
            @JsonProperty("registered_capital_unit") String registeredCapitalUnit,
            @JsonProperty("website_url") String websiteUrl,
            @JsonProperty("benefit_codes") List<String> benefitCodes,
            @JsonProperty("contact_name") String contactName,
            @JsonProperty("contact_mobile") String contactMobile,
            @JsonProperty("contact_mobile_hidden") boolean contactMobileHidden,
            @JsonProperty("contact_wechat") String contactWechat,
            @JsonProperty("contact_wechat_same_mobile") boolean contactWechatSameMobile,
            @JsonProperty("contact_phone") String contactPhone,
            @JsonProperty("contact_email") String contactEmail,
            @JsonProperty("contact_qq") String contactQq,
            @JsonProperty("auth_status") int authStatus,
            @JsonProperty("reject_reason") String rejectReason,
            @JsonProperty("certification_material_summary") Map<String, Object> certificationMaterialSummary,
            @JsonProperty("updated_at") LocalDateTime updatedAt
    ) {
    }

    public record CompanyProfileSaveRequest(
            @JsonProperty("company_name") String companyName,
            @JsonProperty("industry_code") String industryCode,
            @JsonProperty("nature_code") String natureCode,
            @JsonProperty("scale_code") String scaleCode,
            @JsonProperty("city_code") String cityCode,
            String address,
            @JsonProperty("company_profile") String companyProfile,
            @JsonProperty("registered_capital_amount") String registeredCapitalAmount,
            @JsonProperty("registered_capital_unit") String registeredCapitalUnit,
            @JsonProperty("website_url") String websiteUrl,
            @JsonProperty("benefit_codes") List<String> benefitCodes,
            @JsonProperty("contact_name") String contactName,
            @JsonProperty("contact_mobile") String contactMobile,
            @JsonProperty("contact_mobile_hidden") Boolean contactMobileHidden,
            @JsonProperty("contact_wechat") String contactWechat,
            @JsonProperty("contact_wechat_same_mobile") Boolean contactWechatSameMobile,
            @JsonProperty("contact_phone") String contactPhone,
            @JsonProperty("contact_email") String contactEmail,
            @JsonProperty("contact_qq") String contactQq
    ) {
    }

    public record CertificationSubmitRequest(
            @JsonProperty("company_name") String companyName,
            @JsonProperty("license_no") String licenseNo,
            @JsonProperty("industry_code") String industryCode,
            @JsonProperty("nature_code") String natureCode,
            @JsonProperty("scale_code") String scaleCode,
            @JsonProperty("city_code") String cityCode,
            String address,
            @JsonProperty("company_profile") String companyProfile,
            @JsonProperty("registered_capital_amount") String registeredCapitalAmount,
            @JsonProperty("registered_capital_unit") String registeredCapitalUnit,
            @JsonProperty("website_url") String websiteUrl,
            @JsonProperty("benefit_codes") List<String> benefitCodes,
            @JsonProperty("contact_name") String contactName,
            @JsonProperty("contact_mobile") String contactMobile,
            @JsonProperty("contact_mobile_hidden") Boolean contactMobileHidden,
            @JsonProperty("contact_wechat") String contactWechat,
            @JsonProperty("contact_wechat_same_mobile") Boolean contactWechatSameMobile,
            @JsonProperty("contact_phone") String contactPhone,
            @JsonProperty("contact_email") String contactEmail,
            @JsonProperty("contact_qq") String contactQq,
            @JsonProperty("certification_material_summary") Map<String, Object> certificationMaterialSummary
    ) {
    }

    public record ApplicationPageResponse(
            @JsonProperty("application_list") List<ApplicationItemResponse> applicationList,
            long total
    ) {
    }

    public record ApplicationItemResponse(
            @JsonProperty("application_id") long applicationId,
            @JsonProperty("job_id") long jobId,
            @JsonProperty("job_title") String jobTitle,
            @JsonProperty("application_status") int applicationStatus,
            @JsonProperty("status_label") String statusLabel,
            @JsonProperty("apply_time") LocalDateTime applyTime,
            @JsonProperty("display_name_masked") String displayNameMasked,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("skills_summary") String skillsSummary,
            @JsonProperty("experience_years") Integer experienceYears,
            @JsonProperty("has_resume_attachment") boolean hasResumeAttachment,
            @JsonProperty("company_stage_note") String companyStageNote,
            @JsonProperty("stage_changed_at") LocalDateTime stageChangedAt
    ) {
    }

    public record ApplicationStageRequest(
            String stage,
            String note
    ) {
    }

    public record InterviewSessionPageResponse(
            @JsonProperty("session_list") List<InterviewSessionItemResponse> sessionList,
            long total
    ) {
    }

    public record InterviewSessionItemResponse(
            @JsonProperty("session_id") long sessionId,
            @JsonProperty("application_id") Long applicationId,
            @JsonProperty("job_id") long jobId,
            @JsonProperty("job_title") String jobTitle,
            int status,
            @JsonProperty("session_name") String sessionName,
            @JsonProperty("session_time") LocalDateTime sessionTime,
            String location
    ) {
    }

    public record CompanyStyleImagePageResponse(
            @JsonProperty("image_list") List<CompanyStyleImageResponse> imageList,
            long total
    ) {
    }

    public record CompanyStyleImageResponse(
            @JsonProperty("image_id") long imageId,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("content_type") String contentType,
            @JsonProperty("size_bytes") long sizeBytes,
            @JsonProperty("display_order") int displayOrder,
            int status,
            @JsonProperty("review_status") int reviewStatus,
            @JsonProperty("uploaded_at") LocalDateTime uploadedAt,
            @JsonProperty("content_url") String contentUrl
    ) {
    }

    public record CompanyStyleImageOrderRequest(
            @JsonProperty("image_ids") List<Long> imageIds
    ) {
    }

    public record CompanyStyleImageDownload(
            String fileName,
            String contentType,
            byte[] content
    ) {
    }

    public record CompanyLogoResponse(
            @JsonProperty("has_logo") boolean hasLogo,
            @JsonProperty("logo_status") String logoStatus,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("content_type") String contentType,
            @JsonProperty("size_bytes") Long sizeBytes,
            @JsonProperty("uploaded_at") LocalDateTime uploadedAt,
            @JsonProperty("content_url") String contentUrl
    ) {
    }

    public record CompanyLogoDownload(
            String fileName,
            String contentType,
            byte[] content
    ) {
    }

    public record JobDeleteRequest(
            @JsonProperty("reason") String reason
    ) {
    }

    public record JobRestoreRequest(
            @JsonProperty("reason") String reason
    ) {
    }

    public record ResumeSearchPageResponse(
            @JsonProperty("snapshot_list") List<ResumeSearchItemResponse> snapshotList,
            long total,
            int page,
            int size
    ) {
    }

    public record ResumeSearchItemResponse(
            @JsonProperty("snapshot_id") long snapshotId,
            @JsonProperty("display_name_masked") String displayNameMasked,
            @JsonProperty("age_band") String ageBand,
            String gender,
            @JsonProperty("education_code") String educationCode,
            @JsonProperty("highest_education") String highestEducation,
            @JsonProperty("experience_years") Integer experienceYears,
            @JsonProperty("expected_positions") List<String> expectedPositions,
            @JsonProperty("expected_cities") List<String> expectedCities,
            @JsonProperty("expected_salary") String expectedSalary,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("category_code") String categoryCode,
            @JsonProperty("industry_code") String industryCode,
            @JsonProperty("major_name") String majorName,
            @JsonProperty("work_nature") String workNature,
            @JsonProperty("resume_tags") List<String> resumeTags,
            @JsonProperty("skills_summary") String skillsSummary,
            @JsonProperty("updated_at") String updatedAt
    ) {
    }

    public record ResumeSearchDetailResponse(
            @JsonProperty("snapshot_id") long snapshotId,
            @JsonProperty("display_name_masked") String displayNameMasked,
            @JsonProperty("age_band") String ageBand,
            String gender,
            @JsonProperty("education_code") String educationCode,
            @JsonProperty("highest_education") String highestEducation,
            @JsonProperty("experience_years") Integer experienceYears,
            @JsonProperty("expected_positions") List<String> expectedPositions,
            @JsonProperty("expected_cities") List<String> expectedCities,
            @JsonProperty("expected_salary") String expectedSalary,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("category_code") String categoryCode,
            @JsonProperty("industry_code") String industryCode,
            @JsonProperty("major_name") String majorName,
            @JsonProperty("work_nature") String workNature,
            @JsonProperty("resume_tags") List<String> resumeTags,
            @JsonProperty("skills_summary") String skillsSummary,
            @JsonProperty("education_summary") String educationSummary,
            @JsonProperty("experience_summary") String experienceSummary,
            @JsonProperty("self_description_summary") String selfDescriptionSummary,
            @JsonProperty("contact_access_hint") String contactAccessHint,
            @JsonProperty("updated_at") String updatedAt
    ) {
    }

    public record ResumeSnapshotReportRequest(
            @JsonProperty("reason_code") String reasonCode,
            String remark
    ) {
    }

    public record ResumeSnapshotReportResponse(
            @JsonProperty("report_id") long reportId,
            @JsonProperty("snapshot_id") long snapshotId,
            @JsonProperty("report_status") String reportStatus,
            String message
    ) {
    }

    public record ResumeAccessRequestPayload(
            @JsonProperty("request_type") String requestType,
            String reason
    ) {
    }

    public record ResumeAccessRequestResponse(
            @JsonProperty("request_id") long requestId,
            @JsonProperty("request_type") String requestType,
            String status,
            @JsonProperty("created_at") String createdAt
    ) {
    }
}
