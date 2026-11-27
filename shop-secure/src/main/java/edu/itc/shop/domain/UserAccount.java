package edu.itc.shop.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * A registered user account.
 * <p>
 * The {@code passwordHash} field is intentionally excluded from
 * {@link #toString()} so it never leaks into logs or error messages.
 */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String username;

    /** BCrypt hash produced by {@link org.springframework.security.crypto.password.PasswordEncoder}. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Eagerly fetched so that {@link edu.itc.shop.security.JpaUserDetailsService}
     * can build authorities without an open session.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_account_roles",
            joinColumns = @JoinColumn(name = "user_account_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    /** Required by JPA. */
    protected UserAccount() {}

    public UserAccount(String username, String passwordHash, Set<Role> roles) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.roles        = new HashSet<>(roles);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId()           { return id; }
    public String getUsername()   { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled()    { return enabled; }
    public Set<Role> getRoles()   { return roles; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Intentionally omits {@code passwordHash} to prevent accidental logging.
     */
    @Override
    public String toString() {
        return "UserAccount{id=" + id + ", username='" + username + "', enabled=" + enabled
                + ", roles=" + roles + "}";
    }
}
