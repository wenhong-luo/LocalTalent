package cn.localtalent.backend.job.application;

import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.job.api.PortalJobPageResponse;
import cn.localtalent.backend.job.api.PortalJobResponse;
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

    public PortalJobPageResponse list(String keyword, String cityCode, String categoryCode, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<PortalJobResponse> items = jobRepository
                .listVisible(keyword, cityCode, categoryCode, normalizedSize, offset)
                .stream()
                .map(this::response)
                .toList();
        return new PortalJobPageResponse(items, jobRepository.countVisible(keyword, cityCode, categoryCode));
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
}
