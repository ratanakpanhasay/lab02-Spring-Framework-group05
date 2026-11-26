
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
