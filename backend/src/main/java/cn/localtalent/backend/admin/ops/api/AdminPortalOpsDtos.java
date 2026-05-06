package cn.localtalent.backend.admin.ops.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminPortalOpsDtos {

    private AdminPortalOpsDtos() {
    }

    public record FeatureResponse(
            @JsonProperty("operator_portal_ops_enabled") boolean operatorPortalOpsEnabled
    ) {
    }

    public record OpsOverviewResponse(
            FeatureResponse features,
            @JsonProperty("pending_company_count") long pendingCompanyCount,
            @JsonProperty("pending_job_count") long pendingJobCount,
            @JsonProperty("pending_export_count") long pendingExportCount,
            @JsonProperty("published_content_count") long publishedContentCount,
            @JsonProperty("published_event_count") long publishedEventCount,
            @JsonProperty("active_recommendation_count") long activeRecommendationCount,
            @JsonProperty("pending_risk_count") long pendingRiskCount,
            @JsonProperty("recent_audit_count") long recentAuditCount
    ) {
    }

    public record RecommendationRequest(
            @JsonProperty("slot_code") String slotCode,
            @JsonProperty("target_type") String targetType,
            @JsonProperty("target_id") Long targetId,
            @JsonProperty("title_override") String titleOverride,
            @JsonProperty("summary_override") String summaryOverride,
            @JsonProperty("display_order") Integer displayOrder,
            Integer status,
            @JsonProperty("start_time") LocalDateTime startTime,
            @JsonProperty("end_time") LocalDateTime endTime
    ) {
    }

    public record RecommendationResponse(
            @JsonProperty("recommendation_id") long recommendationId,
            @JsonProperty("slot_code") String slotCode,
            @JsonProperty("target_type") String targetType,
            @JsonProperty("target_id") long targetId,
            @JsonProperty("title_override") String titleOverride,
            @JsonProperty("summary_override") String summaryOverride,
            @JsonProperty("display_order") int displayOrder,
            int status,
            @JsonProperty("start_time") LocalDateTime startTime,
            @JsonProperty("end_time") LocalDateTime endTime,
            @JsonProperty("target_valid") boolean targetValid,
            @JsonProperty("invalid_reason") String invalidReason,
            @JsonProperty("updated_at") LocalDateTime updatedAt
    ) {
    }

    public record RecommendationPageResponse(
            @JsonProperty("recommendation_list") List<RecommendationResponse> recommendationList,
            long total,
            int page,
            int size
    ) {
    }

    public record RiskReviewResponse(
            @JsonProperty("risk_id") long riskId,
            @JsonProperty("risk_type") String riskType,
            @JsonProperty("target_type") String targetType,
            @JsonProperty("target_id") long targetId,
            String severity,
            int status,
            String title,
            String summary,
            String decision,
            @JsonProperty("handler_id") Long handlerId,
            @JsonProperty("handled_at") LocalDateTime handledAt,
            @JsonProperty("created_at") LocalDateTime createdAt,
            @JsonProperty("updated_at") LocalDateTime updatedAt
    ) {
    }

    public record RiskReviewPageResponse(
            @JsonProperty("risk_review_list") List<RiskReviewResponse> riskReviewList,
            long total,
            int page,
            int size
    ) {
    }

    public record RiskHandleRequest(
            int status,
            String decision
    ) {
    }
}
