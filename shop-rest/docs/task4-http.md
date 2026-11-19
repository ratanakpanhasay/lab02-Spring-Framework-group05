# Lab-04 Task 4 — Providers and Filters: HTTP Test Evidence

Captured on 2026-11-19 against the real server started with
`mvn -q compile exec:java` (`http://localhost:8080/api/`), curl 8.22.0, Git Bash on Windows 11.
`-Djava.util.logging.SimpleFormatter.format="%4$s [%3$s] %5$s%n"` was added only for one-line log output.

Automated check: `mvn -q verify` → `ProvidersTest` 19 tests and `ApiKeysPropertyTest` 2 tests,
`Failures: 0, Errors: 0, Skipped: 0`.

| Provider | Kind / order | Behaviour |
|---|---|---|
| `RequestTimingFilter` | `@PreMatching` request filter, `@Priority(1)` | Records the request start time first, so the duration is measured even when CORS or `ApiKeyFilter` abort the request |
| `CorsFilter` | `@PreMatching` request + response filter | Answers preflight `OPTIONS` with **200**; adds to **every** response, including 401: `Access-Control-Allow-Origin: http://localhost:5173`, `Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS`, `Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match`, `Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id` |
| `ApiKeyFilter` | request filter, `@Priority(Priorities.AUTHENTICATION)` (1000) | `GET`/`HEAD`/`OPTIONS` public; other methods need `X-Api-Key` matching one of the comma-separated keys in system property `shop.api.keys` (default `demo-123`), else 401 `application/problem+json` + `WWW-Authenticate: ApiKey realm="shop"` |
| `CorrelationFilter` | request + response filter, `@Priority(Priorities.USER)` (5000), not pre-matching | Runs **after** `ApiKeyFilter`. Reuses a safe `X-Correlation-Id` (`[A-Za-z0-9._-]{1,64}`) or generates a UUID and sets it on every response (the response filter resolves it itself when an earlier filter aborted). Writes one access-log line per request: `METHOD path -> status [correlation-id] duration ms` |
| `@Compress` | `@NameBinding` annotation | Only on `GET /products` |
| `GzipWriterInterceptor` | `WriterInterceptor` bound by `@Compress` | Gzips when `Accept-Encoding` allows gzip (honours `q=0`); sets `Content-Encoding: gzip` and `Vary: Accept-Encoding` |


## 1. API-key authentication (`ApiKeyFilter`)

### 1.1 GET is public (no key)

```bash
curl -i http://localhost:8080/api/products
```

```
HTTP/1.1 200 OK
X-Correlation-Id: dd3f17e2-a458-428f-a734-4d4621704bfc
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Vary: Accept-Encoding
Content-Type: application/json
Content-Length: 184

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0}]
```

### 1.2 HEAD is public (no key)

```bash
curl -I http://localhost:8080/api/products
```

```
HTTP/1.1 200 OK
X-Correlation-Id: 900fa95d-3b0b-4e22-9fa1-1ab3ae3dcde0
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Vary: Accept-Encoding
Content-Length: 184
Content-Type: application/json
```

### 1.3 POST without key → 401 + `WWW-Authenticate` + problem+json

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Teapot","sku":"TP-01","price":18.00,"stock":5}'
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: 195c320b-4ae9-4379-a15d-0b691eac82b2
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 132

