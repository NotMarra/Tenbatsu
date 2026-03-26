package dev.notmarra.tenbatsu.models;


import dev.notmarra.notlib.database.annotation.Column;
import dev.notmarra.notlib.database.annotation.Table;

import java.time.Instant;
import java.util.UUID;

@Table(name = "reports")
public class Report {

    @Column(name = "id", primaryKey = true, autoIncrement = true)
    private int id;

    @Column(name = "reporter_uuid")
    private String reporterUuid;

    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "target_uuid")
    private String targetUuid;

    @Column(name = "target_name")
    private String targetName;

    @Column(name = "reason")
    private String reason;

    @Column(name = "reported_at")
    private long reportedAt;

    @Column(name = "handled")
    private boolean handled;

    @Column(name = "handled_by")
    private String handledBy;

    public Report() {}

    public Report(UUID reporterUuid, String reporterName, UUID targetUuid,
                  String targetName, String reason) {
        this.reporterUuid = reporterUuid.toString();
        this.reporterName = reporterName;
        this.targetUuid = targetUuid.toString();
        this.targetName = targetName;
        this.reason = reason;
        this.reportedAt = Instant.now().getEpochSecond();
        this.handled = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReporterUuid() { return reporterUuid; }
    public void setReporterUuid(String reporterUuid) { this.reporterUuid = reporterUuid; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getTargetUuid() { return targetUuid; }
    public void setTargetUuid(String targetUuid) { this.targetUuid = targetUuid; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getReportedAt() { return reportedAt; }
    public void setReportedAt(long reportedAt) { this.reportedAt = reportedAt; }

    public boolean isHandled() { return handled; }
    public void setHandled(boolean handled) { this.handled = handled; }

    public String getHandledBy() { return handledBy; }
    public void setHandledBy(String handledBy) { this.handledBy = handledBy; }
}