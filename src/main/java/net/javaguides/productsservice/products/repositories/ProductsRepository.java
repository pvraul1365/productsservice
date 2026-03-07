package net.javaguides.productsservice.products.repositories;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.javaguides.productsservice.products.controllers.ProductsController;
import net.javaguides.productsservice.products.enums.ProductErrors;
import net.javaguides.productsservice.products.exceptions.ProductException;
import net.javaguides.productsservice.products.model.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

/**
 * Repository component responsible for asynchronous persistence operations on {@link Product} entities
 * stored in Amazon DynamoDB using the AWS SDK Enhanced Async Client.
 *
 * <p>This repository encapsulates access to the configured products table and exposes CRUD-style methods:
 * retrieval of all products, lookup by identifier, creation, deletion, and conditional update.</p>
 *
 * <h2>Behavior summary</h2>
 * <ul>
 *   <li><b>getAll()</b>: performs a full table scan and returns a {@code PagePublisher<Product>}.</li>
 *   <li><b>getById(String)</b>: retrieves a single product by partition key.</li>
 *   <li><b>create(Product)</b>: inserts a product and completes with the same product instance.</li>
 *   <li><b>deleteById(String)</b>: deletes by identifier and completes with the deleted item when available.</li>
 *   <li><b>update(Product, String)</b>: updates an existing product after enforcing key consistency and
 *       uses a conditional expression ({@code attribute_exists(id)}) to prevent accidental upserts.</li>
 * </ul>
 *
 * <h2>Asynchronous model</h2>
 * <p>All single-item operations return {@link java.util.concurrent.CompletableFuture} and are non-blocking.
 * Callers should compose or handle completion and error signals explicitly.</p>
 *
 * <h2>Operational notes</h2>
 * <p>The scan-based retrieval method is suitable for demos or small datasets but is typically not recommended
 * for production workloads due to cost and latency characteristics. Prefer targeted query patterns and paging
 * strategies for scalable access.</p>
 *
 * <h2>Configuration</h2>
 * <p>The repository binds to a DynamoDB table name supplied via application configuration
 * (for example: {@code aws.productsddb.name}) and requires a preconfigured
 * {@link software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient}.</p>
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 16/02/2026 - 16:57
 * @since 1.17
 */
@Repository
@XRayEnabled
public class ProductsRepository {
    private static final Logger LOG = LogManager.getLogger(ProductsRepository.class);

    /** Condition to prevent accidental upserts — fails if the item does not already exist. */
    private static final Expression EXISTS_CONDITION =
            Expression.builder().expression("attribute_exists(id)").build();

    private final DynamoDbAsyncTable<Product> productsTable;

    /**
     * Constructs a new {@code ProductsRepository} with the specified DynamoDB client and table name.
     *
     * @param dynamoDbEnhancedAsyncClient the asynchronous DynamoDB client used for data operations; must not be {@code null}
     * @param productsDdbName the name of the DynamoDB table to operate on; must not be {@code null} or empty
     */
    public ProductsRepository(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                              @Value("${aws.productsddb.name}") String productsDdbName) {
        this.productsTable = dynamoDbEnhancedAsyncClient.table(productsDdbName, TableSchema.fromBean(Product.class));
    }

