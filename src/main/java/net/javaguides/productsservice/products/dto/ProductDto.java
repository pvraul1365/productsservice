package net.javaguides.productsservice.products.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.javaguides.productsservice.products.model.Product;

public record ProductDto(
        String id,
        String name,
        String code,
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
