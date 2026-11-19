# Lab-04 Task 4 — Providers and Filters: HTTP Test Evidence

Captured on 2026-11-19 against the real server started with
`mvn -q compile exec:java` (`http://localhost:8080/api/`), curl 8.22.0, Git Bash on Windows 11.
`-Djava.util.logging.SimpleFormatter.format="%4$s [%3$s] %5$s%n"` was added only for one-line log output.

Automated check: `mvn -q verify` → `ProvidersTest` 22 tests and `ApiKeysPropertyTest` 2 tests,
`Failures: 0, Errors: 0, Skipped: 0`.

| Provider | Kind / order | Behaviour |
|---|---|---|
| `RequestTimingFilter` | `@PreMatching` request filter, `@Priority(1)` | Records the request start time first, so the duration is measured even when CORS or `ApiKeyFilter` abort the request |
| `ApiKeyFilter` | `@PreMatching` request filter, `@Priority(Priorities.AUTHENTICATION)` (1000) | Runs before resource matching, so unauthorized writes to unknown paths get 401, not 404. `GET`/`HEAD`/`OPTIONS` public; other methods need `X-Api-Key` matching one of the comma-separated keys in system property `shop.api.keys` (default `demo-123`), else 401 `application/problem+json` + `WWW-Authenticate: ApiKey realm="shop"` |
| `CorsFilter` | `@PreMatching` request + response filter | Answers preflight `OPTIONS` with **200**; adds to **every** response, including 401: `Access-Control-Allow-Origin: http://localhost:5173`, `Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE`, `Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match`, `Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id` |
| `CorrelationFilter` | request + response filter, `@Priority(Priorities.USER)` (5000), not pre-matching | Runs **after** `ApiKeyFilter`. Reuses a safe `X-Correlation-Id` (`[A-Za-z0-9._-]{1,64}`) or generates a UUID and sets it on every response (the response filter resolves it itself when an earlier filter aborted). Writes one access-log line per request via `System.Logger`: `METHOD path -> status [correlation-id] duration ms` (path without query string) |
| `@Compress` | `@NameBinding` annotation | Only on `GET /products` |
| `GzipWriterInterceptor` | `WriterInterceptor` bound by `@Compress` | Gzips when `Accept-Encoding` allows gzip (honours `q=0`); sets `Content-Encoding: gzip` and `Vary: Accept-Encoding` |


## 1. API-key authentication (`ApiKeyFilter`)

### 1.1 GET is public (no key)

```bash
curl -i http://localhost:8080/api/products
```

```
HTTP/1.1 200 OK
X-Correlation-Id: c5a4da86-ca4f-4026-bf70-25c0c7e48467
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 3f49913d-8932-432d-9ed6-ae3f484d6b1e
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 79f92278-e573-445e-bdbf-d0321f685af3
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: d1206f8d-54ab-4bcd-99f9-84d8edf59b28
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: b0d3fed6-7a0c-4b5c-8db8-b0da5d55d577
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 8358521c-a4cb-409c-9278-74bffbef7688
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 32612d1c-a3d0-4d79-a55e-317400057b61
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
```

### 1.8 POST to an unmatched path without key → 401, not 404 (`ApiKeyFilter` is `@PreMatching`)

```bash
curl -i -X POST http://localhost:8080/api/no-such-resource \
  -H "Content-Type: application/json" -d '{}'
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: f818ed11-8e3d-4c3b-8380-e9a3341b2c22
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 140

{"detail":"Missing or invalid X-Api-Key header","instance":"/api/no-such-resource","status":401,"title":"Unauthorized","type":"about:blank"}
```


### 1.9 Comma-separated keys: `shop.api.keys=alpha-1,beta-2`

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
X-Correlation-Id: 1e509205-daa4-4b2b-b733-335164d12f96
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 72b05306-f000-4734-8cae-f6e03d90a68d
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 959946a4-fc9f-46ad-a484-7f62712c1ab8
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 132

