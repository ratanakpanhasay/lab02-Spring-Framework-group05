# Lab-04 Task 4 — Providers and Filters: HTTP Test Evidence

Captured on 2026-11-19 against the real server started with
`mvn -q compile exec:java` (`http://localhost:8080/api/`), curl 8.22.0, Git Bash on Windows 11.
`-Djava.util.logging.SimpleFormatter.format="%4$s [%3$s] %5$s%n"` was added only for one-line log output.

Automated check: `mvn -q verify` → `ProvidersTest` 19 tests and `ApiKeysPropertyTest` 2 tests,
`Failures: 0, Errors: 0, Skipped: 0`.

| Provider | Kind / order | Behaviour |
|---|---|---|
| `CorsFilter` | `@PreMatching` request + response filter | Answers preflight `OPTIONS` with **200**; adds `Access-Control-Allow-Origin: http://localhost:5173`, `-Allow-Methods`, `-Allow-Headers`, `-Expose-Headers` to **every** response, including 401 |
| `ApiKeyFilter` | request filter, `@Priority(Priorities.AUTHENTICATION)` (1000) | `GET`/`HEAD`/`OPTIONS` public; other methods need `X-Api-Key` matching one of the comma-separated keys in system property `shop.api.keys` (default `demo-123`), else 401 `application/problem+json` + `WWW-Authenticate` |
| `CorrelationFilter` | request + response filter, `@Priority(Priorities.USER)` (5000), not pre-matching | Runs **after** `ApiKeyFilter`. Reuses a safe `X-Correlation-Id` (`[A-Za-z0-9._-]{1,64}`) or generates a UUID and sets it on every response (the response filter resolves it itself when an earlier filter aborted). Writes one access-log line per request: `METHOD path -> status cid=ID` |
| `@Compress` | `@NameBinding` annotation | Only on `GET /products` |
| `GzipWriterInterceptor` | `WriterInterceptor` bound by `@Compress` | Gzips when `Accept-Encoding` allows gzip (honours `q=0`); sets `Content-Encoding: gzip` and `Vary: Accept-Encoding` |


## 1. API-key authentication (`ApiKeyFilter`)

### 1.1 GET is public (no key)

```bash
curl -i http://localhost:8080/api/products
```

