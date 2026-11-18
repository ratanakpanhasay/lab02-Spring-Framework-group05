
# Shop REST

Product Catalogue API built with Jakarta REST 4.0,
Eclipse Jersey 4.0, and Grizzly HTTP Server.

## Requirements
- JDK 25
- Maven 3.9.16
- curl

## Project
- Group ID: edu.itc
- Artifact ID: shop-rest
- Version: 1.0-SNAPSHOT

## Task 1 — Stateless Resource

ProductResource does not store mutable request-specific fields because
each HTTP request should be handled independently, preventing shared-state
problems when multiple clients access the API concurrently.