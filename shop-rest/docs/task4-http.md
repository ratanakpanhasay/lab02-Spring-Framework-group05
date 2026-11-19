# Lab-04 Task 4 — Providers and Filters: HTTP Test Evidence

Captured on 2026-11-19 against `mvn -q compile exec:java`
(`http://localhost:8080/api/`) with curl 8.22.0 (Git Bash, Windows 11).
API key: `dev-secret` (default; override with the `SHOP_API_KEY` env variable).

Automated check: `mvn -q verify` → `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`
(`ProvidersTest`).

| Provider | Kind | Behaviour |
|---|---|---|
| `CorrelationFilter` | `@PreMatching` request + response filter | Reuses a safe incoming `X-Correlation-Id` (`[A-Za-z0-9._-]{1,64}`) or generates a UUID; echoes it on every response |
| `CorsFilter` | `@PreMatching` request + response filter | Answers preflight `OPTIONS` with 204; adds `Access-Control-*` headers only for allowed origins (`http://localhost:3000`, `http://localhost:5173`) |
| `ApiKeyFilter` | request filter, `Priorities.AUTHENTICATION` | `GET`/`HEAD`/`OPTIONS` are public; other methods require `X-API-Key`, else 401 `application/problem+json` |
| `@Compress` | `@NameBinding` annotation | Placed on `GET /products` and `GET /products/{id}` |
| `GzipWriterInterceptor` | `WriterInterceptor`, bound by `@Compress` | Gzips the body when `Accept-Encoding` allows gzip; always adds `Vary: Accept-Encoding` |

---

## 1. API-key authentication

### 1.1 GET without key is public

```bash
curl -i http://localhost:8080/api/products
```

```
HTTP/1.1 200 OK
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: c5e2e03f-b488-490b-bed2-d426c653e32d
Content-Type: application/json
Content-Length: 184

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0}]
```

### 1.2 POST without key → 401

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Teapot","sku":"TP-01","price":18.00,"stock":5}'
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey header="X-API-Key"
Vary: Origin
X-Correlation-Id: 6ea1bd4f-e3ad-401f-a572-ba0d3a88719c
Content-Type: application/problem+json
Content-Length: 132

{"detail":"Missing or invalid X-API-Key header","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

### 1.3 POST with wrong key → 401

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-API-Key: wrong" \
  -d '{"name":"Teapot","sku":"TP-01","price":18.00,"stock":5}'
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey header="X-API-Key"
Vary: Origin
X-Correlation-Id: 1dc8eee1-83fc-4ef5-a210-264a281a5f66
Content-Type: application/problem+json
Content-Length: 132

{"detail":"Missing or invalid X-API-Key header","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

### 1.4 POST with correct key → 201

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-API-Key: dev-secret" \
  -d '{"name":"Teapot","sku":"TP-01","price":18.00,"stock":5}'
```

```
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/4
Vary: Origin
X-Correlation-Id: 9583a949-87a3-4c4c-92c2-46a546542b0b
Content-Type: application/json
Content-Length: 62

{"id":4,"name":"Teapot","price":18.00,"sku":"TP-01","stock":5}
```

### 1.5 DELETE without key → 401, with key → 204

```bash
curl -i -X DELETE http://localhost:8080/api/products/4
```

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey header="X-API-Key"
Vary: Origin
X-Correlation-Id: def62e6c-3e58-460d-9bde-892ceb12d101
Content-Type: application/problem+json
Content-Length: 134

{"detail":"Missing or invalid X-API-Key header","instance":"/api/products/4","status":401,"title":"Unauthorized","type":"about:blank"}
```

```bash
curl -i -X DELETE http://localhost:8080/api/products/4 -H "X-API-Key: dev-secret"
```

```
HTTP/1.1 204 No Content
Vary: Origin
X-Correlation-Id: 820bc886-d6eb-42b7-a0df-dae83a7dafa5
```

---

## 2. Correlation ID

### 2.1 Client-supplied ID is echoed

```bash
curl -i http://localhost:8080/api/products/1 -H "X-Correlation-Id: demo-123"
```

```
HTTP/1.1 200 OK
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: demo-123
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
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: 6d8bfc1d-f8ad-47f2-b7f0-e47087a06cd2
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

### 2.3 ID is also present on error responses

```bash
curl -i http://localhost:8080/api/products/999 -H "X-Correlation-Id: trace-404"
```

```
HTTP/1.1 404 Not Found
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: trace-404
Content-Type: application/problem+json
Content-Length: 119

{"detail":"Product 999 not found","instance":"/api/products/999","status":404,"title":"Not Found","type":"about:blank"}
```

When no header is sent, a UUID is generated (see every other response in this file).

---

## 3. CORS

### 3.1 Preflight from an allowed origin → 204 with CORS headers (no API key needed)

```bash
curl -i -X OPTIONS http://localhost:8080/api/products \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type, X-API-Key"
```

```
HTTP/1.1 204 No Content
Vary: Origin
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Expose-Headers: Location, X-Correlation-Id
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Accept-Encoding, X-API-Key, X-Correlation-Id
Access-Control-Max-Age: 3600
X-Correlation-Id: caebd86c-bf34-4851-a7a6-0f4d86682fd5
```

### 3.2 Simple GET from an allowed origin

```bash
curl -i http://localhost:8080/api/products/1 -H "Origin: http://localhost:5173"
```

```
HTTP/1.1 200 OK
Vary: Origin
Vary: Accept-Encoding
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Expose-Headers: Location, X-Correlation-Id
X-Correlation-Id: e3ef1063-e08f-4f76-a62b-19ef1e4ea63a
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

### 3.3 Disallowed origin gets no `Access-Control-Allow-Origin`

```bash
curl -i http://localhost:8080/api/products/1 -H "Origin: http://evil.example"
```

```
HTTP/1.1 200 OK
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: 4cbbac3e-c6f0-42b5-9b8d-5aa702976a60
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

```bash
curl -i -X OPTIONS http://localhost:8080/api/products \
  -H "Origin: http://evil.example" -H "Access-Control-Request-Method: DELETE"
```

```
HTTP/1.1 204 No Content
Vary: Origin
X-Correlation-Id: 5846cfd4-ca07-4ebb-b938-39ab2b2fcdb3
```

The browser blocks the request because no allow headers are returned.

---

## 4. Gzip compression (`@Compress` + `GzipWriterInterceptor`)

### 4.1 Gzip requested → compressed body (184 → 123 bytes)

```bash
curl -s -D - -o /tmp/list.gz http://localhost:8080/api/products -H "Accept-Encoding: gzip"
file /tmp/list.gz && gunzip -c /tmp/list.gz
```

```
HTTP/1.1 200 OK
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: f23672c8-a11d-44a3-9a81-3845d8e44f7d
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
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: 79a54c4b-25af-4ce4-8256-cf4067b5642b
Content-Encoding: gzip
Content-Type: application/json
Content-Length: 123

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0}]
```

### 4.3 No gzip without `Accept-Encoding` (see 1.1: `Content-Length: 184`, no `Content-Encoding`)

### 4.4 `gzip;q=0` refuses gzip → plain body

```bash
curl -i http://localhost:8080/api/products -H "Accept-Encoding: gzip;q=0"
```

```
HTTP/1.1 200 OK
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: 0448610a-7a38-4a92-bf34-24837c065539
Content-Type: application/json
Content-Length: 184

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0}]
```

### 4.5 Gzip also works for XML on `GET /products/{id}`

```bash
curl -s -D - -o /tmp/one.gz http://localhost:8080/api/products/1 \
  -H "Accept-Encoding: gzip" -H "Accept: application/xml"
