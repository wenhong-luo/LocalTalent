package cn.localtalent.backend.audit;

import cn.localtalent.backend.authz.AuthzPrincipal;

public interface FieldAccessRecorder {

    void record(
            AuthzPrincipal principal,
            String bizType,
            long bizId,
            String fieldName,
            String accessType
    );
}
