# Copilot Instructions

> Responses should sound like a bro, and be concise.

## Architecture Overview

Spring Boot 3 / Java 21 microservice that exposes a REST CRUD API for products backed by **Amazon DynamoDB** (async) with **AWS X-Ray** distributed tracing.

```
config/          → DynamoDBConfig, XRayConfig, XRayInspector (AOP)
products/
  controllers/   → ProductsController  (@RestController, @XRayEnabled)
  dto/           → ProductDto          (Java record)
  model/         → Product             (DynamoDB entity, manual getters/setters)
  repositories/  → ProductsRepository  (@Repository, @XRayEnabled, async)
```

## Key Patterns

### DynamoDB Entity (`Product`)
- Annotate class with `@DynamoDbBean` — **no** `@Data` Lombok here; use manual getters/setters.
- `@DynamoDbPartitionKey` goes on the **getter**, not the field.
- Table and schema bound via `TableSchema.fromBean(Product.class)` in the repository constructor.

### Repository (`ProductsRepository`)
- All operations return `CompletableFuture<T>` or `PagePublisher<T>` — fully async.
- Partition key lookups are delegated to a private `keyOf(String id)` helper to avoid duplication.
- Conditional updates use a `private static final Expression` constant (`attribute_exists(id)`).
- `getAll()` uses a full table scan — acceptable for demos, **not** for production.
- `.join()` to block is done at the **controller** layer, not inside the repository.

### DTO (`ProductDto`)
- Defined as a **Java record** with a `ProductDto(Product)` constructor for mapping from entity.
- Static `toProduct(ProductDto)` factory method maps back to the entity.
- Field name mapping: `productName` ↔ `name`, `productUrl` ↔ `url`.

### X-Ray Tracing
- Add `@XRayEnabled` to any `@RestController` or `@Repository` class to get automatic tracing.
- `DynamoDbAsyncClient` includes `TracingInterceptor` via `ClientOverrideConfiguration`.
- Sampling rules are loaded from `src/main/resources/xray/xray-sampling-rules.json`.

### Logging
- Uses **Log4j2** — `spring-boot-starter-logging` (Logback) is globally excluded in `build.gradle`.
- Always use `LogManager.getLogger(ClassName.class)` — never SLF4J's `LoggerFactory` in the `products/` layer.

## Configuration (`application.properties`)
```properties
aws.region=us-east-1
aws.productsddb.name=products   # injected into ProductsRepository via @Value
```

## Build & Run
```bash
# Run tests
./gradlew test

# Build fat JAR
./gradlew bootJar

# Docker (multi-stage, eclipse-temurin:21-jdk-alpine)
docker build -t productsservice .
docker run -p 8080:8080 productsservice
```

## REST Endpoints
| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/products` | Full scan — returns list |
| GET | `/api/products/{id}` | 404 with message string if not found |
| POST | `/api/products` | UUID assigned server-side |
| PUT | `/api/products/{id}` | Fails if item doesn't exist (conditional expression) |
| DELETE | `/api/products/{id}` | Returns deleted item or 404 |
