package cn.localtalent.backend.company.workbench.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class CompanyWorkbenchDtos {

    private CompanyWorkbenchDtos() {
    }

    public record FeatureResponse(
            @JsonProperty("company_workbench_enabled") boolean companyWorkbenchEnabled
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
            @JsonProperty("company_profile") String companyProfile
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
}
