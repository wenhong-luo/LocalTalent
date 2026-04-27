package cn.localtalent.backend.authz;

public record ResourceOwner(
        Long candidateId,
        Long companyId,
        Long jobId,
        Long sessionId,
        boolean aggregateOnly,
        boolean auditOnly,
        boolean detailAuthorized
) {

    public static ResourceOwner candidate(long candidateId) {
        return new ResourceOwner(candidateId, null, null, null, false, false, false);
    }

    public static ResourceOwner company(long companyId) {
        return new ResourceOwner(null, companyId, null, null, false, false, false);
    }

    public static ResourceOwner companyDetail(long companyId) {
        return new ResourceOwner(null, companyId, null, null, false, false, true);
    }

    public static ResourceOwner aggregate() {
        return new ResourceOwner(null, null, null, null, true, false, false);
    }

    public static ResourceOwner audit() {
        return new ResourceOwner(null, null, null, null, false, true, false);
    }
}
