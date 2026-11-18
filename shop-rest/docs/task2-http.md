# Task 2 – RESTful Web Services: HTTP Testing

## 1. Overview

This document describes the testing of the RESTful Web Services developed in Task 2.

The API manages products using CRUD operations (Create, Read, Update, and Delete). It supports JSON and XML formats and returns appropriate HTTP status codes.

**Base URL:**

```text
http://localhost:8080/api
```

**Products endpoint:**

```text
/api/products
```

## 2. REST API Endpoints

| HTTP Method | Endpoint | Description |
|---|---|---|
| GET | `/products` | Retrieve all products |
| GET | `/products/{id}` | Retrieve a product by ID |
| POST | `/products` | Create a new product |
| PUT | `/products/{id}` | Replace an existing product |
| PATCH | `/products/{id}/stock` | Change product stock |
| DELETE | `/products/{id}` | Delete a product |

## 3. HTTP Status Codes

| Status Code | Meaning | Usage |
|---|---|---|
| 200 OK | Successful request | Retrieve or update a product |
| 201 Created | Resource created | Create a new product |
| 204 No Content | Successful deletion | Delete a product |
| 404 Not Found | Resource not found | Product ID does not exist |
| 409 Conflict | Request conflicts with business rules | Duplicate SKU or insufficient stock |
| 415 Unsupported Media Type | Unsupported content type | Sending an unsupported format |

## 4. Testing the API

Start the application:

```bash
mvn -q compile exec:java
```

Keep the server terminal running. Open another terminal to execute the following tests.

### Test 1: Get All Products

```bash
curl -i http://localhost:8080/api/products
```

Expected result:

- Status: `200 OK`
- Returns a list of products.

### Test 2: Get a Product by ID

```bash
curl -i http://localhost:8080/api/products/1
```

Expected result:

- Status: `200 OK`
- Returns the product with ID 1.

### Test 3: Get a Product That Does Not Exist

```bash
curl -i http://localhost:8080/api/products/99
```

Expected result:

- Status: `404 Not Found`

### Test 4: Create a Product Using JSON

```bash
curl -i -X POST http://localhost:8080/api/products \
-H "Content-Type: application/json" \
-d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
```

Expected result:

- Status: `201 Created`
- Returns the created product.
- Includes a `Location` response header containing the new product URL.

### Test 5: Create a Product with a Duplicate SKU

Send the same POST request again using SKU `TP-01`.

Expected result:

- Status: `409 Conflict`
- Duplicate SKU is rejected.

### Test 6: Update a Product Using PUT

Replace `4` with the ID of the product you created in Test 4 if necessary.

```bash
curl -i -X PUT http://localhost:8080/api/products/4 \
-H "Content-Type: application/json" \
-d '{"name":"Large Teapot","sku":"TP-01","price":35.00,"stock":8}'
```

Expected result:

- Status: `200 OK`
- Product information is updated.

### Test 7: Change Stock Using PATCH

```bash
curl -i -X PATCH http://localhost:8080/api/products/3/stock \
-H "Content-Type: application/json" \
-d '{"delta":10}'
```

Expected result:

- Status: `200 OK`
- Stock increases by 10.

### Test 8: Try to Remove More Stock Than Available

```bash
curl -i -X PATCH http://localhost:8080/api/products/3/stock \
-H "Content-Type: application/json" \
-d '{"delta":-100}'
```

Expected result:

- Status: `409 Conflict`
- Stock cannot become negative.

### Test 9: Delete a Product

Replace `4` with the ID of an existing product.

```bash
curl -i -X DELETE http://localhost:8080/api/products/4
```

Expected result:

- Status: `204 No Content`
- Product is deleted successfully.

### Test 10: Delete a Product That Does Not Exist

```bash
curl -i -X DELETE http://localhost:8080/api/products/99
```

Expected result:

- Status: `404 Not Found`

## 5. JSON and XML Testing

### Create a Product Using XML

```bash
curl -i -X POST http://localhost:8080/api/products \
-H "Content-Type: application/xml" \
-d '<product><name>Plate</name><sku>PL-01</sku><price>8.50</price><stock>20</stock></product>'
```

Expected result:

- Status: `201 Created`
- Product is accepted in XML format.

### Retrieve Products Using XML

```bash
curl -i http://localhost:8080/api/products \
-H "Accept: application/xml"
```

Expected result:

- Status: `200 OK`
- Returns products in XML format.

### Test Unsupported Media Type

```bash
curl -i -X POST http://localhost:8080/api/products \
-H "Content-Type: text/plain" \
-d "Teapot"
```

Expected result:

- Status: `415 Unsupported Media Type`

## 6. Summary

Task 2 implements the main REST API operations:

- GET retrieves product information.
- POST creates new products.
- PUT replaces existing product information.
- PATCH changes product stock.
- DELETE removes products.
- JSON and XML are supported for product data.
- HTTP status codes communicate the result of each request.
- Business rules prevent duplicate SKUs and negative stock quantities.

## 7. Test Results

Record your actual test results after running the application.

| Test | Expected Status | Actual Status |
|---|---|---|
| Get all products | 200 OK | |
| Get product by ID | 200 OK | |
| Get missing product | 404 Not Found | |
| Create product | 201 Created | |
| Duplicate SKU | 409 Conflict | |
| Update product | 200 OK | |
| Change stock | 200 OK | |
| Insufficient stock | 409 Conflict | |
| Delete product | 204 No Content | |
| Delete missing product | 404 Not Found | |
| Create product using XML | 201 Created | |
| Retrieve products using XML | 200 OK | |
| Unsupported media type | 415 Unsupported Media Type | |

**Note:** The expected results above are the results to verify, not recorded test evidence. Fill in the actual status codes after running each test.