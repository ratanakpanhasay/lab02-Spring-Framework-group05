# Task 3 API session

Captured on 2026-11-11 (Asia/Bangkok) against the packaged application with the dev profile. Requests were executed with Invoke-WebRequest; response bodies below are captured verbatim, with no omissions.

Start the same application after mvn clean verify:

```powershell
java -jar target/shop-jpa-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=18083
```

## GET /api/orders

```powershell
Invoke-WebRequest -Uri "http://localhost:18083/api/orders"
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"content":[{"id":30,"status":"CANCELLED","placedAt":"2026-01-02T05:00:00Z","customer":{"name":"Sokha"}},{"id":29,"status":"DELIVERED","placedAt":"2026-01-02T04:00:00Z","customer":{"name":"Sokha"}},{"id":28,"status":"SHIPPED","placedAt":"2026-01-02T03:00:00Z","customer":{"name":"Sokha"}},{"id":27,"status":"CONFIRMED","placedAt":"2026-01-02T02:00:00Z","customer":{"name":"Sokha"}},{"id":26,"status":"PAID","placedAt":"2026-01-02T01:00:00Z","customer":{"name":"Sokha"}},{"id":25,"status":"PENDING","placedAt":"2026-01-02T00:00:00Z","customer":{"name":"Sokha"}},{"id":24,"status":"CANCELLED","placedAt":"2026-01-01T23:00:00Z","customer":{"name":"Sokha"}},{"id":23,"status":"DELIVERED","placedAt":"2026-01-01T22:00:00Z","customer":{"name":"Sokha"}},{"id":22,"status":"SHIPPED","placedAt":"2026-01-01T21:00:00Z","customer":{"name":"Sokha"}},{"id":21,"status":"CONFIRMED","placedAt":"2026-01-01T20:00:00Z","customer":{"name":"Sokha"}},{"id":20,"status":"PAID","placedAt":"2026-01-01T19:00:00Z","customer":{"name":"Sophea"}},{"id":19,"status":"PENDING","placedAt":"2026-01-01T18:00:00Z","customer":{"name":"Sophea"}},{"id":18,"status":"CANCELLED","placedAt":"2026-01-01T17:00:00Z","customer":{"name":"Sophea"}},{"id":17,"status":"DELIVERED","placedAt":"2026-01-01T16:00:00Z","customer":{"name":"Sophea"}},{"id":16,"status":"SHIPPED","placedAt":"2026-01-01T15:00:00Z","customer":{"name":"Sophea"}},{"id":15,"status":"CONFIRMED","placedAt":"2026-01-01T14:00:00Z","customer":{"name":"Sophea"}},{"id":14,"status":"PAID","placedAt":"2026-01-01T13:00:00Z","customer":{"name":"Sophea"}},{"id":13,"status":"PENDING","placedAt":"2026-01-01T12:00:00Z","customer":{"name":"Sophea"}},{"id":12,"status":"CANCELLED","placedAt":"2026-01-01T11:00:00Z","customer":{"name":"Sophea"}},{"id":11,"status":"DELIVERED","placedAt":"2026-01-01T10:00:00Z","customer":{"name":"Sophea"}}],"empty":false,"first":true,"last":false,"number":0,"numberOfElements":20,"pageable":{"offset":0,"pageNumber":0,"pageSize":20,"paged":true,"sort":{"empty":false,"sorted":true,"unsorted":false},"unpaged":false},"size":20,"sort":{"empty":false,"sorted":true,"unsorted":false},"totalElements":30,"totalPages":2}
```

## GET /api/orders?status=PAID&page=0&size=5&sort=placedAt,desc

```powershell
Invoke-WebRequest -Uri "http://localhost:18083/api/orders?status=PAID&page=0&size=5&sort=placedAt,desc"
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"content":[{"id":26,"status":"PAID","placedAt":"2026-01-02T01:00:00Z","customer":{"name":"Sokha"}},{"id":20,"status":"PAID","placedAt":"2026-01-01T19:00:00Z","customer":{"name":"Sophea"}},{"id":14,"status":"PAID","placedAt":"2026-01-01T13:00:00Z","customer":{"name":"Sophea"}},{"id":8,"status":"PAID","placedAt":"2026-01-01T07:00:00Z","customer":{"name":"Dara"}},{"id":2,"status":"PAID","placedAt":"2026-01-01T01:00:00Z","customer":{"name":"Dara"}}],"empty":false,"first":true,"last":true,"number":0,"numberOfElements":5,"pageable":{"offset":0,"pageNumber":0,"pageSize":5,"paged":true,"sort":{"empty":false,"sorted":true,"unsorted":false},"unpaged":false},"size":5,"sort":{"empty":false,"sorted":true,"unsorted":false},"totalElements":5,"totalPages":1}
```

