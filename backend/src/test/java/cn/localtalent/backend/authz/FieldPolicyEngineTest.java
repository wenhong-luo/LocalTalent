package cn.localtalent.backend.authz;

import static org.assertj.core.api.Assertions.assertThat;

import cn.localtalent.backend.audit.FieldAccessRecorder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FieldPolicyEngineTest {

    private final RecordingFieldAccessRecorder recorder = new RecordingFieldAccessRecorder();
    private final FakeFieldPolicyRepository repository = new FakeFieldPolicyRepository();
    private final AuthzPrincipal principal = new AuthzPrincipal(
            cn.localtalent.backend.auth.domain.IdentityType.OPERATOR,
            1L,
            null,
            List.of("operator"),
            "token-id");

    private FieldPolicyEngine engine;

    @BeforeEach
    void setUp() {
        repository.clear();
        recorder.clear();
        engine = new FieldPolicyEngine(repository, recorder);
    }

    @Test
    void s1FieldShouldAllowByDefault() {
        Map<String, Object> result = apply(field("city_code", "320100", FieldSensitivity.S1));

        assertThat(result).containsEntry("city_code", "320100");
        assertThat(recorder.events()).isEmpty();
    }

    @Test
    void s2FieldShouldDenyByDefault() {
        Map<String, Object> result = apply(field("application_status", "pending", FieldSensitivity.S2));

        assertThat(result).containsEntry("application_status", null);
        assertThat(recorder.events()).isEmpty();
    }

    @Test
    void s3FieldShouldDenyAndRecordByDefault() {
        Map<String, Object> result = apply(field("mobile", "13812345678", FieldSensitivity.S3));

        assertThat(result).containsEntry("mobile", null);
        assertThat(recorder.events()).containsExactly("mobile:DENY");
    }

    @Test
    void s4FieldShouldDenyAndRecordByDefault() {
        Map<String, Object> result = apply(field("consent_evidence", "evidence", FieldSensitivity.S4));

        assertThat(result).containsEntry("consent_evidence", null);
        assertThat(recorder.events()).containsExactly("consent_evidence:DENY");
    }

    @Test
    void explicitAllowShouldReturnOriginalValue() {
        repository.put("mobile", FieldPolicyDecision.ALLOW, null);

        Map<String, Object> result = apply(field("mobile", "13812345678", FieldSensitivity.S3));

        assertThat(result).containsEntry("mobile", "13812345678");
        assertThat(recorder.events()).containsExactly("mobile:ALLOW");
    }

    @Test
    void explicitDenyShouldHideValue() {
        repository.put("email", FieldPolicyDecision.DENY, null);

        Map<String, Object> result = apply(field("email", "alice@example.com", FieldSensitivity.S3));

        assertThat(result).containsEntry("email", null);
        assertThat(recorder.events()).containsExactly("email:DENY");
    }

    @Test
    void mobileMaskShouldKeepPrefixAndSuffix() {
        repository.put("mobile", FieldPolicyDecision.MASK, "MOBILE");

        Map<String, Object> result = apply(field("mobile", "13812345678", FieldSensitivity.S3));

        assertThat(result).containsEntry("mobile", "138****5678");
    }

    @Test
    void emailMaskShouldKeepDomain() {
        repository.put("email", FieldPolicyDecision.MASK, "EMAIL");

        Map<String, Object> result = apply(field("email", "alice@example.com", FieldSensitivity.S3));

        assertThat(result).containsEntry("email", "a***@example.com");
    }

    @Test
    void nameMaskShouldKeepFirstCharacter() {
        repository.put("real_name", FieldPolicyDecision.MASK, "NAME");

        Map<String, Object> result = apply(field("real_name", "张三", FieldSensitivity.S3));

        assertThat(result).containsEntry("real_name", "张*");
    }

    @Test
    void defaultMaskShouldReturnGenericMask() {
        repository.put("resume_summary", FieldPolicyDecision.MASK, null);

        Map<String, Object> result = apply(field("resume_summary", "long resume text", FieldSensitivity.S3));

        assertThat(result).containsEntry("resume_summary", "***");
    }

    @Test
    void multiRoleShouldUseStrongestAllowDecision() {
        repository.put("email", FieldPolicyDecision.MASK, "EMAIL");
        repository.put("email", FieldPolicyDecision.ALLOW, null);

        Map<String, Object> result = apply(field("email", "alice@example.com", FieldSensitivity.S3));

        assertThat(result).containsEntry("email", "alice@example.com");
        assertThat(recorder.events()).containsExactly("email:ALLOW");
    }

    @Test
    void multiRoleShouldUseMaskWhenOnlyDenyAndMaskExist() {
        repository.put("mobile", FieldPolicyDecision.DENY, null);
        repository.put("mobile", FieldPolicyDecision.MASK, "MOBILE");

        Map<String, Object> result = apply(field("mobile", "13812345678", FieldSensitivity.S3));

        assertThat(result).containsEntry("mobile", "138****5678");
        assertThat(recorder.events()).containsExactly("mobile:MASK");
    }

    @Test
    void unconfiguredSensitiveFieldsShouldNotLeak() {
        Map<String, Object> result = apply(
                field("mobile", "13812345678", FieldSensitivity.S3),
                field("resume_body", "resume body", FieldSensitivity.S3),
                field("realname_verify_result", "passed", FieldSensitivity.S4));

        assertThat(result).containsEntry("mobile", null)
                .containsEntry("resume_body", null)
                .containsEntry("realname_verify_result", null);
    }

    private Map<String, Object> apply(ProtectedField... fields) {
        return engine.apply(principal, "candidate_profile", 100L, List.of(fields));
    }

    private ProtectedField field(String name, Object value, FieldSensitivity sensitivity) {
        return new ProtectedField(name, value, sensitivity);
    }

    private static final class FakeFieldPolicyRepository implements FieldPolicyRepository {

        private final Map<String, List<FieldPolicy>> policies = new LinkedHashMap<>();

        @Override
        public Map<String, List<FieldPolicy>> findPolicies(
                Collection<String> roleCodes,
                String bizType,
                Collection<String> fieldNames
        ) {
            Map<String, List<FieldPolicy>> selected = new LinkedHashMap<>();
            for (String fieldName : fieldNames) {
                if (policies.containsKey(fieldName)) {
                    selected.put(fieldName, policies.get(fieldName));
                }
            }
            return selected;
        }

        void put(String fieldName, FieldPolicyDecision decision, String maskRule) {
            policies.computeIfAbsent(fieldName, ignored -> new ArrayList<>())
                    .add(new FieldPolicy(fieldName, decision, maskRule));
        }

        void clear() {
            policies.clear();
        }
    }

    private static final class RecordingFieldAccessRecorder implements FieldAccessRecorder {

        private final List<String> events = new ArrayList<>();

        @Override
        public void record(
                AuthzPrincipal principal,
                String bizType,
                long bizId,
                String fieldName,
                String accessType
        ) {
            events.add(fieldName + ":" + accessType);
        }

        List<String> events() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
