package com.ticket.user.domain;

import com.ticket.security.Role;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String actorId;

    @Column(nullable = false)
    private String actorEmail;

    @Column(nullable = false)
    private String targetId;

    @Column(nullable = false)
    private String targetEmail;

    @Column(nullable = false)
    private String details;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected AdminAuditLog() {}

    public static AdminAuditLog roleChanged(UUID actorId, String actorEmail,
                                            UUID targetId, String targetEmail,
                                            Role previousRole, Role newRole) {
        AdminAuditLog log = new AdminAuditLog();
        log.action = "ROLE_CHANGED";
        log.actorId = actorId.toString();
        log.actorEmail = actorEmail;
        log.targetId = targetId.toString();
        log.targetEmail = targetEmail;
        log.details = previousRole + " -> " + newRole;
        log.occurredAt = LocalDateTime.now();
        return log;
    }

    public static AdminAuditLog bootstrapped(UUID targetId, String targetEmail) {
        AdminAuditLog log = new AdminAuditLog();
        log.action = "ADMIN_BOOTSTRAPPED";
        log.actorId = "SYSTEM";
        log.actorEmail = "SYSTEM";
        log.targetId = targetId.toString();
        log.targetEmail = targetEmail;
        log.details = "USER -> ADMIN (bootstrap)";
        log.occurredAt = LocalDateTime.now();
        return log;
    }

    public UUID getId() { return id; }
    public String getAction() { return action; }
    public String getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public String getTargetId() { return targetId; }
    public String getTargetEmail() { return targetEmail; }
    public String getDetails() { return details; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}