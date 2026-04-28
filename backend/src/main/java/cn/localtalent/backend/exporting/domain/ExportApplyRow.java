package cn.localtalent.backend.exporting.domain;

import java.time.LocalDateTime;

public record ExportApplyRow(
        long exportId,
        Long companyId,
        long applyUserId,
        String applyIdentityType,
        String applyRoleCode,
        String bizType,
        String scopeJson,
        String reason,
        int approveStatus,
        Long approveUserId,
        LocalDateTime approveTime,
        String rejectReason,
        int generateStatus,
        String fileObjectKey,
        String fileSha256,
        Long fileSizeBytes,
        LocalDateTime generatedAt,
        String downloadUrl,
        LocalDateTime expireTime,
        LocalDateTime downloadIssuedAt,
        int downloadCount,
        String errorMsg,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
