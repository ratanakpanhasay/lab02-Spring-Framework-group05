# Lab 04 — Task 2: CRUD, HTTP Status Codes, JSON and XML

## 1. Purpose and setup

Task 2 extends the JAX-RS product API with POST, PUT, PATCH and DELETE operations. It uses `ProductDto` for JSON and XML representations, preserves the required HTTP status codes, and handles duplicate SKUs and insufficient stock with `409 Conflict`.

- Base URL: `http://localhost:8080/api`
- Product collection: `/api/products`
- Start server from `shop-rest/`: `mvn -q compile exec:java`
- HTTP client: Windows CMD `curl -i` (with the Grizzly server running in another terminal)

## 2. Endpoint summary

| Method | Path | Expected success | Error behavior |
|---|---|---|---|
| GET | `/products` | 200, product DTO list | — |
| GET | `/products/{id}` | 200, product DTO | 404 if missing |
| POST | `/products` | 201, `Location` and product DTO | 409 for duplicate SKU; 415 for unsupported media type |
| PUT | `/products/{id}` | 200, updated product DTO | 404 if missing |
| PATCH | `/products/{id}/stock` | 200, updated product DTO | 409 if stock would become negative |
| DELETE | `/products/{id}` | 204, empty body | 404 if missing |

The `ProductDto` response does not expose the domain model's `version` field. JSON and XML are supported for product representations; PATCH stock uses a JSON request body.

## 3. Actual HTTP transcript

The following sections reproduce the **actual response headers and bodies** saved during testing in `task2-actual-http(1).txt`. Some requests changed the in-memory state before later captures (e.g., Mug had previously been updated to 45 units). Product IDs and quantities below reflect that test session.

### 3.1 GET all products — 200 OK

```cmd
curl -i http://localhost:8080/api/products
```

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 256

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":45},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0},{"id":4,"name":"Electric Teapot","price":35.00,"sku":"TP-01","stock":8}]
```

### 3.2 POST JSON — 201 Created, Location header

```cmd
curl -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d "{\"name\":\"Notebook\",\"sku\":\"NB-01\",\"price\":12.50,\"stock\":10}"
```

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/6
Content-Type: application/json
Content-Length: 65

{"id":6,"name":"Notebook","price":12.50,"sku":"NB-01","stock":10}
```

The `Location` header identifies the newly created product. Its identifier is assigned by the repository.

### 3.3 Duplicate SKU — 409 Conflict

The identical Notebook POST request was repeated.

```http
HTTP/1.1 409 Conflict
Content-Length: 0
```

### 3.4 GET XML — 200 OK

```cmd
curl -i http://localhost:8080/api/products/4 -H "Accept: application/xml"
```

```http
HTTP/1.1 200 OK
Content-Type: application/xml
Content-Length: 161

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="4"><name>Electric Teapot</name><sku>TP-01</sku><price>35.00</price><stock>8</stock></product>
```

### 3.5 PATCH stock successfully — 200 OK

```cmd
curl -i -X PATCH http://localhost:8080/api/products/2/stock -H "Content-Type: application/json" -d "{\"delta\":5}"
```

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 59

{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":50}
```

This request increased Mug stock from 45 to 50.

### 3.6 PATCH with insufficient stock — 409 Conflict

```cmd
curl -i -X PATCH http://localhost:8080/api/products/3/stock -H "Content-Type: application/json" -d "{\"delta\":-1}"
```

```http
HTTP/1.1 409 Conflict
Content-Length: 0
```

Tea has zero stock; removing one unit was rejected.

### 3.7 DELETE existing product — 204 No Content

```cmd
curl -i -X DELETE http://localhost:8080/api/products/6
```

```http
HTTP/1.1 204 No Content
```

The successful DELETE response has no body.

## 4. Additional manually verified scenarios

The following scenarios were exercised during the same review and confirmed in the terminal conversation. **Their full raw HTTP headers were not retained in the uploaded transcript**, so they are not represented as captured HTTP exchanges.

| Scenario | Windows CMD request | Observed result |
|---|---|---|
| GET existing product | `curl -i http://localhost:8080/api/products/2` | 200 OK (confirmed earlier) |
| GET missing product | `curl -i http://localhost:8080/api/products/99` | 404 Not Found (confirmed earlier) |
| POST with XML | `curl -i -X POST http://localhost:8080/api/products -H "Content-Type: application/xml" -H "Accept: application/xml" -d "<product><name>Cup</name><sku>CP-01</sku><price>8.50</price><stock>10</stock></product>"` | 201 Created (confirmed earlier) |
| GET created XML product | `curl -i http://localhost:8080/api/products/5 -H "Accept: application/xml"` | 200 OK, XML response (confirmed earlier) |
| PUT existing product | `curl -i -X PUT http://localhost:8080/api/products/4 -H "Content-Type: application/json" -d "{\"name\":\"Electric Teapot\",\"sku\":\"TP-01\",\"price\":35.00,\"stock\":8}"` | Updated product JSON returned; reported as 200 OK |
| DELETE product again | `curl -i -X DELETE http://localhost:8080/api/products/6` | 404 Not Found (confirmed earlier) |
| Unsupported media type | `curl -i -X POST http://localhost:8080/api/products -H "Content-Type: text/plain" -d "Teapot"` | 415 Unsupported Media Type (confirmed earlier) |

**Not confirmed by an actual result:** PUT `/api/products/999` → 404. This is an expected Task 2 behavior, but the conversation did not include its response. It is not marked as passed below.

## 5. Verification summary

| Requirement | Expected status | Evidence | Outcome |
|---|---|---|---|
| GET product list | 200 | Captured response and headers | Pass |
| GET existing product | 200 | Earlier interactive check | Pass |
| GET missing product | 404 | Earlier interactive check | Pass |
| POST JSON and `Location` | 201 | Captured response and headers | Pass |
| Duplicate SKU rejection | 409 | Captured response and headers | Pass |
| XML POST | 201 | Earlier interactive check | Pass |
| XML GET | 200 | Captured response and headers | Pass |
| PUT existing product | 200 | Updated JSON observed during interactive check | Pass (status reported) |
| PUT missing product | 404 | No confirmed response retained | Not verified |
| PATCH stock success | 200 | Captured response and headers | Pass |
| PATCH insufficient stock | 409 | Captured response and headers | Pass |
| DELETE existing product | 204 | Captured response and headers | Pass |
| DELETE missing product | 404 | Earlier interactive check | Pass |
| POST text/plain | 415 | Earlier interactive check | Pass |

## 6. Conclusion

The recorded responses demonstrate the required creation, duplicate-SKU handling, JSON and XML output, stock adjustment, insufficient-stock rejection, and successful deletion. Additional interactive checks support XML input, PUT replacement, missing-product handling, and unsupported-media-type rejection. The PUT-missing-product case remains unverified in the evidence available for this report.

Task 3 will introduce formal Bean Validation and RFC 9457 `application/problem+json` error representations; those features are outside Task 2's required implementation.
