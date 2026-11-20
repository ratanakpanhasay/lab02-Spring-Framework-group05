# Lab 04 — Task 5: HTTP Transcript

Actual HTTP responses captured from the running Jersey/Grizzly application. Requests use Windows CMD syntax. Product data is stored in memory and may reset after restarting the server.

## 1. Pagination: page 1, size 10

**Request**

```cmd
curl -i "http://localhost:8080/api/products?page=1&size=10"
```

**Actual response**

```http
HTTP/1.1 200 OK
X-Total-Count: 33
Link: <http://localhost:8080/api/products?size=10&page=0>; rel="first"
Link: <http://localhost:8080/api/products?size=10&page=3>; rel="last"
Link: <http://localhost:8080/api/products?size=10&page=0>; rel="prev"
Link: <http://localhost:8080/api/products?size=10&page=2>; rel="next"
X-Correlation-Id: 07517ac9-7d1f-42e8-b8c3-ea553e519b22
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Vary: Accept-Encoding
Content-Type: application/json
Content-Length: 759

[{"id":20,"name":"Sample Product 17","price":27.00,"sku":"SP-17","stock":27},{"id":21,"name":"Sample Product 18","price":28.00,"sku":"SP-18","stock":28},{"id":22,"name":"Sample Product 19","price":29.00,"sku":"SP-19","stock":29},{"id":5,"name":"Sample Product 2","price":12.00,"sku":"SP-02","stock":12},{"id":23,"name":"Sample Product 20","price":30.00,"sku":"SP-20","stock":30},{"id":24,"name":"Sample Product 21","price":31.00,"sku":"SP-21","stock":31},{"id":25,"name":"Sample Product 22","price":32.00,"sku":"SP-22","stock":32},{"id":26,"name":"Sample Product 23","price":33.00,"sku":"SP-23","stock":33},{"id":27,"name":"Sample Product 24","price":34.00,"sku":"SP-24","stock":34},{"id":28,"name":"Sample Product 25","price":35.00,"sku":"SP-25","stock":35}]
```

## 2. GET product with ETag

**Request**

```cmd
curl -i http://localhost:8080/api/products/1
```

**Actual response**

```http
HTTP/1.1 200 OK
ETag: "1"
Cache-Control: private, no-transform, max-age=60
X-Correlation-Id: 5b9c6d89-60a1-4c16-91ad-c0fa3b0ebd95
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

## 3. GET with If-None-Match

**Request**

```cmd
curl -i http://localhost:8080/api/products/1 -H "If-None-Match: \"1\""
```

**Actual response**

```http
HTTP/1.1 304 Not Modified
ETag: "1"
Cache-Control: private, no-transform, max-age=60
X-Correlation-Id: 170b1966-46de-473d-8229-fb18071532f9
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
```

## 4. PUT without If-Match - 428

**Request**

```cmd
curl -i -X PUT http://localhost:8080/api/products/1 -H "X-Api-Key: demo-123" -H "Content-Type: application/json" -d "{\"name\":\"Kettle\",\"sku\":\"KT-01\",\"price\":29.90,\"stock\":12}"
```

**Actual response**

```http
HTTP/1.1 428 Precondition Required
X-Correlation-Id: 3d0e9d42-8999-47dd-9797-59a101ffefc3
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 135

{"detail":"If-Match header is required","instance":"/api/products/1","status":428,"title":"Precondition Required","type":"about:blank"}
```

## 5. PUT with stale ETag - 412

**Request**

```cmd
curl -i -X PUT http://localhost:8080/api/products/1 -H "X-Api-Key: demo-123" -H "Content-Type: application/json" -H "If-Match: \"999\"" -d "{\"name\":\"Kettle\",\"sku\":\"KT-01\",\"price\":29.90,\"stock\":12}"
```

**Actual response**

```http
HTTP/1.1 412 Precondition Failed
X-Correlation-Id: eb4c12ac-2b4d-43fb-8832-0ea429854b76
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 136

{"detail":"Product version does not match","instance":"/api/products/1","status":412,"title":"Precondition Failed","type":"about:blank"}
```

## 6. PUT with correct ETag - 200

**Request**

```cmd
curl -i -X PUT http://localhost:8080/api/products/1 -H "X-Api-Key: demo-123" -H "Content-Type: application/json" -H "If-Match: \"1\"" -d "{\"name\":\"Kettle\",\"sku\":\"KT-01\",\"price\":29.90,\"stock\":12}"
```

**Actual response**

```http
HTTP/1.1 200 OK
ETag: "2"
X-Correlation-Id: b1da1333-4f87-4554-8c63-f896bdfd9282
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":29.90,"sku":"KT-01","stock":12}
```

## 7. API v1 - JSON

**Request**

```cmd
curl -i http://localhost:8080/api/products/1 -H "Accept: application/json"
```

**Actual response**

```http
HTTP/1.1 200 OK
ETag: "2"
Cache-Control: private, no-transform, max-age=60
X-Correlation-Id: 08c62bc5-417d-4e9c-bb47-e72d92ebe23a
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":29.90,"sku":"KT-01","stock":12}
```

## 8. API v2 - Vendor JSON

**Request**

```cmd
curl -i http://localhost:8080/api/products/1 -H "Accept: application/vnd.shop.v2+json"
```

**Actual response**

```http
HTTP/1.1 200 OK
ETag: "2"
Cache-Control: private, no-transform, max-age=60
X-Correlation-Id: dc666f73-43bb-4562-8e5b-5b22501b367a
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/vnd.shop.v2+json
Content-Length: 108

{"available":true,"id":1,"name":"Kettle","price":{"amount":29.90,"currency":"EUR"},"sku":"KT-01","stock":12}
```

## 9. POST Review - 201

**Request**

```cmd
curl -i -X POST http://localhost:8080/api/products/1/reviews -H "X-Api-Key: demo-123" -H "Content-Type: application/json" -d "{\"stars\":5,\"author\":\"Dara\",\"text\":\"Good quality!\"}"
```

**Actual response**

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/1/reviews/3
X-Correlation-Id: b29c3887-1f16-4246-94a0-e080f440c03f
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 71

{"author":"Dara","id":3,"productId":1,"stars":5,"text":"Good quality!"}
```

## 10. GET Reviews - 200

**Request**

```cmd
curl -i http://localhost:8080/api/products/1/reviews
```

**Actual response**

```http
HTTP/1.1 200 OK
X-Correlation-Id: 1d1816b3-9c06-4788-b4db-d9be0df5e417
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 218

[{"author":"Sokha","id":1,"productId":1,"stars":5,"text":"Excellent kettle!"},{"author":"Jame","id":2,"productId":1,"stars":4,"text":"Love this"},{"author":"Dara","id":3,"productId":1,"stars":5,"text":"Good quality!"}]
```

## 11. Reviews for Unknown Product - 404

**Request**

```cmd
curl -i http://localhost:8080/api/products/999/reviews
```

**Actual response**

```http
HTTP/1.1 404 Not Found
X-Correlation-Id: daba885f-e792-4ca5-93d5-bfe13c004724
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 127

{"detail":"Product 999 not found","instance":"/api/products/999/reviews","status":404,"title":"Not Found","type":"about:blank"}
```

## Verification summary

| Feature | Recorded HTTP status |
|---|---|
| Pagination with navigation headers | 200 |
| GET with ETag | 200 |
| Conditional GET | 304 |
| PUT without If-Match | 428 |
| PUT with stale If-Match | 412 |
| PUT with valid If-Match | 200 |
| v1 and v2 representations | 200 / 200 |
| Create and list reviews | 201 / 200 |
| Reviews of unknown product | 404 |
