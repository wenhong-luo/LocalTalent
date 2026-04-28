package cn.localtalent.backend.openapi;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class OpenApiRetryQueueService {

    private final OpenApiJdbcRepository repository;

    public OpenApiRetryQueueService(OpenApiJdbcRepository repository) {
        this.repository = repository;
    }

    public int retryDueTasks(LocalDateTime now) {
        return repository.retryDueTasks(now);
    }
}