{"detail":"Missing or invalid X-Api-Key header","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

### 1.4 POST with wrong key → 401

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-Api-Key: wrong" \
  -d '{"name":"Teapot","sku":"TP-01","price":18.00,"stock":5}'
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: 568dce99-1525-4179-9a24-3a7ed23cb4bc
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 132

{"detail":"Missing or invalid X-Api-Key header","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

### 1.5 POST with default key `demo-123` → 201

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-Api-Key: demo-123" \
  -d '{"name":"Teapot","sku":"TP-01","price":18.00,"stock":5}'
```

```
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/4
X-Correlation-Id: 127c72f4-5930-48c9-95d5-2fccc9f540ea
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 62

{"id":4,"name":"Teapot","price":18.00,"sku":"TP-01","stock":5}
```

### 1.6 DELETE without key → 401

```bash
curl -i -X DELETE http://localhost:8080/api/products/4
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: b70d9518-0e15-4217-8931-6a207f1e4d33
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 134

{"detail":"Missing or invalid X-Api-Key header","instance":"/api/products/4","status":401,"title":"Unauthorized","type":"about:blank"}
```

### 1.7 DELETE with key → 204

```bash
curl -i -X DELETE http://localhost:8080/api/products/4 -H "X-Api-Key: demo-123"
```

```
HTTP/1.1 204 No Content
X-Correlation-Id: a9b5996c-e7c0-4a61-b979-8e2ce2df2e4f
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
```


### 1.8 Comma-separated keys: `shop.api.keys=alpha-1,beta-2`

Server restarted with:

```bash
mvn -q compile exec:java "-Dshop.api.keys=alpha-1,beta-2"
```

Key `alpha-1` → 201

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-Api-Key: alpha-1" \
  -d '{"name":"Cup","sku":"CP-01","price":3.00,"stock":1}'
```

```
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/4
X-Correlation-Id: b4c0506f-47e8-4302-8409-054706b4cb0c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 58

{"id":4,"name":"Cup","price":3.00,"sku":"CP-01","stock":1}
```

Key `beta-2` → 201

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-Api-Key: beta-2" \
  -d '{"name":"Bowl","sku":"BW-01","price":5.00,"stock":2}'
```

```
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/5
X-Correlation-Id: e5432ceb-1e3c-4a76-889c-1ce849924ae8
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 59

{"id":5,"name":"Bowl","price":5.00,"sku":"BW-01","stock":2}
```

Default `demo-123` is rejected once the property is set → 401

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-Api-Key: demo-123" \
  -d '{"name":"Plate","sku":"PL-01","price":4.00,"stock":3}'
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: 25397b5a-4fd5-4da1-a440-69bd6a618ef2
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 132

{"detail":"Missing or invalid X-Api-Key header","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

Access log of the restarted server:

```
INFO [edu.itc.shop.access] POST /api/products -> 201 [b4c0506f-47e8-4302-8409-054706b4cb0c] 37 ms
INFO [edu.itc.shop.access] POST /api/products -> 201 [e5432ceb-1e3c-4a76-889c-1ce849924ae8] 2 ms
INFO [edu.itc.shop.access] POST /api/products -> 401 [25397b5a-4fd5-4da1-a440-69bd6a618ef2] 3 ms
```


## 2. Correlation ID (`CorrelationFilter`)

### 2.1 Client-supplied ID is echoed

```bash
curl -i http://localhost:8080/api/products/1 -H "X-Correlation-Id: trace-42"
```

```
HTTP/1.1 200 OK
X-Correlation-Id: trace-42
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

### 2.2 Unsafe ID is replaced with a fresh UUID

```bash
curl -i http://localhost:8080/api/products/1 -H "X-Correlation-Id: bad id<script>"
```

```
HTTP/1.1 200 OK
X-Correlation-Id: 4170b4f5-8f2c-41a2-b259-4b50668302f4
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

### 2.3 ID kept on a 401 (ApiKeyFilter aborted before CorrelationFilter's request filter ran)

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-Correlation-Id: trace-401" \
  -d '{"name":"Teapot","sku":"TP-01","price":18.00,"stock":5}'
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: trace-401
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 132

{"detail":"Missing or invalid X-Api-Key header","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

### 2.4 ID on a 404 error

```bash
curl -i http://localhost:8080/api/products/999 -H "X-Correlation-Id: trace-404"
```

```
HTTP/1.1 404 Not Found
X-Correlation-Id: trace-404
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 119

{"detail":"Product 999 not found","instance":"/api/products/999","status":404,"title":"Not Found","type":"about:blank"}
```

Without the header a UUID is generated — see every other response in this file, including the 401s in 1.3, 1.4 and 1.6.


## 3. CORS (`CorsFilter`)

### 3.1 Preflight → 200 with CORS headers (no API key needed)

```bash
curl -i -X OPTIONS http://localhost:8080/api/products \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type, X-Api-Key"
```

```
HTTP/1.1 200 OK
X-Correlation-Id: 8f856c17-e337-4be1-9760-c14671fe4d6c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Length: 0
```

### 3.2 CORS headers on a normal response

```bash
curl -i http://localhost:8080/api/products/1 -H "Origin: http://localhost:5173"
```

```
HTTP/1.1 200 OK
X-Correlation-Id: 681b95d8-2d49-40f3-9e90-2a0f16794aeb
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

### 3.3 CORS headers on an unauthorized response

```bash
curl -i -X DELETE http://localhost:8080/api/products/1 -H "Origin: http://localhost:5173"
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: 1797036c-0ed5-4984-bbef-b6b12717f22c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 134

{"detail":"Missing or invalid X-Api-Key header","instance":"/api/products/1","status":401,"title":"Unauthorized","type":"about:blank"}
```


## 4. Gzip (`@Compress` on `GET /products` + `GzipWriterInterceptor`)

### 4.1 `Accept-Encoding: gzip` → `Content-Encoding: gzip` + `Vary: Accept-Encoding` (184 → 123 bytes)

```bash
curl -s -D - -o /tmp/list.gz http://localhost:8080/api/products -H "Accept-Encoding: gzip"
file /tmp/list.gz; gunzip -c /tmp/list.gz
```

```
HTTP/1.1 200 OK
X-Correlation-Id: e9750a6f-f168-44c8-aa3f-d4d61da8b6d8
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Vary: Accept-Encoding
Content-Encoding: gzip
Content-Type: application/json
Content-Length: 123

/tmp/list.gz: gzip compressed data, original size modulo 2^32 184
[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0}]
```

### 4.2 `--compressed` (curl decodes automatically)

```bash
curl -i --compressed http://localhost:8080/api/products
```

```
HTTP/1.1 200 OK
X-Correlation-Id: da5349b4-3f5d-479e-99cd-28f1dd0409e8
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Vary: Accept-Encoding
Content-Encoding: gzip
Content-Type: application/json
Content-Length: 123

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0}]
```

### 4.3 `gzip;q=0` refuses gzip → plain body

```bash
curl -i http://localhost:8080/api/products -H "Accept-Encoding: gzip;q=0"
```

```
HTTP/1.1 200 OK
X-Correlation-Id: a4c85986-3261-4a26-b3a2-4ea410759671
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Vary: Accept-Encoding
Content-Type: application/json
Content-Length: 184

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0}]
```

### 4.4 `GET /products/{id}` has no `@Compress` → never gzipped

```bash
curl -i http://localhost:8080/api/products/1 -H "Accept-Encoding: gzip"
```

```
HTTP/1.1 200 OK
X-Correlation-Id: c3b39aea-a399-4053-8f7d-fb0995a5898d
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

Without `Accept-Encoding` the list is plain (see 1.1: `Content-Length: 184`, no `Content-Encoding`).


## 5. Tasks 1–3 regression

### 5.1 Bean Validation → 400 with field errors

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-Api-Key: demo-123" \
  -d '{"name":"","sku":"invalid","price":-5}'
```

```
HTTP/1.1 400 Bad Request
X-Correlation-Id: 474ad5c2-05a0-4cba-8e70-f0312cb97ecf
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Connection: close
Content-Length: 353

{"detail":"One or more fields are invalid","errors":[{"field":"create.arg0.sku","message":"must match \"[A-Z]{2}-\\d{2}\""},{"field":"create.arg0.price","message":"must be greater than or equal to 0.01"},{"field":"create.arg0.name","message":"must not be blank"}],"instance":"/api/products","status":400,"title":"Validation Failed","type":"about:blank"}
```

### 5.2 Money param converter → 400

```bash
curl -i "http://localhost:8080/api/products?minPrice=abc"
```

```
HTTP/1.1 400 Bad Request
X-Correlation-Id: 72cfe66d-9cd3-427f-8b64-f51b9502101a
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Vary: Accept-Encoding
Content-Type: application/problem+json
Connection: close
Content-Length: 144

{"detail":"Invalid money format. Use the format '12.50 EUR'","instance":"/api/products","status":400,"title":"Bad Request","type":"about:blank"}
```

### 5.3 Filter + sort

```bash
curl -i "http://localhost:8080/api/products?q=tea&sort=name,asc"
```

```
HTTP/1.1 200 OK
X-Correlation-Id: 9fe0991e-bc24-4023-8299-8bf1b019571f
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Vary: Accept-Encoding
Content-Type: application/json
Content-Length: 60

[{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0}]
```

### 5.4 Unknown sort field → 400

```bash
curl -i "http://localhost:8080/api/products?sort=colour,asc"
```

```
HTTP/1.1 400 Bad Request
X-Correlation-Id: 28bebca4-3728-4e0d-a23a-8043cf719a28
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Vary: Accept-Encoding
Content-Type: application/problem+json
Connection: close
Content-Length: 122

{"detail":"Unknown sort field: colour","instance":"/api/products","status":400,"title":"Bad Request","type":"about:blank"}
```

### 5.5 PUT full replace

```bash
curl -i -X PUT http://localhost:8080/api/products/2 \
  -H "Content-Type: application/json" -H "X-Api-Key: demo-123" \
  -d '{"name":"Big Mug","sku":"MG-01","price":7.50,"stock":40}'
```

```
HTTP/1.1 200 OK
X-Correlation-Id: 084b4f6f-30b3-49cb-834f-9d0649461483
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":2,"name":"Big Mug","price":7.50,"sku":"MG-01","stock":40}
```

### 5.6 PATCH insufficient stock → 409

```bash
curl -i -X PATCH http://localhost:8080/api/products/3/stock \
  -H "Content-Type: application/json" -H "X-Api-Key: demo-123" -d '{"delta":-1}'
```

```
HTTP/1.1 409 Conflict
X-Correlation-Id: 18b1e729-4bbb-4052-a53c-09536a0f5730
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 119

{"detail":"Insufficient stock","instance":"/api/products/3/stock","status":409,"title":"Conflict","type":"about:blank"}
```

### 5.7 XML content negotiation

```bash
curl -i http://localhost:8080/api/products/2 -H "Accept: application/xml"
```

```
HTTP/1.1 200 OK
X-Correlation-Id: 63727247-04c5-4f1d-8e8b-684085103501
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/xml
Content-Length: 153

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="2"><name>Big Mug</name><sku>MG-01</sku><price>7.50</price><stock>40</stock></product>
```


## 6. Access log — one line per request

Server log (`edu.itc.shop.access`) for sections 1.1–5.7 (excluding 1.8), in request order:

```
INFO [edu.itc.shop.access] GET /api/products -> 200 [dd3f17e2-a458-428f-a734-4d4621704bfc] 1 ms
INFO [edu.itc.shop.access] HEAD /api/products -> 200 [900fa95d-3b0b-4e22-9fa1-1ab3ae3dcde0] 1 ms
INFO [edu.itc.shop.access] POST /api/products -> 401 [195c320b-4ae9-4379-a15d-0b691eac82b2] 1 ms
INFO [edu.itc.shop.access] POST /api/products -> 401 [568dce99-1525-4179-9a24-3a7ed23cb4bc] 0 ms
INFO [edu.itc.shop.access] POST /api/products -> 201 [127c72f4-5930-48c9-95d5-2fccc9f540ea] 27 ms
INFO [edu.itc.shop.access] DELETE /api/products/4 -> 401 [b70d9518-0e15-4217-8931-6a207f1e4d33] 0 ms
INFO [edu.itc.shop.access] DELETE /api/products/4 -> 204 [a9b5996c-e7c0-4a61-b979-8e2ce2df2e4f] 3 ms
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 [trace-42] 1 ms
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 [4170b4f5-8f2c-41a2-b259-4b50668302f4] 0 ms
INFO [edu.itc.shop.access] POST /api/products -> 401 [trace-401] 0 ms
INFO [edu.itc.shop.access] GET /api/products/999 -> 404 [trace-404] 6 ms
INFO [edu.itc.shop.access] OPTIONS /api/products -> 200 [8f856c17-e337-4be1-9760-c14671fe4d6c] 0 ms
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 [681b95d8-2d49-40f3-9e90-2a0f16794aeb] 0 ms
INFO [edu.itc.shop.access] DELETE /api/products/1 -> 401 [1797036c-0ed5-4984-bbef-b6b12717f22c] 0 ms
INFO [edu.itc.shop.access] GET /api/products -> 200 [e9750a6f-f168-44c8-aa3f-d4d61da8b6d8] 1 ms
INFO [edu.itc.shop.access] GET /api/products -> 200 [da5349b4-3f5d-479e-99cd-28f1dd0409e8] 1 ms
INFO [edu.itc.shop.access] GET /api/products -> 200 [a4c85986-3261-4a26-b3a2-4ea410759671] 1 ms
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 [c3b39aea-a399-4053-8f7d-fb0995a5898d] 0 ms
INFO [edu.itc.shop.access] POST /api/products -> 400 [474ad5c2-05a0-4cba-8e70-f0312cb97ecf] 33 ms
INFO [edu.itc.shop.access] GET /api/products?minPrice=abc -> 400 [72cfe66d-9cd3-427f-8b64-f51b9502101a] 4 ms
INFO [edu.itc.shop.access] GET /api/products?q=tea&sort=name,asc -> 200 [9fe0991e-bc24-4023-8299-8bf1b019571f] 1 ms
INFO [edu.itc.shop.access] GET /api/products?sort=colour,asc -> 400 [28bebca4-3728-4e0d-a23a-8043cf719a28] 1 ms
INFO [edu.itc.shop.access] PUT /api/products/2 -> 200 [084b4f6f-30b3-49cb-834f-9d0649461483] 2 ms
INFO [edu.itc.shop.access] PATCH /api/products/3/stock -> 409 [18b1e729-4bbb-4052-a53c-09536a0f5730] 18 ms
INFO [edu.itc.shop.access] GET /api/products/2 -> 200 [63727247-04c5-4f1d-8e8b-684085103501] 0 ms
```

25 HTTP requests → 25 access-log lines, including every 401 and the preflight (4.1 is one request; `file`/`gunzip` run locally).

After `minPrice=abc` Jersey additionally prints its own HK2 `WARNING` stack trace for the converter's `BadRequestException`; this is pre-existing Task 3 behaviour and the client still receives the 400 problem response.
