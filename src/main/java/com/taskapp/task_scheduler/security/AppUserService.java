package com.taskapp.task_scheduler.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.taskapp.auth.CurrentUser;
import com.taskapp.auth.ExternalUserId;
import com.taskapp.auth.UserIdentity;
import com.taskapp.task_scheduler.model.AppUser;
import com.taskapp.task_scheduler.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AppUserService {
    private final CurrentUser currentUser;
    private final AppUserRepository users;
    private final TransactionTemplate transactions;

    public AppUserService(CurrentUser currentUser, AppUserRepository users, PlatformTransactionManager manager) {
        this.currentUser = currentUser;
        this.users = users;
        this.transactions = new TransactionTemplate(manager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Resolves the authenticated identity on every request, including the local active flag. */
    public Account requireCurrent() {
        UserIdentity identity = currentUser.requireVerifiedEmail();
        validate(identity);
        String key = identityKey(identity.id());
        try {
            return transactions.execute(status -> resolve(identity, key, true));
        } catch (DataIntegrityViolationException collision) {
            // Concurrent first logins can race on the unique key. Retry only after rollback,
            // and only when the exact identity now exists; never retry a failed transaction.
            Account existing = transactions.execute(status -> resolve(identity, key, false));
            if (existing == null) throw collision;
            return existing;
        }
    }

    private Account resolve(UserIdentity identity, String key, boolean create) {
        AppUser user = users.findByIdentityKey(key).orElse(null);
        String name = identity.displayName();
        if (name == null || name.isBlank()) name = "Mi cuenta";
        if (name.length() > 255) name = name.substring(0, 255);
        if (user == null) {
            if (!create) return null;
            user = users.saveAndFlush(new AppUser(key, identity.id().issuer(), identity.id().subject(),
                    identity.email(), name));
        }
        if (!user.getIssuer().equals(identity.id().issuer()) || !user.getSubject().equals(identity.id().subject())) {
            throw new AccessDeniedException("External identity mismatch");
        }
        if (!user.isActive()) throw new AccessDeniedException("Account unavailable");
        user.updateProfile(identity.email(), name);
        return new Account(user.getId(), user.getDisplayName(), user.getEmail());
    }

    private static void validate(UserIdentity identity) {
        if (identity.id().issuer().length() > 512 || identity.id().subject().length() > 255
                || identity.email().length() > 320) {
            throw new AccessDeniedException("Invalid identity profile");
        }
    }

    private static String identityKey(ExternalUserId identity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] issuer = identity.issuer().getBytes(StandardCharsets.UTF_8);
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(issuer.length).array());
            digest.update(issuer);
            digest.update(identity.subject().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public record Account(Long id, String displayName, String email) { }
}
