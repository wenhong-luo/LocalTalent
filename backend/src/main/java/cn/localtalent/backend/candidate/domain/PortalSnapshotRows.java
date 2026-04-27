package cn.localtalent.backend.candidate.domain;

import java.util.List;

public record PortalSnapshotRows(List<PortalSnapshotRow> rows, long total) {
}
