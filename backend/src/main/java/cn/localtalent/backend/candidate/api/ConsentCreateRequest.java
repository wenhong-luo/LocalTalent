package cn.localtalent.backend.candidate.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ConsentCreateRequest(
        @JsonProperty("consent_scope") List<String> consentScope,
        @JsonProperty("consent_version") String consentVersion,
        @JsonProperty("realname_verified") Boolean realnameVerified,
        @JsonProperty("second_confirmed") Boolean secondConfirmed
) {
}
