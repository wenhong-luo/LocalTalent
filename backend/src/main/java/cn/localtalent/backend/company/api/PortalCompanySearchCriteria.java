package cn.localtalent.backend.company.api;

public record PortalCompanySearchCriteria(
        String keyword,
        String cityCode,
        String industryCode,
        String natureCode,
        String scaleCode,
        String verified
) {
}
