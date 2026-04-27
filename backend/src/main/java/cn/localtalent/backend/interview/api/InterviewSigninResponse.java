package cn.localtalent.backend.interview.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewSigninResponse(
        @JsonProperty("signin_id") long signinId,
        @JsonProperty("session_id") long sessionId,
        @JsonProperty("application_id") long applicationId,
        @JsonProperty("redirect_type") String redirectType
) {
}
