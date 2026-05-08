package cn.localtalent.backend.candidatecenter.application;

import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterOverviewResponse;
import cn.localtalent.backend.candidatecenter.domain.CandidateApplicationSummary;
import cn.localtalent.backend.candidatecenter.domain.CandidateConsentSummary;
import cn.localtalent.backend.candidatecenter.domain.CandidateResumeSummary;
import cn.localtalent.backend.candidatecenter.domain.CandidateSigninSummary;
import cn.localtalent.backend.candidatecenter.infrastructure.CandidateCenterJdbcRepository;
import cn.localtalent.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CandidateCenterService {

    private final CandidateCenterJdbcRepository repository;
    private final CandidateClosureService closureService;

    public CandidateCenterService(CandidateCenterJdbcRepository repository, CandidateClosureService closureService) {
        this.repository = repository;
        this.closureService = closureService;
    }

    public CandidateCenterOverviewResponse overview() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.CANDIDATE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", "candidate identity required");
        }
        long candidateId = principal.userId();
        CandidateResumeSummary resumeSummary = repository.resumeSummary(candidateId);
        CandidateConsentSummary consentSummary = repository.consentSummary(candidateId);
        return new CandidateCenterOverviewResponse(
                resume(resumeSummary),
                applications(repository.applicationSummary(candidateId)),
                signin(repository.signinSummary(candidateId)),
                consent(consentSummary),
                closureService.stats(candidateId),
                closureService.features(),
                closureService.onboarding(candidateId, resumeSummary, consentSummary.publishStatus()));
    }

    private CandidateCenterOverviewResponse.CandidateResumeSummaryResponse resume(CandidateResumeSummary summary) {
        return new CandidateCenterOverviewResponse.CandidateResumeSummaryResponse(
                summary.completionPercent(),
                summary.updatedAt(),
                summary.skillsSummary());
    }

    private CandidateCenterOverviewResponse.CandidateApplicationSummaryResponse applications(
            CandidateApplicationSummary summary
    ) {
        return new CandidateCenterOverviewResponse.CandidateApplicationSummaryResponse(
                summary.total(),
                applicationStatusLabel(summary.latestStatus()),
                summary.latestJobTitle());
    }

    private CandidateCenterOverviewResponse.CandidateSigninSummaryResponse signin(CandidateSigninSummary summary) {
        return new CandidateCenterOverviewResponse.CandidateSigninSummaryResponse(
                summary.latestStatus(),
                summary.latestTime());
    }

    private CandidateCenterOverviewResponse.CandidateConsentSummaryResponse consent(CandidateConsentSummary summary) {
        return new CandidateCenterOverviewResponse.CandidateConsentSummaryResponse(
                summary.consentId(),
                summary.publishStatus(),
                summary.publishableFlag(),
                summary.statusLabel(),
                summary.reason(),
                summary.updatedAt());
    }

    private String applicationStatusLabel(Integer status) {
        if (status == null) {
            return "暂无投递";
        }
        return switch (status) {
            case 0 -> "已投递";
            case 2 -> "邀约面试";
            case 3 -> "已签到";
            default -> "状态" + status;
        };
    }
}
