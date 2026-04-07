package dev.notmarra.tenbatsu.models;

import dev.notmarra.notlib.database.annotation.Column;
import dev.notmarra.notlib.database.annotation.Table;

import java.time.Instant;
import java.util.UUID;

@Table(name = "punishments")
public class Punishment {

    @Column(name = "id", primaryKey = true, autoIncrement = true)
    private Integer id;

    @Column(name = "player_uuid")
    private String playerUuid;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "staff_uuid")
    private String staffUuid;

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "type")
    private String type;

    @Column(name = "reason")
    private String reason;

    @Column(name = "issued_at")
    private long issuedAt;

    @Column(name = "expires_at")
    private long expiresAt; // -1 = permanent

    @Column(name = "active")
    private boolean active;

    @Column(name = "ip_address")
    private String ipAddress;

    // Constructors
    public Punishment() {}

    public Punishment(UUID playerUuid, String playerName, String staffUuid,
                      String staffName, PunishmentType type, String reason,
                      long expiresAt) {
        this.playerUuid = playerUuid.toString();
        this.playerName = playerName;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.type = type.name();
        this.reason = reason;
        this.issuedAt = Instant.now().getEpochSecond();
        this.expiresAt = expiresAt;
        this.active = true;
    }

    // Getters / Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public UUID getPlayerUuid() { return UUID.fromString(playerUuid); }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getStaffUuid() { return staffUuid; }
    public void setStaffUuid(String staffUuid) { this.staffUuid = staffUuid; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public PunishmentType getType() { return PunishmentType.valueOf(type); }
    public void setType(String type) { this.type = type; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getIssuedAt() { return issuedAt; }
    public void setIssuedAt(long issuedAt) { this.issuedAt = issuedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public boolean isPermanent() { return expiresAt == -1; }

    public boolean isExpired() {
        if (isPermanent()) return false;
        return Instant.now().getEpochSecond() > expiresAt;
    }
}