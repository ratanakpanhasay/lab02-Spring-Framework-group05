# Shop Secure — Lab 05

## Task 1: Default Spring Security Behavior

### Default Security Configuration

Spring Boot automatically protects HTTP endpoints when Spring Security is included and no custom `SecurityFilterChain` is defined.

When accessing `GET /api/products` without authentication, the server returned **401 Unauthorized**.

### UserDetailsServiceAutoConfiguration

Initially, Spring Boot did not generate a default user or display a generated security password.

The auto-configuration debug report showed that `UserDetailsServiceAutoConfiguration` did not match because the OAuth2 Resource Server dependency introduced `OpaqueTokenIntrospector`.

To test HTTP Basic authentication, we configured a temporary development user:

- Username: `user`
- Password: Provided through the `LAB05_DEMO_PASSWORD` environment variable.

After restarting the application, Spring Boot configured an `InMemoryUserDetailsManager`.

### HTTP Test Results

| Request | Result |
|---|---|
| Anonymous `GET /api/products` | 401 Unauthorized |
| Authenticated `GET /api/products` | 200 OK with three products |

The actual HTTP tests are documented in `security-session.md`.
