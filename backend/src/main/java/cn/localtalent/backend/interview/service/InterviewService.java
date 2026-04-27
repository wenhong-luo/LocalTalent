package cn.localtalent.backend.interview.service;

import cn.localtalent.backend.application.domain.ApplicationRow;
import cn.localtalent.backend.application.service.ApplicationService;
import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.authz.DataScopeService;
import cn.localtalent.backend.authz.ResourceOwner;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import cn.localtalent.backend.interview.api.InterviewSessionCreateRequest;
import cn.localtalent.backend.interview.api.InterviewSessionResponse;
import cn.localtalent.backend.interview.api.InterviewSigninRequest;
import cn.localtalent.backend.interview.api.InterviewSigninResponse;
import cn.localtalent.backend.interview.api.SigninCodeRequest;
import cn.localtalent.backend.interview.api.SigninCodeResponse;
import cn.localtalent.backend.interview.domain.InterviewSessionRow;
import cn.localtalent.backend.interview.infrastructure.InterviewJdbcRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewService {

    private static final int DEFAULT_TTL_MINUTES = 30;
    private static final int MAX_TTL_MINUTES = 120;

    private final InterviewJdbcRepository interviewRepository;
    private final ApplicationService applicationService;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final SigninCodeService signinCodeService;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public InterviewService(
            InterviewJdbcRepository interviewRepository,
            ApplicationService applicationService,
            DataScopeService dataScopeService,
            AuditService auditService,
            SigninCodeService signinCodeService
    ) {
        this.interviewRepository = interviewRepository;
        this.applicationService = applicationService;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
        this.signinCodeService = signinCodeService;
    }

    @Transactional
    public InterviewSessionResponse createSession(long applicationId, InterviewSessionCreateRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        ApplicationRow application = applicationService.requireCompanyOwnedApplication(principal, applicationId);
        InterviewSessionCreateRequest normalized = normalizeSessionCreate(request);
        if (application.status() == ApplicationService.STATUS_SIGNED_IN) {
            throw validation("application has already signed in");
        }

        try {
            long sessionId = interviewRepository.createSession(application, normalized);
            applicationService.markInvited(applicationId);
            InterviewSessionRow row = requireSession(sessionId);
            auditService.record(principal, "interview_session", sessionId, "interview_session_create", null, json(row));
            return response(row);
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "INTERVIEW_409", "interview session already exists");
        }
    }

    @Transactional
    public SigninCodeResponse generateCode(long sessionId, SigninCodeRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        InterviewSessionRow session = requireSession(sessionId);
        dataScopeService.assertWritable(principal, "interview_session", ResourceOwner.company(session.companyId()));
        int ttlMinutes = normalizeTtl(request == null ? null : request.ttlMinutes());
        String code = signinCodeService.generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);
        interviewRepository.updateSigninCode(sessionId, signinCodeService.hash(code), expiresAt);
        return new SigninCodeResponse(sessionId, code, expiresAt);
    }

    @Transactional
    public InterviewSigninResponse signin(InterviewSigninRequest request) {
        AuthzPrincipal principal = requireCandidatePrincipal();
        String code = required(request == null ? null : request.sessionCode(), "session_code", 128);
        InterviewSessionRow session = interviewRepository.findByCodeHash(signinCodeService.hash(code))
                .orElseThrow(() -> validation("invalid signin code"));
        if (session.applicationId() == null || session.candidateId() == null) {
            throw validation("invalid signin code");
        }
        dataScopeService.assertWritable(principal, "interview_signin", ResourceOwner.candidate(session.candidateId()));
        if (!session.candidateId().equals(principal.userId())) {
            throw forbidden("candidate does not own this signin code");
        }
        if (session.signinCodeUsedAt() != null) {
            throw validation("signin code already used");
        }
        if (session.signinCodeExpiresAt() == null || session.signinCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw validation("signin code expired");
        }
        if (interviewRepository.markCodeUsed(session.sessionId()) != 1) {
            throw validation("signin code already used or expired");
        }

        try {
            long signinId = interviewRepository.insertSignin(
                    session.sessionId(),
                    principal.userId(),
                    normalizeSignChannel(request.signChannel()),
                    normalizeNullable(request.deviceId(), 128));
            applicationService.markSignedIn(session.applicationId());
            auditService.record(principal, "interview_signin", signinId, "interview_signin", null, json(session));
            return new InterviewSigninResponse(signinId, session.sessionId(), session.applicationId(), "status");
        } catch (DuplicateKeyException exception) {
            throw validation("candidate already signed in");
        }
    }

    private InterviewSessionRow requireSession(long sessionId) {
        return interviewRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "interview session not found"));
    }

    private InterviewSessionResponse response(InterviewSessionRow row) {
        if (row.applicationId() == null) {
            throw validation("interview session is not bound to application");
        }
        return new InterviewSessionResponse(
                row.sessionId(),
                row.applicationId(),
                row.jobId(),
                row.companyId(),
                row.status(),
                row.sessionName(),
                row.sessionTime(),
                row.location());
    }

    private InterviewSessionCreateRequest normalizeSessionCreate(InterviewSessionCreateRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        if (request.sessionTime() == null) {
            throw validation("session_time is required");
        }
        return new InterviewSessionCreateRequest(
                required(request.sessionName(), "session_name", 150),
                request.sessionTime(),
                normalizeNullable(request.location(), 255));
    }

    private int normalizeTtl(Integer ttlMinutes) {
        if (ttlMinutes == null) {
            return DEFAULT_TTL_MINUTES;
        }
        if (ttlMinutes < 1 || ttlMinutes > MAX_TTL_MINUTES) {
            throw validation("ttl_minutes must be between 1 and 120");
        }
        return ttlMinutes;
    }

    private String normalizeSignChannel(String value) {
        String normalized = normalizeNullable(value, 32);
        return normalized == null ? "qrcode" : normalized;
    }

    private String required(String value, String fieldName, int maxLength) {
        String normalized = normalizeNullable(value, maxLength);
        if (normalized == null) {
            throw validation(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw validation("field length is too long");
        }
        return normalized;
    }

    private AuthzPrincipal requireCompanyPrincipal() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.COMPANY || principal.companyId() == null) {
            throw forbidden("company identity required");
        }
        return principal;
    }

    private AuthzPrincipal requireCandidatePrincipal() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.CANDIDATE) {
            throw forbidden("candidate identity required");
        }
        return principal;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", message);
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", message);
    }
}
