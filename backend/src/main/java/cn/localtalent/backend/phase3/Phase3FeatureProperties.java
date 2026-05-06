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
     * Prompt 28 private company workbench. Defaults to false in configuration.
     */
    private boolean companyWorkbench;

    public boolean isCandidateClosure() {
        return candidateClosure;
    }

    public void setCandidateClosure(boolean candidateClosure) {
        this.candidateClosure = candidateClosure;
    }

    public boolean isCompanyWorkbench() {
        return companyWorkbench;
    }

    public void setCompanyWorkbench(boolean companyWorkbench) {
        this.companyWorkbench = companyWorkbench;
    }
}
