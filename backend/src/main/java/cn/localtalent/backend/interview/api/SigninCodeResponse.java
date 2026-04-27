package cn.localtalent.backend.interview.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record SigninCodeResponse(
        @JsonProperty("session_id") long sessionId,
        @JsonProperty("session_code") String sessionCode,
        @JsonProperty("expires_at") LocalDateTime expiresAt
) {
}
