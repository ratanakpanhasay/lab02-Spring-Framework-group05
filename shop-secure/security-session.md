
# Lab 05 — Security Session

## Task 1: Default Spring Security

### Default auto-configuration

Spring Boot's SecurityFilterChain protects API endpoints by default.

Initially, UserDetailsServiceAutoConfiguration did not create a default user because the OAuth2 Resource Server dependency introduced OpaqueTokenIntrospector.

For testing, a temporary user was enabled using the `spring.security.user` properties and a password supplied through an environment variable.

### Test 1: Anonymous GET /api/products

Command:
`curl -i http://localhost:8080/api/products`

Result: HTTP 401 Unauthorized

### Test 2: Authenticated GET /api/products

Command:
`curl -i -u user:<demo-password> http://localhost:8080/api/products`

Result: HTTP 200 OK, returning three seeded products.
 
## Task 1 - Final Security Tests 
 
### 1. Anonymous GET - 200 
```http 
HTTP/1.1 200 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Content-Length: 111
Date: Thu, 26 Nov 2026 12:01:55 GMT

[{"name":"Wireless Mouse","price":19.90,"stock":15,"id":2},{"name":"Keyboard","price":39.90,"stock":10,"id":3}] 
``` 
### 2. Anonymous POST - 401 
```http 
HTTP/1.1 401 
WWW-Authenticate: Basic realm="Realm"
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/problem+json;charset=UTF-8
Content-Length: 99
Date: Thu, 26 Nov 2026 12:01:55 GMT

{"detail":"Authentication required","instance":"/api/products","status":401,"title":"Unauthorized"} 
``` 
### 3. Alice DELETE - 403 
```http 
HTTP/1.1 403 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/problem+json;charset=UTF-8
Content-Length: 88
Date: Thu, 26 Nov 2026 12:01:55 GMT

{"detail":"Access denied","instance":"/api/products/2","status":403,"title":"Forbidden"} 
``` 
### 4. Admin DELETE - 204 
```http 
HTTP/1.1 204 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Date: Thu, 26 Nov 2026 12:01:55 GMT

 
``` 
## Task 2 — Users and Passwords in the Database

### Registration: validation failure (400)

```
$ curl -i -X POST localhost:8080/api/register \
       -H 'Content-Type: application/json' \
       -d '{"username":"bob","password":"short"}'

HTTP/1.1 400
Content-Type: application/problem+json;charset=UTF-8

{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/register",
  "errors": [
    {"field": "password", "message": "size must be between 12 and 72"},
    {"field": "password", "message": "must contain an upper-case letter, a lower-case letter and a digit"}
  ]
}
```

### Registration: success (201)

```
$ curl -i -X POST localhost:8080/api/register \
       -H 'Content-Type: application/json' \
       -d '{"username":"bob","password":"Correct-Horse-9"}'

HTTP/1.1 201
Location: /api/me
Content-Type: application/json

{"username":"bob"}
```

### GET /api/me with Basic credentials (200)

```
$ curl -i -u bob:Correct-Horse-9 localhost:8080/api/me

HTTP/1.1 200
Content-Type: application/json

{"name":"bob","roles":["ROLE_CUSTOMER"]}
```

### Registration: duplicate username (409)

```
$ curl -i -X POST localhost:8080/api/register \
       -H 'Content-Type: application/json' \
       -d '{"username":"bob","password":"Correct-Horse-9"}'

HTTP/1.1 409
Content-Type: application/problem+json;charset=UTF-8

{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Username already taken: bob",
  "instance": "/api/register"
}
```

### USER_ACCOUNT table — bcrypt hash evidence

The following row was retrieved from the in-memory H2 database after registering `bob`.
It confirms that the `DelegatingPasswordEncoder` stores passwords as `{bcrypt}$2a$10$…`
and that the raw password is **never** stored.

```
ID  USERNAME  PASSWORD_HASH                                                              ENABLED
3   bob       {bcrypt}$2a$10$Qm8Yy4p1mJ0k3G0G3rZbUeC7O0rX3n6nT0Ff1c9uYw1n0UQxRk5aG  TRUE
```

## Task 3 — Roles and Method Security

