package cn.localtalent.backend.job.application;

import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.job.api.PortalJobPageResponse;
import cn.localtalent.backend.job.api.PortalJobResponse;
import cn.localtalent.backend.job.api.PortalJobSearchCriteria;
import cn.localtalent.backend.job.domain.JobPostRow;
import cn.localtalent.backend.job.infrastructure.JobJdbcRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PortalJobService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JobJdbcRepository jobRepository;

    public PortalJobService(JobJdbcRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public PortalJobPageResponse list(PortalJobSearchCriteria criteria, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        PortalJobSearchCriteria normalizedCriteria = normalize(criteria);
        List<PortalJobResponse> items = jobRepository
                .listVisible(normalizedCriteria, normalizedSize, offset)
                .stream()
                .map(this::response)
                .toList();
        return new PortalJobPageResponse(items, jobRepository.countVisible(normalizedCriteria));
    }

    public PortalJobResponse get(long jobId) {
        return jobRepository.findVisibleById(jobId)
                .map(this::response)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "job not found"));
    }

    private PortalJobResponse response(JobPostRow row) {
        return new PortalJobResponse(
                row.jobId(),
                row.title(),
                row.companyName(),
                row.categoryCode(),
                row.cityCode(),
                row.salaryMin(),
                row.salaryMax(),
                row.jobDesc(),
                row.updatedAt());
    }

    private PortalJobSearchCriteria normalize(PortalJobSearchCriteria criteria) {
        if (criteria == null) {
            return new PortalJobSearchCriteria(null, null, null, null, null, null, null, null, null);
        }
        Integer salaryMin = criteria.salaryMin() == null || criteria.salaryMin() < 0 ? null : criteria.salaryMin();
        Integer salaryMax = criteria.salaryMax() == null || criteria.salaryMax() < 0 ? null : criteria.salaryMax();
        Integer updatedWithin = criteria.updatedWithin() == null || criteria.updatedWithin() <= 0
                ? null
                : Math.min(criteria.updatedWithin(), 365);
        return new PortalJobSearchCriteria(
                criteria.keyword(),
                criteria.cityCode(),
                criteria.categoryCode(),
                salaryMin,
                salaryMax,
                criteria.industryCode(),
                criteria.scaleCode(),
                updatedWithin,
                criteria.sort());
    }
}
