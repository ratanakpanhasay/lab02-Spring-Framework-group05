# CSRF Strategy

## Current authentication strategy

The `/api/**` endpoints use stateless JWT Bearer authentication. Clients explicitly send their JWT through the `Authorization` header. The API does not accept session cookies or HTTP Basic authentication.

CSRF protection is disabled for the Bearer-only API because browsers do not automatically attach Bearer tokens to cross-site requests. Public product review pages require no authentication credentials.

## Future session-based SPA

If a same-origin SPA using session cookies is introduced, CSRF protection must be enabled for its filter chain. Spring Security can use a cookie-based CSRF token repository:

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(
        CookieCsrfTokenRepository.withHttpOnlyFalse()
    )
);
```

The frontend reads the issued `XSRF-TOKEN` cookie and sends its value through the `X-XSRF-TOKEN` header when making state-changing requests.

```javascript
const token = decodeURIComponent(
    document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='))
        .substring('XSRF-TOKEN='.length)
);

fetch('/orders', {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': token
    },
    body: JSON.stringify(order)
});
```

The server validates the CSRF token before processing the request. The future SPA must also configure Spring Security's SPA token handling and obtain a fresh token when required. CSRF protection should remain enabled for cookie-authenticated endpoints.