Verified against the running application on 2026-11-27 with a fresh in-memory
H2 database. Alice and admin were seeded. Bob registered through `/api/register`
and authenticated with HTTP Basic. The excerpts below show the observed status,
selected response headers, and body for each request.

### Alice creates an order

`POST /api/orders` with `{"productId":1,"quantity":2}`

```
Status 201
Content-Type: application/json
Location: /api/orders/1

{"customer":"alice","productId":1,"quantity":2,"id":1,"status":"NEW"}
```

### Bob is denied access to Alice's order

`GET /api/orders/1`

```
Status 403
Content-Type: application/problem+json

{"detail":"Access denied","instance":"/api/orders/1","status":403,"title":"Forbidden"}
```

### Admin reads Alice's order

`GET /api/orders/1`

```
Status 200
Content-Type: application/json

{"customer":"alice","productId":1,"quantity":2,"id":1,"status":"NEW"}
```

Additional live checks confirmed that Alice could read her own order and history.
Bob received 403 for Alice's history, the full order list, and shipping. Admin
could read Alice's history and the full list, and received 204 for shipping.
Alice then read the order with status `SHIPPED`. A missing order returned 404 and
an invalid quantity returned 400. The method-level denial matched the existing
filter-chain denial's JSON fields.

## Task 4 — JWT Authentication and Stateless API

### Why JWT Authentication?

Task 3 used HTTP Basic Authentication, where the client sends username and password with each request. In Task 4, authentication was changed to JWT (JSON Web Token).

After a successful login, the server issues an access token containing the user's identity and roles. The client then uses this token to authenticate subsequent requests by sending it in the Authorization header.

JWT allows the API to operate statelessly because the server does not need to maintain a server-side session for each authenticated user.

### Login Endpoint

The login endpoint is:

`POST /api/auth/login`

The client sends the username and password in a JSON request body. The server authenticates the credentials using the configured AuthenticationManager.

- Valid credentials: returns HTTP 200 and a JWT access token.
- Invalid credentials: returns HTTP 401 Unauthorized.

The issued token contains the user's subject, roles, issuer, issued-at time, expiration time, and a unique token identifier (`jti`).

### Token Validation and Authorization

The API is configured as a stateless resource server using JWT Bearer authentication.

When a client sends a request with an access token in the `Authorization: Bearer` header, Spring Security:

1. Extracts the Bearer token.
2. Validates the JWT signature using the configured secret key.
3. Validates the issuer (`shop-api`).
4. Checks the token's expiration time.
5. Converts the `roles` claim into granted authorities.
6. Uses the authorities to determine whether the user is authorized to access the endpoint.

The security configuration uses `SessionCreationPolicy.STATELESS`, so HTTP sessions are not used to maintain authentication state.

- Missing or invalid token: HTTP 401 Unauthorized.
- Valid token but insufficient permissions: HTTP 403 Forbidden.

### Authentication Tests

The authentication flow was tested using HTTP requests.

#### 1. Successful login

`POST /api/auth/login` with valid credentials returned HTTP 200 and an access token.

#### 2. Invalid credentials

Login with an incorrect password returned HTTP 401 Unauthorized.

#### 3. Request without a token

`GET /api/orders` without an Authorization header returned HTTP 401 Unauthorized.

#### 4. Request with a valid token

`GET /api/orders` with a valid Bearer token returned HTTP 200 OK.

#### 5. Invalid token

A request containing a malformed JWT returned HTTP 401 Unauthorized.

### Automated Tests

The full Maven test suite was executed using:

`./mvnw test`

All 13 tests passed successfully.

| Test Class | Tests | Failures | Errors |
|---|---:|---:|---:|
| AuthFlowTest | 4 | 0 | 0 |
| OrderControllerSecurityTest | 3 | 0 | 0 |
| OrderServiceSecurityTest | 5 | 0 | 0 |
| ShopSecureApplicationTests | 1 | 0 | 0 |
| **Total** | **13** | **0** | **0** |

The test suite verified successful authentication, invalid credentials, unauthenticated access, authenticated requests, controller-level security, service-level authorization, and application context startup.

## Task 5 — Security Hardening

### 5.1 CSRF Strategy

