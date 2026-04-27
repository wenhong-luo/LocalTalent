package cn.localtalent.backend.authz;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface FieldPolicyRepository {

    Map<String, List<FieldPolicy>> findPolicies(
            Collection<String> roleCodes,
            String bizType,
            Collection<String> fieldNames
    );
}
