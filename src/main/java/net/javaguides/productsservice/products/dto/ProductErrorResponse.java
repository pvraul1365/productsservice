package net.javaguides.productsservice.products.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ProductErrorResponse(
       String message,
       int statusCode,
       @JsonInclude(JsonInclude.Include.NON_NULL) String requestId,
       @JsonInclude(JsonInclude.Include.NON_NULL) String productId
) {
}