```
HTTP/1.1 200 OK
X-Correlation-Id: 8306e2e7-3cc1-466e-81eb-f120fb49f038
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: bf102ce0-cd50-4481-9499-b1bf486a39d0
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
WWW-Authenticate: ApiKey realm="shop-rest"
X-Correlation-Id: ab711f25-4ff1-4136-b7c7-8ef7874bee4f
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
WWW-Authenticate: ApiKey realm="shop-rest"
X-Correlation-Id: c7f68c65-e80b-4dc9-b0bb-49cbf95f3479
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: 77cf8f90-0356-4f50-b8f7-bc09e9459a95
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
WWW-Authenticate: ApiKey realm="shop-rest"
X-Correlation-Id: 09369d94-c2bb-4081-8465-b85f8b1bd65d
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: b609c940-7382-45f9-8f9f-a0bf12e8b7e6
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: 7f992b9d-da70-4deb-9738-fb28515b9c83
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: f30dba5a-fcc2-4dde-9306-ab4c1893389e
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
WWW-Authenticate: ApiKey realm="shop-rest"
X-Correlation-Id: 36787510-5d12-4cdc-a91b-13ca3b47ed28
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 132

{"detail":"Missing or invalid X-Api-Key header","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

Access log of the restarted server:

```
INFO [edu.itc.shop.access] POST /api/products -> 201 cid=7f992b9d-da70-4deb-9738-fb28515b9c83
INFO [edu.itc.shop.access] POST /api/products -> 201 cid=f30dba5a-fcc2-4dde-9306-ab4c1893389e
INFO [edu.itc.shop.access] POST /api/products -> 401 cid=36787510-5d12-4cdc-a91b-13ca3b47ed28
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
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: c52fd33b-5564-4fe8-a56f-417e0ddb75a1
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
WWW-Authenticate: ApiKey realm="shop-rest"
X-Correlation-Id: trace-401
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: 74c72b62-83eb-458a-a67b-3f169229eb5d
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
Content-Length: 0
```

### 3.2 CORS headers on a normal response

```bash
curl -i http://localhost:8080/api/products/1 -H "Origin: http://localhost:5173"
```

```
HTTP/1.1 200 OK
X-Correlation-Id: aacb85e6-e320-40ec-9e09-8e43035c1ce1
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
WWW-Authenticate: ApiKey realm="shop-rest"
X-Correlation-Id: 1dc795b5-a35c-4d87-8623-06abb30aa2cc
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: d36ce374-a141-4a54-addb-49d60325f83d
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: e69ba5e9-edc1-4c92-869c-d6abf5e4c1f7
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: 2fd97dc1-4f9d-4916-8234-0619ac0021fd
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: d6477cce-7ce4-4870-b63f-0a244a69432c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: 7e060517-7c5f-49df-b053-6aaa65dd3443
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: 6b0fb481-d204-4781-b22a-c008690cc4fc
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: 5204a651-bbb9-45bb-8995-d46763085b0f
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: a945cf9d-da0d-48d2-a069-f929c63ec30e
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: 5ba73fef-07ab-4d6d-9ece-bbe39846d6d9
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: f6e0a01f-605f-4201-9b4b-5954dbf04560
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
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
X-Correlation-Id: ed76b05f-4144-49b9-90d7-0d29bd429193
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-Api-Key, X-Correlation-Id
Access-Control-Expose-Headers: Location, X-Correlation-Id
Content-Type: application/xml
Content-Length: 153

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="2"><name>Big Mug</name><sku>MG-01</sku><price>7.50</price><stock>40</stock></product>
```


## 6. Access log — one line per request

Server log (`edu.itc.shop.access`) for sections 1.1–5.7 (excluding 1.8), in request order:

```
INFO [edu.itc.shop.access] GET /api/products -> 200 cid=8306e2e7-3cc1-466e-81eb-f120fb49f038
INFO [edu.itc.shop.access] HEAD /api/products -> 200 cid=bf102ce0-cd50-4481-9499-b1bf486a39d0
INFO [edu.itc.shop.access] POST /api/products -> 401 cid=ab711f25-4ff1-4136-b7c7-8ef7874bee4f
INFO [edu.itc.shop.access] POST /api/products -> 401 cid=c7f68c65-e80b-4dc9-b0bb-49cbf95f3479
INFO [edu.itc.shop.access] POST /api/products -> 201 cid=77cf8f90-0356-4f50-b8f7-bc09e9459a95
INFO [edu.itc.shop.access] DELETE /api/products/4 -> 401 cid=09369d94-c2bb-4081-8465-b85f8b1bd65d
INFO [edu.itc.shop.access] DELETE /api/products/4 -> 204 cid=b609c940-7382-45f9-8f9f-a0bf12e8b7e6
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 cid=trace-42
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 cid=c52fd33b-5564-4fe8-a56f-417e0ddb75a1
INFO [edu.itc.shop.access] POST /api/products -> 401 cid=trace-401
INFO [edu.itc.shop.access] GET /api/products/999 -> 404 cid=trace-404
INFO [edu.itc.shop.access] OPTIONS /api/products -> 200 cid=74c72b62-83eb-458a-a67b-3f169229eb5d
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 cid=aacb85e6-e320-40ec-9e09-8e43035c1ce1
INFO [edu.itc.shop.access] DELETE /api/products/1 -> 401 cid=1dc795b5-a35c-4d87-8623-06abb30aa2cc
INFO [edu.itc.shop.access] GET /api/products -> 200 cid=d36ce374-a141-4a54-addb-49d60325f83d
INFO [edu.itc.shop.access] GET /api/products -> 200 cid=e69ba5e9-edc1-4c92-869c-d6abf5e4c1f7
INFO [edu.itc.shop.access] GET /api/products -> 200 cid=2fd97dc1-4f9d-4916-8234-0619ac0021fd
INFO [edu.itc.shop.access] GET /api/products/1 -> 200 cid=d6477cce-7ce4-4870-b63f-0a244a69432c
INFO [edu.itc.shop.access] POST /api/products -> 400 cid=7e060517-7c5f-49df-b053-6aaa65dd3443
INFO [edu.itc.shop.access] GET /api/products?minPrice=abc -> 400 cid=6b0fb481-d204-4781-b22a-c008690cc4fc
INFO [edu.itc.shop.access] GET /api/products?q=tea&sort=name,asc -> 200 cid=5204a651-bbb9-45bb-8995-d46763085b0f
INFO [edu.itc.shop.access] GET /api/products?sort=colour,asc -> 400 cid=a945cf9d-da0d-48d2-a069-f929c63ec30e
INFO [edu.itc.shop.access] PUT /api/products/2 -> 200 cid=5ba73fef-07ab-4d6d-9ece-bbe39846d6d9
INFO [edu.itc.shop.access] PATCH /api/products/3/stock -> 409 cid=f6e0a01f-605f-4201-9b4b-5954dbf04560
INFO [edu.itc.shop.access] GET /api/products/2 -> 200 cid=ed76b05f-4144-49b9-90d7-0d29bd429193
```

25 HTTP requests → 25 access-log lines, including every 401 and the preflight (4.1 is one request; `file`/`gunzip` run locally).

After `minPrice=abc` Jersey additionally prints its own HK2 `WARNING` stack trace for the converter's `BadRequestException`; this is pre-existing Task 3 behaviour and the client still receives the 400 problem response.
