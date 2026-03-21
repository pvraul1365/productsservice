package net.javaguides.productsservice.products.controllers;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import net.javaguides.productsservice.events.dto.EventType;
import net.javaguides.productsservice.events.services.IEventsPublisher;
import net.javaguides.productsservice.products.dto.ProductDto;
import net.javaguides.productsservice.products.enums.ProductErrors;
import net.javaguides.productsservice.products.exceptions.ProductException;
import net.javaguides.productsservice.products.model.Product;
import net.javaguides.productsservice.products.repositories.ProductsRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * ProductsController
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 31/01/2026 - 08:35
 * @since 1.17
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@XRayEnabled
public class ProductsController {

    private static final Logger LOG = LogManager.getLogger(ProductsController.class);

    private final ProductsRepository productsRepository;
    private final IEventsPublisher eventsPublisher;

    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(value = "code", required = false) final String code) throws ProductException {

        if (code != null) {
            LOG.info("ℹ️ - GET /api/products called with code filter: {}", code);
            Product productByCode = productsRepository.getByCode(code).join();

            if (productByCode == null) {
                throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, null);
            } else {
                return new ResponseEntity<>(new ProductDto(productByCode), HttpStatus.OK);
            }

        } else {
            LOG.info("ℹ️ - GET /api/products called");
            List<ProductDto> productsDto = new ArrayList<>();

            productsRepository.getAll().items().subscribe(product -> {
                productsDto.add(new ProductDto(product));
            }).join();

            return new ResponseEntity<>(productsDto, HttpStatus.OK);
        }
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("id") final String id) throws  ProductException {
        LOG.info("ℹ️ - GET /api/products/{} called", id);

        Product product = productsRepository.getById(id).join();
        if (product != null) {
            LOG.info("✅ - Product found: {}", product);
            return new ResponseEntity<>(new ProductDto(product), HttpStatus.OK);
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody final ProductDto productDto)
            throws ProductException, JsonProcessingException, ExecutionException, InterruptedException {
        LOG.info("ℹ️ - POST /api/products called with payload: {}", productDto);

        Product productCreated = ProductDto.toProduct(productDto);
        productCreated.setId(UUID.randomUUID().toString());
        CompletableFuture<Product> productCompletableFuture = productsRepository.create(productCreated);

        CompletableFuture<PublishResponse> publishResponseCompletableFuture =
                eventsPublisher.sendProductEvent(productCreated, EventType.PRODUCT_CREATED,
                                "raul.perez.vicente@gmail.com");

        CompletableFuture.allOf(productCompletableFuture, publishResponseCompletableFuture).join();
        PublishResponse publishResponse = publishResponseCompletableFuture.get();
        ThreadContext.put("messageId", publishResponse.messageId());

        LOG.info("✅ - Product created with ID: {}", productCreated.getId());
        return new ResponseEntity<>(new ProductDto(productCreated), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ProductDto> deleteProduct(@PathVariable("id") final String id)
            throws ProductException, JsonProcessingException {
        LOG.info("ℹ️ - DELETE /api/products/{} called", id);
        Product productDeleted = productsRepository.deleteById(id).join();
        if (productDeleted != null) {
            PublishResponse publishResponse = eventsPublisher.sendProductEvent(productDeleted, EventType.PRODUCT_DELETED, "raul.perez.vicente@gmail.com")
                    .join();
            ThreadContext.put("messageId", publishResponse.messageId());

            LOG.info("✅ - Product with id {} deleted successfully", id);
            return new ResponseEntity<>(new ProductDto(productDeleted), HttpStatus.OK);
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable("id") final String id,
                                                    @Valid @RequestBody final ProductDto productDto)
            throws ProductException, JsonProcessingException {
        LOG.info("ℹ️ - PUT /api/products/{} called with payload: {}", id, productDto);
        try {
            Product updatedProduct = productsRepository.update(ProductDto.toProduct(productDto), id).join();

            PublishResponse publishResponse = eventsPublisher.sendProductEvent(updatedProduct, EventType.PRODUCT_UPDATED,
                            "raul.perez.vicente@gmail.com")
                    .join();
            ThreadContext.put("messageId", publishResponse.messageId());

            LOG.info("✅ - Product with id {} updated successfully", id);
            return new ResponseEntity<>(new ProductDto(updatedProduct), HttpStatus.OK);
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }
}
