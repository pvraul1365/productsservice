# Products Service

A Spring Boot 3 / Java 21 microservice exposing a REST CRUD API for products, backed by **Amazon DynamoDB** (async client) with **AWS X-Ray** distributed tracing.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | Amazon DynamoDB (AWS SDK v2 Enhanced Client) |
| Tracing | AWS X-Ray SDK |
| Logging | Log4j2 |
| Build | Gradle |
| Containerization | Docker (eclipse-temurin:21-jdk-alpine) |

## Project Structure

```
src/main/java/net/javaguides/productsservice/
├── ProductsserviceApplication.java
├── config/
│   ├── DynamoDBConfig.java      # DynamoDbAsyncClient + TracingInterceptor bean
│   ├── XRayConfig.java          # X-Ray recorder setup
│   └── XRayInspector.java       # AOP aspect for @XRayEnabled
└── products/
    ├── controllers/
    │   └── ProductsController.java  # REST endpoints
    ├── dto/
    │   └── ProductDto.java          # Java record DTO
    ├── model/
    │   └── Product.java             # @DynamoDbBean entity
    └── repositories/
        └── ProductsRepository.java  # Async DynamoDB operations
```

## REST API

| Method | Path | Description | Status Codes |
|--------|------|-------------|-------------|
| `GET` | `/api/products` | List all products | 200 |
| `GET` | `/api/products/{id}` | Get product by ID | 200, 404 |
| `POST` | `/api/products` | Create a new product | 201 |
| `PUT` | `/api/products/{id}` | Update a product | 200, 404 |
| `DELETE` | `/api/products/{id}` | Delete a product | 200, 404 |

### Sample `ProductDto` Payload

```json
{
  "productName": "MacBook Pro",
  "productUrl": "https://apple.com/macbook-pro",
  "price": 1999.99
}
```

> Note: `id` is assigned server-side on creation (UUID).

## Configuration

`src/main/resources/application.properties`:

```properties
server.port=8080
aws.region=us-east-1
aws.productsddb.name=products
```

AWS credentials are resolved via the default credential provider chain (environment variables, `~/.aws/credentials`, IAM role, etc.).

## Prerequisites

- Java 21+
- Gradle 8+
- AWS account with a DynamoDB table named `products` (partition key: `id`, type `String`)
- AWS credentials configured locally or via environment

## Build & Run

```bash
# Run tests
./gradlew test

# Run locally
./gradlew bootRun

# Build fat JAR
./gradlew bootJar
java -jar build/libs/productsservice-0.0.1-SNAPSHOT.jar
```

## Docker

```bash
# Build image
docker build -t productsservice .

# Run container (pass AWS credentials as env vars)
docker run -p 8080:8080 \
  -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=<your-key> \
  -e AWS_SECRET_ACCESS_KEY=<your-secret> \
  productsservice
```

## AWS X-Ray Tracing

- Annotate classes with `@XRayEnabled` to enable automatic segment creation.
- The `DynamoDbAsyncClient` includes `TracingInterceptor` for DynamoDB trace propagation.
- Sampling rules are defined in `src/main/resources/xray/xray-sampling-rules.json`.
- The X-Ray daemon must be running locally or as a sidecar on port `2000` (UDP).

## DynamoDB Table Setup (AWS CLI)

```bash
aws dynamodb create-table \
  --table-name products \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

## License

Demo project — for educational purposes.
