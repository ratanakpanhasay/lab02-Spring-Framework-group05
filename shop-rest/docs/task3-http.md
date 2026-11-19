
# Lab-04 Task 3 — HTTP Test Evidence

## 1. Filtered and sorted product list

**Request:**
```bash
curl -i "http://localhost:8080/api/products?q=tea&sort=name,asc"
```

**Expected:** 200 OK. Returns Tea only, sorted by name ascending.

**Actual:** 200 OK. Returned Tea.

## 2. Invalid Money parameter

**Request:**
```bash
curl -i "http://localhost:8080/api/products?minPrice=abc"
```

**Expected:** 400 Bad Request with application/problem+json.

**Actual:** 400 Bad Request. Invalid money format was reported.

## 3. Unknown sort field

**Request:**
```bash
curl -i "http://localhost:8080/api/products?sort=colour,asc"
```

**Expected:** 400 Bad Request with application/problem+json.

**Actual:** 400 Bad Request. Unknown sort field: colour.

## 4. Invalid product creation

**Request:**
```bash
curl -i -X POST http://localhost:8080/api/products \
-H "Content-Type: application/json" \
-d '{"name":"","sku":"invalid","price":-5}'
```

**Expected:** 400 Bad Request with three validation errors.

**Actual:** 400 Bad Request. Errors returned for name, sku, and price.

## 5. Product not found

**Request:**
```bash
curl -i http://localhost:8080/api/products/999
```

**Expected:** 404 Not Found with application/problem+json.

**Actual:** 404 Not Found. Product 999 not found.

## 6. Internal server error

**Test:** Temporarily throw IllegalStateException from the product lookup endpoint.

**Request:**
```bash
curl -i http://localhost:8080/api/products/1
```

**Expected:** 500 Internal Server Error with application/problem+json and a reference ID. No stack trace in the response.

**Actual:** 500 Internal Server Error. Reference ID returned; no stack trace exposed.

The temporary exception was removed after testing.
