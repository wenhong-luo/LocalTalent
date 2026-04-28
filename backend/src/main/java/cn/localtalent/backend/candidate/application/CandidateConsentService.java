package cn.localtalent.backend.candidate.application;

import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.authz.DataScopeService;
import cn.localtalent.backend.authz.ResourceOwner;
import cn.localtalent.backend.candidate.api.ConsentCreateRequest;
import cn.localtalent.backend.candidate.api.ConsentCreateResponse;
import cn.localtalent.backend.candidate.api.ConsentRevokeRequest;
import cn.localtalent.backend.candidate.api.ConsentRevokeResponse;
import cn.localtalent.backend.candidate.domain.CandidateProfileRaw;
import cn.localtalent.backend.candidate.domain.ConsentRecord;
import cn.localtalent.backend.candidate.domain.IdempotentActionResult;
import cn.localtalent.backend.candidate.infrastructure.CandidateJdbcRepository;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateConsentService {

    private static final String CREATE_API_CODE = "candidate.consent.create";
    private static final String REVOKE_API_CODE = "candidate.consent.revoke";

    private final CandidateJdbcRepository candidateRepository;
    private final IdempotencyService idempotencyService;
    private final SnapshotAssembler snapshotAssembler;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public CandidateConsentService(
            CandidateJdbcRepository candidateRepository,
            IdempotencyService idempotencyService,
            SnapshotAssembler snapshotAssembler,
            DataScopeService dataScopeService,
            AuditService auditService
    ) {
        this.candidateRepository = candidateRepository;
        this.idempotencyService = idempotencyService;
        this.snapshotAssembler = snapshotAssembler;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
        this.objectMapper = AuditJsonMapper.create();
    }

    @Transactional
    public ConsentCreateResponse createConsent(ConsentCreateRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidatePrincipal();
        NormalizedConsentRequest normalized = normalizeCreateRequest(request);
        return idempotencyService.execute(
                CREATE_API_CODE,
                principal,
                idempotencyKey,
                normalized.fingerprint(),
                ConsentCreateResponse.class,
                () -> createConsentOnce(principal, normalized));
    }

    @Transactional
    public ConsentRevokeResponse revokeConsent(
            long consentId,
            ConsentRevokeRequest request,
            String idempotencyKey
    ) {
        AuthzPrincipal principal = requireCandidatePrincipal();
        String reason = normalizeReason(request == null ? null : request.reason());
        Map<String, Object> fingerprint = new LinkedHashMap<>();
        fingerprint.put("consent_id", consentId);
        fingerprint.put("reason", reason);
        return idempotencyService.execute(
                REVOKE_API_CODE,
                principal,
                idempotencyKey,
                fingerprint,
                ConsentRevokeResponse.class,
                () -> revokeConsentOnce(principal, consentId, reason));
    }

    private IdempotentActionResult<ConsentCreateResponse> createConsentOnce(
            AuthzPrincipal principal,
            NormalizedConsentRequest request
    ) {
        long candidateId = principal.userId();
        dataScopeService.assertWritable(principal, "candidate_consent", ResourceOwner.candidate(candidateId));
        CandidateProfileRaw profile = candidateRepository.findCandidateProfile(candidateId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_401", "authentication required"));

        long consentId = candidateRepository.insertConsent(
                candidateId,
                toJson(request.scope()),
                request.consentVersion(),
                request.realnameVerified(),
                request.secondConfirmed());
        candidateRepository.offlineActiveSnapshots(candidateId);
        String snapshotJson = snapshotAssembler.build(principal, profile);
        long snapshotId = candidateRepository.insertPublishSnapshot(candidateId, request.consentVersion(), snapshotJson);
        candidateRepository.upsertControlProfileAfterConsent(candidateId, request.consentVersion(), consentId, snapshotId);

        auditService.record(principal, "candidate_consent", consentId, "consent_submit", null, "{\"consent_status\":1}");
        auditService.record(principal, "candidate_publish_snapshot", snapshotId, "snapshot_publish", null, "{\"status\":1}");

        ConsentCreateResponse response = new ConsentCreateResponse(consentId, 1, snapshotId, 1);
        return new IdempotentActionResult<>(response, "candidate_consent", consentId);
    }

    private IdempotentActionResult<ConsentRevokeResponse> revokeConsentOnce(
            AuthzPrincipal principal,
            long consentId,
            String reason
    ) {
        ConsentRecord consent = candidateRepository.findConsent(consentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BIZ_404", "consent not found"));
        dataScopeService.assertWritable(principal, "candidate_consent", ResourceOwner.candidate(consent.candidateId()));

        if (consent.revoked()) {
            ConsentRevokeResponse response = new ConsentRevokeResponse(consentId, 1, 0);
            return new IdempotentActionResult<>(response, "candidate_consent", consentId);
        }

        Long snapshotId = candidateRepository.findCurrentSnapshotId(consent.candidateId()).orElse(null);
        candidateRepository.revokeConsent(consentId, consent.candidateId());
        candidateRepository.offlineActiveSnapshots(consent.candidateId());
        candidateRepository.updateControlProfileAfterRevoke(consent.candidateId(), consentId);

        auditService.record(
                principal,
                "candidate_consent",
                consentId,
                "consent_revoke",
                "{\"consent_status\":1}",
                revokeAfterJson(reason));
        if (snapshotId != null) {
            auditService.record(
                    principal,
                    "candidate_publish_snapshot",
                    snapshotId,
                    "snapshot_offline",
                    "{\"status\":1}",
                    "{\"status\":0}");
        }

        ConsentRevokeResponse response = new ConsentRevokeResponse(consentId, 1, 0);
        return new IdempotentActionResult<>(response, "candidate_consent", consentId);
    }

    private AuthzPrincipal requireCandidatePrincipal() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.CANDIDATE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", "candidate identity required");
        }
        return principal;
    }

    private NormalizedConsentRequest normalizeCreateRequest(ConsentCreateRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        List<String> scope = normalizeScope(request.consentScope());
        if (!scope.contains("talent_service")) {
            throw validation("consent_scope must include talent_service");
        }
        String consentVersion = normalizeRequired(request.consentVersion(), "consent_version", 32);
        if (!Boolean.TRUE.equals(request.realnameVerified())) {
            throw validation("realname_verified must be true");
        }
        if (!Boolean.TRUE.equals(request.secondConfirmed())) {
            throw validation("second_confirmed must be true");
        }
        return new NormalizedConsentRequest(scope, consentVersion, true, true);
    }

    private List<String> normalizeScope(List<String> rawScope) {
        if (rawScope == null || rawScope.isEmpty()) {
            throw validation("consent_scope is required");
        }
        List<String> scope = new ArrayList<>();
        for (String value : rawScope) {
            String normalized = normalizeConsentScope(value);
            if (normalized != null && !scope.contains(normalized)) {
                scope.add(normalized);
            }
        }
        scope.sort(Comparator.naturalOrder());
        if (scope.isEmpty()) {
            throw validation("consent_scope is required");
        }
        return scope;
    }

    private String normalizeConsentScope(String value) {
        String normalized = normalize(value);
        if ("talent_service_area".equals(normalized)) {
            return "talent_service";
        }
        return normalized;
    }

    private String normalizeRequired(String value, String fieldName, int maxLength) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw validation(fieldName + " is required");
        }
        if (normalized.length() > maxLength) {
            throw validation(fieldName + " is too long");
        }
        return normalized;
    }

    private String normalizeReason(String reason) {
        String normalized = normalize(reason);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > 200) {
            throw validation("reason is too long");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to write json", exception);
        }
    }

    private String revokeAfterJson(String reason) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("consent_status", 2);
        if (reason != null) {
            after.put("reason", reason);
        }
        return toJson(after);
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", message);
    }

    private record NormalizedConsentRequest(
            List<String> scope,
            String consentVersion,
            boolean realnameVerified,
            boolean secondConfirmed
    ) {

        Map<String, Object> fingerprint() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("consent_scope", scope);
            payload.put("consent_version", consentVersion);
            payload.put("realname_verified", realnameVerified);
            payload.put("second_confirmed", secondConfirmed);
            return payload;
        }
    }
}
