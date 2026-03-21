package net.javaguides.productsservice.events.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ProductFailureEventDto(
        String email,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String id,

        int status,
        String error
) {
}