    private CompletableFuture<Product> checkIfCodeExists(final String code) {
        List<Product> products = new ArrayList<>();
        this.productsTable.index("codeIdx").query(QueryEnhancedRequest.builder()
                        .limit(1)
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                        .partitionValue(code)
                                .build()))
                .build())
                .subscribe(productPage -> {
                    products.addAll(productPage.items());
                }
        ).join();
        if (products.isEmpty()) {
            return CompletableFuture.supplyAsync(() -> null);
        } else {
            return CompletableFuture.supplyAsync(() -> products.get(0));
        }
    }

    public CompletableFuture<Product> getByCode(final String code) {
        LOG.info("🔍 - Retrieving product with code: {}", code);
        Product productByCode = checkIfCodeExists(code).join();
        if (productByCode != null) {
            LOG.info("✅ - Product found with code {}: {}", code, productByCode);
            return this.getById(productByCode.getId());
        } else {
            LOG.info("⚠️ - No product found with code: {}", code);
            return CompletableFuture.supplyAsync(() -> null);
        }
    }

    /**
     * Retrieves all products from the DynamoDB table using a scan operation.
     *
     * @return a {@link PagePublisher<Product>} that emits pages of products as they are retrieved from the table
     */
    public PagePublisher<Product> getAll() {
        // DO NOT DO THIS IN PRODUCTION, THIS IS JUST FOR DEMO PURPOSES, BECAUSE SCAN CAN BE VERY EXPENSIVE AND SLOW, ESPECIALLY IF YOU HAVE A LARGE TABLE, IN PRODUCTION YOU SHOULD USE QUERY INSTEAD OF SCAN, AND YOU SHOULD ALSO USE PAGINATION TO LIMIT THE NUMBER OF ITEMS RETURNED IN EACH REQUEST
        return productsTable.scan();
    }

    /**
     * Retrieves a single product by its unique identifier (partition key).
     *
     * @param productId the unique identifier of the product to retrieve; must not be {@code null}
     * @return a {@link CompletableFuture} that completes with the retrieved {@link Product} if found, or completes with {@code null} if no matching item exists
     */
    public CompletableFuture<Product> getById(final String productId) {
        LOG.info("🔍 - Retrieving product with ID: {}", productId);
        return productsTable.getItem(this.keyOf(productId));
    }

    /**
     * Creates a new {@link Product} in the underlying data store.
     *
     * @param product the product data to persist; must not be {@code null}
     * @return a {@link CompletableFuture} that completes with the created {@link Product} when the operation succeeds
     * @throws NullPointerException if {@code product} is {@code null}
     */
    public CompletableFuture<Product> create(Product product) throws ProductException {
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null) {
            LOG.warn("⚠️ - Attempt to create product with duplicate code '{}': existing product found: {}", product.getCode(), productWithSameCode);
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, productWithSameCode.getId());
        }

        return productsTable.putItem(product)
                .thenApply(ignored -> product);
    }

    /**
     * Deletes a {@link Product} from the underlying data store by its identifier.
     *
     * <p>This operation is asynchronous and returns a {@link CompletableFuture}
     * that completes with the deleted {@link Product} representation (if provided
     * by the data client) once the delete request has finished.</p>
     *
     * @param productId the unique identifier of the product to delete; must not be {@code null}
     * @return a {@link CompletableFuture} that completes when the delete operation finishes
     */
    public CompletableFuture<Product> deleteById(final String productId) {
        return productsTable.deleteItem(this.keyOf(productId));
    }

    /**
     * Updates an existing {@code Product} in the data store using the provided product ID.
     *
     * @param product the product data to persist; its ID is overwritten with {@code productId}
     * @param productId the identifier of the product to update
     * @return a {@link java.util.concurrent.CompletableFuture} that completes with the updated
     *         {@code Product} when the operation succeeds, or completes exceptionally if the
     *         update fails (for example, when the item does not exist)
     */
    public CompletableFuture<Product> update(Product product, final String productId) throws ProductException {
        product.setId(productId);

        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null && !productWithSameCode.getId().equals(product.getId())) {
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, productWithSameCode.getId());
        }

        return productsTable.updateItem(
                UpdateItemEnhancedRequest.<Product>builder(Product.class)
                        .item(product)
                        .conditionExpression(EXISTS_CONDITION)
                        .build());
    }

    /**
     * Helper method to construct a DynamoDB key for a given product ID.
     * @param productId the product identifier to use as the partition key value
     * @return a {@link Key} instance representing the partition key for the specified product ID
     * @throws NullPointerException if {@code productId} is {@code null}
     */
    private Key keyOf(final String productId) {
        return Key.builder().partitionValue(productId).build();
    }
}
