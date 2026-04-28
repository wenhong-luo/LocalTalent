package cn.localtalent.backend.openapi;

import cn.localtalent.backend.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OpenApiIdempotencyService {

    private final OpenApiJdbcRepository repository;
    private final ObjectMapper objectMapper;

    public OpenApiIdempotencyService(OpenApiJdbcRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    public <T> T execute(Class<T> responseType, Supplier<OpenApiIdempotentResult<T>> action) {
        OpenApiRequestContext context = OpenApiContext.current();
        String idempotencyKey = context.idempotencyKey();
        Optional<String> storedResponse = repository.findIdempotentResponse(
                        context.apiCode(),
                        context.client(),
                        idempotencyKey,
                        context.requestHash());
        if (storedResponse.isPresent()) {
            String responseJson = storedResponse.get();
            if ("__HASH_MISMATCH__".equals(responseJson)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "IDEMPOTENCY_409",
                        "idempotency key reused with different request");
            }
            return read(responseJson, responseType);
        }

        OpenApiIdempotentResult<T> result = action.get();
        repository.recordIdempotentResponse(
                context.apiCode(),
                context.client(),
                idempotencyKey,
                context.requestHash(),
                write(result.response()),
                result.resourceType(),
                result.resourceId());
        return result.response();
    }

    private <T> T read(String responseJson, Class<T> responseType) {
        try {
            return objectMapper.readValue(responseJson, responseType);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to read open api idempotent response", exception);
        }
    }

    private String write(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to write open api idempotent response", exception);
        }
    }
}
