package com.taskapp.auth;

/** Stable external identity. Neither email nor a local database ID replaces this pair. */
public record ExternalUserId(String issuer, String subject) {

    public ExternalUserId {
        if (issuer == null || issuer.isBlank() || subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Issuer and subject are required");
        }
        // Preserve the values exactly; normalizing an issuer changes the identity key.
    }
}
