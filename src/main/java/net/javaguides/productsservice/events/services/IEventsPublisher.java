package net.javaguides.productsservice.events.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.concurrent.CompletableFuture;
import net.javaguides.productsservice.events.dto.EventType;
import net.javaguides.productsservice.events.dto.ProductFailureEventDto;
import net.javaguides.productsservice.products.model.Product;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public interface IEventsPublisher {

    CompletableFuture<PublishResponse> sendProductEvent(Product product, EventType eventType, String email)
            throws JsonProcessingException;


    CompletableFuture<PublishResponse> sendProductFailureEvent(ProductFailureEventDto productFailureEventDto)
            throws JsonProcessingException;
}
