package cn.localtalent.backend.candidate.domain;

public record PortalSnapshotRow(
        long snapshotId,
        String snapshotJson,
        String updatedAt
) {
}