## GET /api/orders?status=PAID&page=1&size=2&sort=placedAt,desc

```powershell
Invoke-WebRequest -Uri "http://localhost:18083/api/orders?status=PAID&page=1&size=2&sort=placedAt,desc"
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"content":[{"id":14,"status":"PAID","placedAt":"2026-01-01T13:00:00Z","customer":{"name":"Sophea"}},{"id":8,"status":"PAID","placedAt":"2026-01-01T07:00:00Z","customer":{"name":"Dara"}}],"empty":false,"first":false,"last":false,"number":1,"numberOfElements":2,"pageable":{"offset":2,"pageNumber":1,"pageSize":2,"paged":true,"sort":{"empty":false,"sorted":true,"unsorted":false},"unpaged":false},"size":2,"sort":{"empty":false,"sorted":true,"unsorted":false},"totalElements":5,"totalPages":3}
```

## GET /api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true

```powershell
Invoke-WebRequest -Uri "http://localhost:18083/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true"
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"content":[{"id":1,"sku":"DEV-001","name":"Mechanical Keyboard","amount":55.00,"stock":5},{"id":2,"sku":"DEV-002","name":"USB Keyboard","amount":25.00,"stock":6}],"empty":false,"first":true,"last":true,"number":0,"numberOfElements":2,"pageable":{"offset":0,"pageNumber":0,"pageSize":20,"paged":true,"sort":{"empty":false,"sorted":true,"unsorted":false},"unpaged":false},"size":20,"sort":{"empty":false,"sorted":true,"unsorted":false},"totalElements":2,"totalPages":1}
```

## GET /api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true&page=1&size=1

```powershell
Invoke-WebRequest -Uri "http://localhost:18083/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true&page=1&size=1"
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"content":[{"id":2,"sku":"DEV-002","name":"USB Keyboard","amount":25.00,"stock":6}],"empty":false,"first":false,"last":true,"number":1,"numberOfElements":1,"pageable":{"offset":1,"pageNumber":1,"pageSize":1,"paged":true,"sort":{"empty":false,"sorted":true,"unsorted":false},"unpaged":false},"size":1,"sort":{"empty":false,"sorted":true,"unsorted":false},"totalElements":2,"totalPages":2}
```

## GET /api/products?inStock=false

```powershell
Invoke-WebRequest -Uri "http://localhost:18083/api/products?inStock=false"
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"content":[{"id":3,"sku":"DEV-003","name":"Keycap Set","amount":15.00,"stock":0},{"id":7,"sku":"DEV-007","name":"T-shirt","amount":12.00,"stock":0},{"id":11,"sku":"DEV-011","name":"Java Book","amount":35.00,"stock":0}],"empty":false,"first":true,"last":true,"number":0,"numberOfElements":3,"pageable":{"offset":0,"pageNumber":0,"pageSize":20,"paged":true,"sort":{"empty":false,"sorted":true,"unsorted":false},"unpaged":false},"size":20,"sort":{"empty":false,"sorted":true,"unsorted":false},"totalElements":3,"totalPages":1}
```

## GET /api/customers/dara@example.com/orders

