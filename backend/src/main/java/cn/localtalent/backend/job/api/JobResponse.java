package cn.localtalent.backend.job.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record JobResponse(
        @JsonProperty("job_id") long jobId,
        @JsonProperty("company_id") long companyId,
        String title,
        @JsonProperty("job_nature_code") String jobNatureCode,
        @JsonProperty("category_code") String categoryCode,
        @JsonProperty("category_name") String categoryName,
        @JsonProperty("experience_code") String experienceCode,
        @JsonProperty("education_code") String educationCode,
        @JsonProperty("recruit_count") Integer recruitCount,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("work_region_path") String workRegionPath,
        String address,
        @JsonProperty("salary_min") Integer salaryMin,
        @JsonProperty("salary_max") Integer salaryMax,
        @JsonProperty("salary_negotiable") boolean salaryNegotiable,
        @JsonProperty("welfare_codes") List<String> welfareCodes,
        @JsonProperty("department_name") String departmentName,
        @JsonProperty("age_min") Integer ageMin,
        @JsonProperty("age_max") Integer ageMax,
        @JsonProperty("age_unlimited") boolean ageUnlimited,
        @JsonProperty("recruitment_time_code") String recruitmentTimeCode,
        @JsonProperty("contact_mode") String contactMode,
        @JsonProperty("contact_name") String contactName,
        @JsonProperty("contact_mobile") String contactMobile,
        @JsonProperty("contact_phone") String contactPhone,
        @JsonProperty("contact_email") String contactEmail,
        @JsonProperty("contact_wechat") String contactWechat,
        @JsonProperty("contact_hidden") boolean contactHidden,
        @JsonProperty("notify_enabled") boolean notifyEnabled,
        @JsonProperty("resume_subscription_enabled") boolean resumeSubscriptionEnabled,
        @JsonProperty("job_desc") String jobDesc,
        int status,
        @JsonProperty("audit_status") int auditStatus,
        @JsonProperty("reject_reason") String rejectReason,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("deleted_at") LocalDateTime deletedAt,
        @JsonProperty("delete_reason") String deleteReason
) {
}
