package cn.localtalent.backend.candidate.domain;

public record IdempotencyStoredResponse(String requestHash, String responseJson) {
}
