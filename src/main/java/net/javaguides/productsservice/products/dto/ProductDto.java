package net.javaguides.productsservice.products.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Positive;
import net.javaguides.productsservice.products.model.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductDto(
        String id,

        @NotBlank(message = "Product name cannot be blank")
        @Size(min = 3, max = 50, message = "Product name must be between 5 and 50 characters")
        String name,

        @NotBlank(message = "Product code cannot be blank")
        @Size(min = 3, max = 15, message = "Product code must be between 5 and 50 characters")
        String code,

        @Positive(message = "Product price must be greater than zero")
        float price,

        String model,
        String url
) {

    public ProductDto(final Product product) {
        this(product.getId(),
                product.getProductName(),
                product.getCode(),
                product.getPrice(),
                product.getModel(),
                product.getProductUrl());
    }

    static public Product toProduct(final ProductDto productDto) {
        final Product product = new Product();
        product.setId(productDto.id());
        product.setProductName(productDto.name());
        product.setCode(productDto.code());
        product.setPrice(productDto.price());
        product.setModel(productDto.model());
        product.setProductUrl(productDto.url());
        
        return product;
    }

}
