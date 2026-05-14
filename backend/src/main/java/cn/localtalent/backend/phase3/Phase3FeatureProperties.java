package cn.localtalent.backend.phase3;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "localtalent.phase3")
public class Phase3FeatureProperties {

    /**
     * Prompt 27 private candidate closure. Defaults to false in configuration.
     */
    private boolean candidateClosure;

    /**
     * Candidate private resume attachment upload. Defaults to false in configuration.
     */
    private boolean resumeAttachmentUpload;

    /**
     * Candidate private rule-based resume AI assist. Defaults to false in configuration.
     */
    private boolean resumeAiAssist;

    /**
     * Prompt 28 private company workbench. Defaults to false in configuration.
     */
    private boolean companyWorkbench;

    /**
     * Private company style image upload inside the company workbench.
     * Defaults to false in configuration.
     */
    private boolean companyStyleUpload;

    /**
     * Private company logo upload inside the company workbench.
     * Defaults to false in configuration.
     */
    private boolean companyLogoUpload;

    /**
     * Company-private controlled resume search over candidate publish snapshots.
     * Defaults to false in configuration.
     */
    private boolean companyResumeSearch;

    /**
     * Prompt 29 operator/auditor portal operations. Defaults to false in configuration.
     */
    private boolean operatorPortalOps;

    public boolean isCandidateClosure() {
        return candidateClosure;
    }

    public void setCandidateClosure(boolean candidateClosure) {
        this.candidateClosure = candidateClosure;
    }

    public boolean isResumeAttachmentUpload() {
        return resumeAttachmentUpload;
    }

    public void setResumeAttachmentUpload(boolean resumeAttachmentUpload) {
        this.resumeAttachmentUpload = resumeAttachmentUpload;
    }

    public boolean isResumeAiAssist() {
        return resumeAiAssist;
    }

    public void setResumeAiAssist(boolean resumeAiAssist) {
        this.resumeAiAssist = resumeAiAssist;
    }

    public boolean isCompanyWorkbench() {
        return companyWorkbench;
    }

    public void setCompanyWorkbench(boolean companyWorkbench) {
        this.companyWorkbench = companyWorkbench;
    }

    public boolean isCompanyStyleUpload() {
        return companyStyleUpload;
    }

    public void setCompanyStyleUpload(boolean companyStyleUpload) {
        this.companyStyleUpload = companyStyleUpload;
    }

    public boolean isCompanyLogoUpload() {
        return companyLogoUpload;
    }

    public void setCompanyLogoUpload(boolean companyLogoUpload) {
        this.companyLogoUpload = companyLogoUpload;
    }

    public boolean isCompanyResumeSearch() {
        return companyResumeSearch;
    }

    public void setCompanyResumeSearch(boolean companyResumeSearch) {
        this.companyResumeSearch = companyResumeSearch;
    }

    public boolean isOperatorPortalOps() {
        return operatorPortalOps;
    }

    public void setOperatorPortalOps(boolean operatorPortalOps) {
        this.operatorPortalOps = operatorPortalOps;
    }
}
