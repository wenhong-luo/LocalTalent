package cn.localtalent.backend.company.application;

import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.company.api.PortalCompanyJobResponse;
import cn.localtalent.backend.company.api.PortalCompanyPageResponse;
import cn.localtalent.backend.company.api.PortalCompanyResponse;
import cn.localtalent.backend.company.api.PortalCompanySearchCriteria;
import cn.localtalent.backend.company.domain.PortalCompanyJobRow;
import cn.localtalent.backend.company.domain.PortalCompanyRow;
import cn.localtalent.backend.company.infrastructure.PortalCompanyJdbcRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PortalCompanyService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DETAIL_JOB_LIMIT = 20;

    private final PortalCompanyJdbcRepository companyRepository;

    public PortalCompanyService(PortalCompanyJdbcRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public PortalCompanyPageResponse list(PortalCompanySearchCriteria criteria, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        PortalCompanySearchCriteria normalizedCriteria = normalize(criteria);
        List<PortalCompanyResponse> items = companyRepository
                .listVisible(normalizedCriteria, normalizedSize, offset)
                .stream()
                .map(row -> response(row, List.of()))
                .toList();
        return new PortalCompanyPageResponse(items, companyRepository.countVisible(normalizedCriteria));
    }

    public PortalCompanyResponse get(long companyId) {
        PortalCompanyRow row = companyRepository.findVisibleById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "company not found"));
        return response(row, companyRepository.listVisibleJobs(companyId, DETAIL_JOB_LIMIT));
    }

    private PortalCompanySearchCriteria normalize(PortalCompanySearchCriteria criteria) {
        if (criteria == null) {
            return new PortalCompanySearchCriteria(null, null, null, null, null, null);
        }
        return new PortalCompanySearchCriteria(
                criteria.keyword(),
                criteria.cityCode(),
                criteria.industryCode(),
                criteria.natureCode(),
                criteria.scaleCode(),
                criteria.verified());
    }

    private PortalCompanyResponse response(PortalCompanyRow row, List<PortalCompanyJobRow> jobs) {
        return new PortalCompanyResponse(
                row.companyId(),
                row.companyName(),
                row.cityCode(),
                row.industryCode(),
                row.natureCode(),
                row.scaleCode(),
                true,
                row.companyProfile(),
                row.openJobCount(),
                row.updatedAt(),
                jobs.stream().map(this::jobResponse).toList());
    }

    private PortalCompanyJobResponse jobResponse(PortalCompanyJobRow row) {
        return new PortalCompanyJobResponse(
                row.jobId(),
                row.title(),
                row.categoryCode(),
                row.cityCode(),
                row.salaryMin(),
                row.salaryMax(),
                row.updatedAt());
    }
}