gunzip -c /tmp/one.gz
```

```
HTTP/1.1 200 OK
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: 7dd2e1cd-7e7e-477a-bff5-5fae0463b9c7
Content-Encoding: gzip
Content-Type: application/xml
Content-Length: 144

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="1"><name>Kettle</name><sku>KT-01</sku><price>24.90</price><stock>12</stock></product>
```

Endpoints without `@Compress` (POST/PUT/PATCH/DELETE) are never compressed and
carry no `Vary: Accept-Encoding` (see sections 1.2–1.5).

---

## 5. Tasks 1–3 regression (still working behind the filters)

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" -H "X-API-Key: dev-secret" \
  -d '{"name":"","sku":"invalid","price":-5}'
```

```
HTTP/1.1 400 Bad Request
Vary: Origin
X-Correlation-Id: a883e980-d928-4c80-a617-f99226668b98
Content-Type: application/problem+json
Connection: close
Content-Length: 353

{"detail":"One or more fields are invalid","errors":[{"message":"must be greater than or equal to 0.01","field":"create.arg0.price"},{"message":"must match \"[A-Z]{2}-\\d{2}\"","field":"create.arg0.sku"},{"message":"must not be blank","field":"create.arg0.name"}],"instance":"/api/products","status":400,"title":"Validation Failed","type":"about:blank"}
```

```bash
curl -i "http://localhost:8080/api/products?minPrice=abc"
```

```
HTTP/1.1 400 Bad Request
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: 94df8cb9-b27a-47a5-bf9a-842c5134e8ff
Content-Type: application/problem+json
Connection: close
Content-Length: 144

{"detail":"Invalid money format. Use the format '12.50 EUR'","instance":"/api/products","status":400,"title":"Bad Request","type":"about:blank"}
```

```bash
curl -i "http://localhost:8080/api/products?q=tea&sort=name,asc"
```

```
HTTP/1.1 200 OK
Vary: Origin
Vary: Accept-Encoding
X-Correlation-Id: dfbfed84-1b4e-4ef7-912c-2a1b70988059
Content-Type: application/json
Content-Length: 60

[{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0}]
```

```bash
curl -i -X PATCH http://localhost:8080/api/products/3/stock \
  -H "Content-Type: application/json" -H "X-API-Key: dev-secret" -d '{"delta":-1}'
```

```
HTTP/1.1 409 Conflict
Vary: Origin
X-Correlation-Id: 8f59de59-7aa3-43d1-aca5-baf69859e1fd
Content-Type: application/problem+json
Content-Length: 119

{"detail":"Insufficient stock","instance":"/api/products/3/stock","status":409,"title":"Conflict","type":"about:blank"}
```
