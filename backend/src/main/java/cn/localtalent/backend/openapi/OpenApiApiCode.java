package cn.localtalent.backend.openapi;

public final class OpenApiApiCode {

    private OpenApiApiCode() {
    }

    public static String fromPath(String method, String requestUri) {
        if ("POST".equalsIgnoreCase(method) && "/api/open/v1/jobs/sync".equals(requestUri)) {
            return "open.jobs.sync";
        }
        if ("POST".equalsIgnoreCase(method) && "/api/open/v1/applications/sync".equals(requestUri)) {
            return "open.applications.sync";
        }
        if ("POST".equalsIgnoreCase(method) && "/api/open/v1/consents/callback".equals(requestUri)) {
            return "open.consents.callback";
        }
        if ("POST".equalsIgnoreCase(method) && "/api/open/v1/candidates/publishable-sync".equals(requestUri)) {
            return "open.candidates.publishable_sync";
        }
        if ("GET".equalsIgnoreCase(method) && "/api/open/v1/mappings/query".equals(requestUri)) {
            return "open.mappings.query";
        }
        return "open.unknown";
    }
}
