package edu.itc.shop.web;

import edu.itc.shop.domain.Role;
import edu.itc.shop.service.AccountService;
import edu.itc.shop.web.dto.MeResponse;
import edu.itc.shop.web.dto.RegisterRequest;
import edu.itc.shop.web.dto.UsernameResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Public registration endpoint and "who am I?" helper.
 *
 * <ul>
 *   <li>{@code POST /api/register}  – create a new CUSTOMER account (public)</li>
 *   <li>{@code GET  /api/me}        – return the authenticated caller's name and roles</li>
 * </ul>
 */
@RestController
public class RegisterController {

    private final AccountService accountService;

    public RegisterController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Registers a new user account with the CUSTOMER role.
     *
     * @return 201 Created with {@code {"username":"…"}} – never the password
     */
    @PostMapping("/api/register")
    public ResponseEntity<UsernameResponse> register(
            @RequestBody @Valid RegisterRequest request) {

        var account = accountService.register(
                request.username(),
                request.password(),
                Set.of(Role.CUSTOMER)
        );

        return ResponseEntity
                .created(URI.create("/api/me"))
                .body(new UsernameResponse(account.getUsername()));
    }

    /**
     * Returns the authenticated user's name and granted authorities.
     * Useful for smoke-testing that the JWT / Basic credentials are accepted.
     */
    @GetMapping("/api/me")
    public MeResponse me(Authentication auth) {
        var roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());
        return new MeResponse(auth.getName(), roles);
    }
}
