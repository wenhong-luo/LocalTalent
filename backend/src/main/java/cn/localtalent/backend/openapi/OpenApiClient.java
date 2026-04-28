package cn.localtalent.backend.openapi;

import java.util.Set;

public record OpenApiClient(
        long id,
        String clientCode,
        String secretHash,
        String sourceSystem,
        Set<String> apiScopes,
        int status
) {

    public boolean allows(String apiCode) {
        return apiScopes.contains("*") || apiScopes.contains(apiCode);
    }
}
