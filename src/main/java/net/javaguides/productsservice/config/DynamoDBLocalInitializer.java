package net.javaguides.productsservice.config;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.concurrent.ExecutionException;

/**
 * DynamoDbLocalInitializer
 * <p>
 * Creates the DynamoDB table (+ GSI) on startup when running locally.
 * Mirrors the CDK stack: products table with id (PK) and codeIdx GSI on code.
 *
 * @author architecture - pvraul
 * @version 07/03/2026
 * @since 1.17
 */
@Profile("local")
@Configuration
public class DynamoDBLocalInitializer {

    private static final Logger log = LogManager.getLogger(DynamoDBLocalInitializer.class);

    private final DynamoDbAsyncClient dynamoDbAsyncClient;

    @Value("${aws.dynamodb.endpoint:#{null}}")
    private String endpoint;

    @Value("${aws.productsddb.name}")
    private String tableName;

    public DynamoDBLocalInitializer(DynamoDbAsyncClient dynamoDbAsyncClient) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
    }

    @PostConstruct
    public void init() {
        if (endpoint == null || endpoint.isEmpty()) {
            log.info("Not local env — skipping DynamoDB table creation.");
            return;
        }

        try {
            boolean tableExists = dynamoDbAsyncClient.listTables()
                    .thenApply(r -> r.tableNames().contains(tableName))
                    .get();

            if (tableExists) {
                log.info("Table '{}' already exists, skipping creation.", tableName);
                return;
            }

            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("code")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("id")
                                    .keyType(KeyType.HASH)
                                    .build()
                    )
                    .globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                    .indexName("codeIdx")
                                    .keySchema(
                                            KeySchemaElement.builder()
                                                    .attributeName("code")
                                                    .keyType(KeyType.HASH)
                                                    .build()
                                    )
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.KEYS_ONLY)
                                            .build())
                                    .provisionedThroughput(ProvisionedThroughput.builder()
                                            .readCapacityUnits(1L)
                                            .writeCapacityUnits(1L)
                                            .build())
                                    .build()
                    )
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(1L)
                            .writeCapacityUnits(1L)
                            .build())
                    .build();

            dynamoDbAsyncClient.createTable(request).get();
            log.info("Table '{}' created successfully with GSI 'codeIdx'.", tableName);

        } catch (ExecutionException | InterruptedException e) {
            log.error("Failed to initialize DynamoDB local table '{}': {}", tableName, e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}