```powershell
Invoke-WebRequest -Uri "http://localhost:18083/api/customers/dara@example.com/orders"
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

[{"id":10,"status":"SHIPPED","placedAt":"2026-01-01T09:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":28,"productId":10,"sku":"DEV-010","productName":"Coffee","quantity":1,"unitPrice":{"amount":10.00,"currency":"USD"}},{"id":29,"productId":11,"sku":"DEV-011","productName":"Java Book","quantity":2,"unitPrice":{"amount":35.00,"currency":"USD"}}],"total":{"amount":80.00,"currency":"USD"}},{"id":9,"status":"CONFIRMED","placedAt":"2026-01-01T08:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":24,"productId":9,"sku":"DEV-009","productName":"Rice","quantity":1,"unitPrice":{"amount":8.00,"currency":"USD"}},{"id":25,"productId":10,"sku":"DEV-010","productName":"Coffee","quantity":2,"unitPrice":{"amount":10.00,"currency":"USD"}},{"id":26,"productId":11,"sku":"DEV-011","productName":"Java Book","quantity":1,"unitPrice":{"amount":35.00,"currency":"USD"}},{"id":27,"productId":12,"sku":"DEV-012","productName":"Notebook","quantity":2,"unitPrice":{"amount":5.00,"currency":"USD"}}],"total":{"amount":73.00,"currency":"USD"}},{"id":8,"status":"PAID","placedAt":"2026-01-01T07:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":21,"productId":8,"sku":"DEV-008","productName":"Jacket","quantity":1,"unitPrice":{"amount":45.00,"currency":"USD"}},{"id":22,"productId":9,"sku":"DEV-009","productName":"Rice","quantity":2,"unitPrice":{"amount":8.00,"currency":"USD"}},{"id":23,"productId":10,"sku":"DEV-010","productName":"Coffee","quantity":1,"unitPrice":{"amount":10.00,"currency":"USD"}}],"total":{"amount":71.00,"currency":"USD"}},{"id":7,"status":"PENDING","placedAt":"2026-01-01T06:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":19,"productId":7,"sku":"DEV-007","productName":"T-shirt","quantity":1,"unitPrice":{"amount":12.00,"currency":"USD"}},{"id":20,"productId":8,"sku":"DEV-008","productName":"Jacket","quantity":2,"unitPrice":{"amount":45.00,"currency":"USD"}}],"total":{"amount":102.00,"currency":"USD"}},{"id":6,"status":"CANCELLED","placedAt":"2026-01-01T05:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":15,"productId":6,"sku":"DEV-006","productName":"Headphones","quantity":1,"unitPrice":{"amount":65.00,"currency":"USD"}},{"id":16,"productId":7,"sku":"DEV-007","productName":"T-shirt","quantity":2,"unitPrice":{"amount":12.00,"currency":"USD"}},{"id":17,"productId":8,"sku":"DEV-008","productName":"Jacket","quantity":1,"unitPrice":{"amount":45.00,"currency":"USD"}},{"id":18,"productId":9,"sku":"DEV-009","productName":"Rice","quantity":2,"unitPrice":{"amount":8.00,"currency":"USD"}}],"total":{"amount":150.00,"currency":"USD"}},{"id":5,"status":"DELIVERED","placedAt":"2026-01-01T04:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":12,"productId":5,"sku":"DEV-005","productName":"Mouse","quantity":1,"unitPrice":{"amount":20.00,"currency":"USD"}},{"id":13,"productId":6,"sku":"DEV-006","productName":"Headphones","quantity":2,"unitPrice":{"amount":65.00,"currency":"USD"}},{"id":14,"productId":7,"sku":"DEV-007","productName":"T-shirt","quantity":1,"unitPrice":{"amount":12.00,"currency":"USD"}}],"total":{"amount":162.00,"currency":"USD"}},{"id":4,"status":"SHIPPED","placedAt":"2026-01-01T03:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":10,"productId":4,"sku":"DEV-004","productName":"Monitor","quantity":1,"unitPrice":{"amount":180.00,"currency":"USD"}},{"id":11,"productId":5,"sku":"DEV-005","productName":"Mouse","quantity":2,"unitPrice":{"amount":20.00,"currency":"USD"}}],"total":{"amount":220.00,"currency":"USD"}},{"id":3,"status":"CONFIRMED","placedAt":"2026-01-01T02:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":6,"productId":3,"sku":"DEV-003","productName":"Keycap Set","quantity":1,"unitPrice":{"amount":15.00,"currency":"USD"}},{"id":7,"productId":4,"sku":"DEV-004","productName":"Monitor","quantity":2,"unitPrice":{"amount":180.00,"currency":"USD"}},{"id":8,"productId":5,"sku":"DEV-005","productName":"Mouse","quantity":1,"unitPrice":{"amount":20.00,"currency":"USD"}},{"id":9,"productId":6,"sku":"DEV-006","productName":"Headphones","quantity":2,"unitPrice":{"amount":65.00,"currency":"USD"}}],"total":{"amount":525.00,"currency":"USD"}},{"id":2,"status":"PAID","placedAt":"2026-01-01T01:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":3,"productId":2,"sku":"DEV-002","productName":"USB Keyboard","quantity":1,"unitPrice":{"amount":25.00,"currency":"USD"}},{"id":4,"productId":3,"sku":"DEV-003","productName":"Keycap Set","quantity":2,"unitPrice":{"amount":15.00,"currency":"USD"}},{"id":5,"productId":4,"sku":"DEV-004","productName":"Monitor","quantity":1,"unitPrice":{"amount":180.00,"currency":"USD"}}],"total":{"amount":235.00,"currency":"USD"}},{"id":1,"status":"PENDING","placedAt":"2026-01-01T00:00:00Z","customerName":"Dara","customerEmail":"dara@example.com","lines":[{"id":1,"productId":1,"sku":"DEV-001","productName":"Mechanical Keyboard","quantity":1,"unitPrice":{"amount":55.00,"currency":"USD"}},{"id":2,"productId":2,"sku":"DEV-002","productName":"USB Keyboard","quantity":2,"unitPrice":{"amount":25.00,"currency":"USD"}}],"total":{"amount":105.00,"currency":"USD"}}]
```

## GET /api/customers/missing@example.com/orders

```powershell
Invoke-WebRequest -Uri "http://localhost:18083/api/customers/missing@example.com/orders"
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

[]
```
