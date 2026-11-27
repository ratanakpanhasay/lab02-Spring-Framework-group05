package edu.itc.shop.service;

import edu.itc.shop.domain.Role;
import edu.itc.shop.domain.UserAccount;
import edu.itc.shop.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Business operations on user accounts (registration, seeding).
 */
@Service
public class AccountService {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;

    public AccountService(UserAccountRepository users, PasswordEncoder encoder) {
        this.users   = users;
        this.encoder = encoder;
    }

    /**
     * Registers a new user account.
     *
     * @param username    desired username (must be unique)
     * @param rawPassword plain-text password – stored as a bcrypt hash, never logged
     * @param roles       initial roles to assign
     * @return the saved {@link UserAccount}
     * @throws UsernameTakenException if the username already exists (HTTP 409)
     */
    @Transactional
    public UserAccount register(String username, String rawPassword, Set<Role> roles) {
        if (users.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        String hash = encoder.encode(rawPassword);    // {bcrypt}$2a$10$…
        UserAccount account = new UserAccount(username, hash, roles);
        return users.save(account);
    }
}
