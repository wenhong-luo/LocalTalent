package cn.localtalent.backend.job.api;

public record PortalJobSearchCriteria(
        String keyword,
        String cityCode,
        String categoryCode,
        Integer salaryMin,
        Integer salaryMax,
        String industryCode,
        String scaleCode,
        Integer updatedWithin,
        String sort
) {
}
