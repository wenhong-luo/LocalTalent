package cn.localtalent.backend.interview.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewSigninRequest(
        @JsonProperty("session_code") String sessionCode,
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("sign_channel") String signChannel
) {
}
