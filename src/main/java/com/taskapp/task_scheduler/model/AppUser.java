package com.taskapp.task_scheduler.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;

/** Local account, distinct from an employee. Passwords and tokens stay out of this entity. */
@Entity
@Table(name = "app_user", uniqueConstraints =
        @UniqueConstraint(name = "uk_app_user_identity", columnNames = "identity_key"))
@Getter
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A digest preserves exact issuer/subject identity under MySQL's usual insensitive collation.
    @Column(name = "identity_key", nullable = false, updatable = false, length = 64)
    private String identityKey;
    @Column(nullable = false, updatable = false, length = 512)
    private String issuer;
    @Column(nullable = false, updatable = false, length = 255)
    private String subject;
    @Column(nullable = false, length = 320)
    private String email;
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Version
    private long version;

    protected AppUser() { }

    public AppUser(String identityKey, String issuer, String subject, String email, String displayName) {
        this.identityKey = identityKey;
        this.issuer = issuer;
        this.subject = subject;
        updateProfile(email, displayName);
        this.createdAt = Instant.now();
    }

    public void updateProfile(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public void deactivate() {
        this.active = false;
    }
}
