
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

