package cn.localtalent.backend.job.api;

public record JobStatusRequest(
        String action,
        String reason
) {
}