The CSRF strategy is documented in `docs/csrf.md`. The stateless API uses JWT Bearer authentication rather than session cookies.

### 5.2 CORS Configuration

CORS is configured in `CorsConfig.java` and enabled in `SecurityConfig.java`.

**Test 1: Allowed origin**


curl -i -X OPTIONS http://localhost:8080/api/products \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization, Content-Type"


Result:

http
HTTP/1.1 200
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,DELETE
Access-Control-Allow-Headers: Authorization, Content-Type
Access-Control-Expose-Headers: Location
Access-Control-Max-Age: 3600

**Test 2: Disallowed origin**


curl -i -X OPTIONS http://localhost:8080/api/products \
  -H "Origin: http://evil.example" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization, Content-Type"


Result:

http
HTTP/1.1 403
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

Invalid CORS request

The trusted origin receives the required CORS headers. The untrusted origin is rejected with HTTP 403 and no Access-Control-Allow-Origin header.

Result: PASS

### 5.3 Security Headers

Security headers are explicitly configured in `SecurityConfig.java` to protect against clickjacking, MIME-type sniffing, insecure transport, and unauthorized resource loading.

**Test 1: HTTP response headers**

```bash
curl -s -D - -o /dev/null http://localhost:8080/api/products
```

Result:

```http
HTTP/1.1 200
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'
Referrer-Policy: strict-origin-when-cross-origin
Content-Type: application/json
```

The initial HEAD request returned HTTP 401 because only GET requests were explicitly permitted for public product endpoints. A GET request was used to verify the headers on a successful response without changing the authorization rules.

**Test 2: HTTPS response headers**

A temporary self-signed certificate was generated to verify HSTS over HTTPS.

```bash
curl -k -s -D - -o /dev/null https://localhost:8443/api/products
```

Result:

```http
HTTP/1.1 200
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000 ; includeSubDomains
X-Frame-Options: DENY
Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'
Referrer-Policy: strict-origin-when-cross-origin
Content-Type: application/json
```

HSTS is correctly returned over HTTPS with a maximum age of one year and includes subdomains. The self-signed certificate and curl's certificate verification bypass were used for local testing only.

**Result: PASS — All five required security headers verified.**

### 5.4 — Stored XSS Protection

#### Implementation

- Created the Review entity, ReviewRepository, and ReviewRequest DTO with input validation.
- Implemented authenticated `POST /api/reviews`.
- Implemented public `GET /api/products/{id}/reviews`.
- Created a Thymeleaf review page at `/products/{id}/reviews.html`.
- Used `th:text` to escape user-controlled review content.

#### Test Results

| Test | Expected | Actual |
|---|---|---|
| Unauthenticated review submission | 401 Unauthorized | PASS |
| Authenticated review submission | 201 Created | PASS |
| Public review retrieval | 200 OK | PASS |
| JSON Content-Type | application/json | PASS |
| X-Content-Type-Options | nosniff | PASS |
| Safe HTML rendering | Escaped content | PASS |
| Unsafe HTML demonstration | Raw HTML rendering | PASS |

#### Safe Rendering

Thymeleaf's `th:text` escapes HTML characters, preventing stored review content from being interpreted as executable HTML.

#### Unsafe Rendering Demonstration

A separate branch, `lab05-task-5-xss-demo`, was created to demonstrate unsafe rendering using `th:utext`.

- Demonstration commit: `a00ea85`
- The browser rendered the submitted HTML as a broken image instead of plain text.
- Content Security Policy remained enabled as an additional protection layer.
- JavaScript execution was not independently verified.
- The demonstration branch was not merged into the secure implementation.

#### Conclusion

User-controlled content must be escaped when rendered in HTML. Content Security Policy provides additional protection but does not replace proper output escaping.

#### Screenshot Evidence

- `05_04_Review_Unauthenticated.png`
- `05_04_XSS_Review_Created.png`
- `05_04_XSS_JSON_Response.png`
- `05_04_XSS_Safe_HTML.png`
- `05_04_XSS_Escaped_HTML_Source.png`
- `05_04_XSS_Unsafe_HTML_Source.png`
- `05_04_XSS_Unsafe_HTML.png`