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

## Task 3 — Roles and Method Security

Order endpoints use HTTP Basic authentication. The service enforces ownership and
administrator permissions, with `ADMIN` implying `CUSTOMER`.

| Request | Behavior |
|---|---|
| `POST /api/orders` | Creates an order in `NEW` status for the authenticated user and returns 201 with a `Location` header. Send a positive `productId` and `quantity` in the JSON body. |
| `GET /api/orders` | Returns the authenticated user's order history. |
| `GET /api/orders?customer=bob` | Returns the named customer's history to that customer or an admin. |
| `GET /api/orders?all=true` | Returns all orders to an admin. |
| `GET /api/orders/{id}` | Returns the order to its owner or an admin. |
| `POST /api/orders/{id}/ship` | Lets an admin set the order status to `SHIPPED` and returns 204. |

Requests without credentials return 401. Access denials return 403 with an
`application/problem+json` body. Requests for missing orders return 404.

See [security-session.md](security-session.md) for the live HTTP results.
