package cn.localtalent.backend.interview.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SigninCodeRequest(
        @JsonProperty("ttl_minutes") Integer ttlMinutes
) {
}