{"detail":"Missing or invalid X-Api-Key header","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

Access log of the restarted server:

```
INFO [edu.itc.shop.access] POST /api/products -> 201 [1e509205-daa4-4b2b-b733-335164d12f96] 32 ms
INFO [edu.itc.shop.access] POST /api/products -> 201 [72b05306-f000-4734-8cae-f6e03d90a68d] 1 ms
INFO [edu.itc.shop.access] POST /api/products -> 401 [959946a4-fc9f-46ad-a484-7f62712c1ab8] 1 ms
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
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 906460a6-d8c0-43da-bb01-f3a7e26a1d74
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 654ad69f-f752-4433-b3d8-54bc8bf99679
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 781af9b8-b8cb-448c-a5f1-7f30a0e3a1f0
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: ad449504-7164-40bb-ad27-91b2df51e37e
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 526d044a-73c9-46a8-8f05-92c875547433
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 4924a130-08cb-41ad-b0ca-a53dcdb7abd8
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 6f3d4c99-2178-49b1-9908-8dbbefb80c60
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 722d3f69-aa25-4043-ba3e-5bd8131cc20d
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 2a44698b-c513-4e54-8532-94fc403e396c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Connection: close
Content-Length: 353

{"detail":"One or more fields are invalid","errors":[{"field":"create.arg0.name","message":"must not be blank"},{"field":"create.arg0.sku","message":"must match \"[A-Z]{2}-\\d{2}\""},{"field":"create.arg0.price","message":"must be greater than or equal to 0.01"}],"instance":"/api/products","status":400,"title":"Validation Failed","type":"about:blank"}
```

### 5.2 Money param converter → 400

```bash
curl -i "http://localhost:8080/api/products?minPrice=abc"
```

```
HTTP/1.1 400 Bad Request
X-Correlation-Id: 3d40ef0d-a61b-4fff-8df5-d83a8e79b63b
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 0e42d4dc-bd98-4bd6-90b8-5e212153c3a0
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 3c220dfa-1b49-41f7-96ff-ff38e64a0868
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 36d92970-a146-4635-98cb-45f9b9d33107
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 48195250-63b9-4bca-9b72-dc3a135fdde2
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
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
X-Correlation-Id: 914f3f79-dd43-46ff-b3f1-d17fd2ce7c2a
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/xml
Content-Length: 153

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="2"><name>Big Mug</name><sku>MG-01</sku><price>7.50</price><stock>40</stock></product>
```


## 6. Access log — one line per request

Server log (`edu.itc.shop.access`, written via `System.Logger`) for sections 1.1–5.7 (excluding 1.9), in request order; the path is logged without the query string:

```
INFO [edu.itc.shop.access] GET /api/products -> 200 [c5a4da86-ca4f-4026-bf70-25c0c7e48467] 1 ms
INFO [edu.itc.shop.access] HEAD /api/products -> 200 [3f49913d-8932-432d-9ed6-ae3f484d6b1e] 1 ms
INFO [edu.itc.shop.access] POST /api/products -> 401 [79f92278-e573-445e-bdbf-d0321f685af3] 1 ms
INFO [edu.itc.shop.access] POST /api/products -> 401 [d1206f8d-54ab-4bcd-99f9-84d8edf59b28] 0 ms
INFO [edu.itc.shop.access] POST /api/products -> 201 [b0d3fed6-7a0c-4b5c-8db8-b0da5d55d577] 28 ms
INFO [edu.itc.shop.access] DELETE /api/products/4 -> 401 [8358521c-a4cb-409c-9278-74bffbef7688] 0 ms
INFO [edu.itc.shop.access] DELETE /api/products/4 -> 204 [32612d1c-a3d0-4d79-a55e-317400057b61] 3 ms
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 [trace-42] 0 ms
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 [906460a6-d8c0-43da-bb01-f3a7e26a1d74] 0 ms
INFO [edu.itc.shop.access] POST /api/products -> 401 [trace-401] 0 ms
INFO [edu.itc.shop.access] GET /api/products/999 -> 404 [trace-404] 5 ms
INFO [edu.itc.shop.access] OPTIONS /api/products -> 200 [654ad69f-f752-4433-b3d8-54bc8bf99679] 0 ms
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 [781af9b8-b8cb-448c-a5f1-7f30a0e3a1f0] 0 ms
INFO [edu.itc.shop.access] DELETE /api/products/1 -> 401 [ad449504-7164-40bb-ad27-91b2df51e37e] 0 ms
INFO [edu.itc.shop.access] GET /api/products -> 200 [526d044a-73c9-46a8-8f05-92c875547433] 0 ms
INFO [edu.itc.shop.access] GET /api/products -> 200 [4924a130-08cb-41ad-b0ca-a53dcdb7abd8] 1 ms
INFO [edu.itc.shop.access] GET /api/products -> 200 [6f3d4c99-2178-49b1-9908-8dbbefb80c60] 1 ms
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 [722d3f69-aa25-4043-ba3e-5bd8131cc20d] 0 ms
INFO [edu.itc.shop.access] POST /api/products -> 400 [2a44698b-c513-4e54-8532-94fc403e396c] 36 ms
INFO [edu.itc.shop.access] GET /api/products -> 400 [3d40ef0d-a61b-4fff-8df5-d83a8e79b63b] 4 ms
INFO [edu.itc.shop.access] GET /api/products -> 200 [0e42d4dc-bd98-4bd6-90b8-5e212153c3a0] 1 ms
INFO [edu.itc.shop.access] GET /api/products -> 400 [3c220dfa-1b49-41f7-96ff-ff38e64a0868] 1 ms
INFO [edu.itc.shop.access] PUT /api/products/2 -> 200 [36d92970-a146-4635-98cb-45f9b9d33107] 2 ms
INFO [edu.itc.shop.access] PATCH /api/products/3/stock -> 409 [48195250-63b9-4bca-9b72-dc3a135fdde2] 16 ms
INFO [edu.itc.shop.access] GET /api/products/2 -> 200 [914f3f79-dd43-46ff-b3f1-d17fd2ce7c2a] 0 ms
INFO [edu.itc.shop.access] POST /api/no-such-resource -> 401 [f818ed11-8e3d-4c3b-8380-e9a3341b2c22] 0 ms
```

26 HTTP requests → 26 access-log lines, including every 401 and the preflight (4.1 is one request; `file`/`gunzip` run locally; the last line is 1.8, run at the end of the session).

After `minPrice=abc` Jersey additionally prints its own HK2 `WARNING` stack trace for the converter's `BadRequestException`; this is pre-existing Task 3 behaviour and the client still receives the 400 problem response.
