package net.javaguides.productsservice.products.controllers;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import net.javaguides.productsservice.products.dto.ProductDto;
import net.javaguides.productsservice.products.enums.ProductErrors;
import net.javaguides.productsservice.products.exceptions.ProductException;
import net.javaguides.productsservice.products.model.Product;
import net.javaguides.productsservice.products.repositories.ProductsRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts() {
        LOG.info("ℹ️ - GET /api/products called");

        List<ProductDto> productsDto = new ArrayList<>();

        productsRepository.getAll().items().subscribe(product -> {
            productsDto.add(new ProductDto(product));
        }).join();

        return new ResponseEntity<>(productsDto, HttpStatus.OK);
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
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody final ProductDto productDto) {
        LOG.info("ℹ️ - POST /api/products called with payload: {}", productDto);

        Product product = ProductDto.toProduct(productDto);
        product.setId(UUID.randomUUID().toString());
        Product createdProduct = productsRepository.create(product).join();

        LOG.info("✅ - Product created with ID: {}", createdProduct.getId());
        return new ResponseEntity<>(new ProductDto(createdProduct), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ProductDto> deleteProduct(@PathVariable("id") final String id) throws ProductException {
        LOG.info("ℹ️ - DELETE /api/products/{} called", id);

        Product productDeleted = productsRepository.deleteById(id).join();
        if (productDeleted != null) {
            LOG.info("✅ - Product with id {} deleted successfully", id);
            return new ResponseEntity<>(new ProductDto(productDeleted), HttpStatus.OK);
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable("id") final String id,
                                                    @Valid @RequestBody final ProductDto productDto) throws ProductException {
        LOG.info("ℹ️ - PUT /api/products/{} called with payload: {}", id, productDto);

        try {
            Product updatedProduct = productsRepository.update(ProductDto.toProduct(productDto), id).join();

            LOG.info("✅ - Product with id {} updated successfully", id);
            return new ResponseEntity<>(new ProductDto(updatedProduct), HttpStatus.OK);
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }
}
