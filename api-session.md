
# Task 4 — REST API Testing Session

## Environment

- Spring Boot: 4.1.1
- Java: 25
- Base URL: http://localhost:8080
- Testing tool: curl (Windows CMD)
- Database: H2

## 1. GET All Products

**Request**
```cmd
curl -i http://localhost:8080/api/products
```

**Result:** HTTP 200 OK

Returned all five sample products.

## 2. GET Products by Search

**Request**
```cmd
curl -i "http://localhost:8080/api/products?q=mouse"
```

**Result:** HTTP 200 OK

Returned the product "Wireless Mouse".

## 3. GET Product by ID

**Request**
```cmd
curl -i http://localhost:8080/api/products/1
```

**Result:** HTTP 200 OK

```json
{"id":1,"name":"Wireless Mouse","price":19.99,"stock":25}
```

## 4. GET Nonexistent Product

**Request**
```cmd
curl -i http://localhost:8080/api/products/999
```

**Result:** HTTP 404 Not Found

```json
{
  "detail": "Product 999 not found",
  "instance": "/api/products/999",
  "status": 404,
  "title": "Not Found"
}
```

## 5. POST Create Product

**Request**
```cmd
curl -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d "{\"name\":\"Gaming Mouse\",\"price\":35.99,\"stock\":20}"
```

**Result:** HTTP 201 Created

Location: /api/products/6

```json
{"id":6,"name":"Gaming Mouse","price":35.99,"stock":20}
```

## 6. PATCH Update Stock

**Request**
```cmd
curl -i -X PATCH http://localhost:8080/api/products/6/stock -H "Content-Type: application/json" -d "{\"delta\":-5}"
```

**Result:** HTTP 200 OK

```json
{"id":6,"name":"Gaming Mouse","price":35.99,"stock":15}
```

## 7. PATCH Insufficient Stock

**Request**
```cmd
curl -i -X PATCH http://localhost:8080/api/products/6/stock -H "Content-Type: application/json" -d "{\"delta\":-30}"
```

**Result:** HTTP 409 Conflict

```json
{
  "detail": "Insufficient stock for product 6",
  "instance": "/api/products/6/stock",
  "status": 409,
  "title": "Conflict"
}
```

A subsequent GET confirmed that stock remained at 15.

## 8. DELETE Product

**Request**
```cmd
curl -i -X DELETE http://localhost:8080/api/products/6
```

**Result:** HTTP 204 No Content

A subsequent GET returned HTTP 404, confirming deletion.

## 9. POST Invalid Product

**Request**
```cmd
curl -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d "{\"name\":\"\",\"price\":-5,\"stock\":-2}"
```

**Result:** HTTP 400 Bad Request

```json
{
  "detail": "Validation failed",
  "instance": "/api/products",
  "status": 400,
  "title": "Validation Failed",
  "errors": [
    "name: must not be blank",
    "price: must be greater than or equal to 0.01",
    "stock: must be greater than or equal to 0"
  ]
}
```

## Test Summary

All five REST endpoints were tested successfully, including validation and error cases.

The automated Maven test suite also passed with BUILD SUCCESS.
