package net.javaguides.productsservice.events.services;

import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.javaguides.productsservice.events.dto.EventType;
import net.javaguides.productsservice.events.dto.ProductEventDto;
import net.javaguides.productsservice.products.model.Product;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.Topic;

/**
 * EventsPublisher
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 08/03/2026 - 10:51
 * @since 1.17
 */
@Service
public class EventsPublisher {

    private final SnsAsyncClient snsAsyncClient;
    private final Topic productEventsTopic;
    private final ObjectMapper objectMapper;

    public EventsPublisher(SnsAsyncClient snsAsyncClient,
                           @Qualifier("productEventsTopic") Topic productEventsTopic,
                           ObjectMapper objectMapper) {
        this.snsAsyncClient = snsAsyncClient;
        this.productEventsTopic = productEventsTopic;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<PublishResponse> sendProductEvent(final Product product, final EventType eventType, final String email)
            throws JsonProcessingException {

        ProductEventDto productEventDto = new ProductEventDto(
                product.getId(),
                product.getCode(),
                email,
                product.getPrice()
        );

        return this.sendEvent(objectMapper.writeValueAsString(productEventDto), eventType);
    }

    private CompletableFuture<PublishResponse> sendEvent(final String data, final EventType eventType) {
         return this.snsAsyncClient.publish(PublishRequest.builder()
                        .message(data)
                        .messageAttributes(Map.of(
                                "eventType", MessageAttributeValue.builder()
                                                .dataType("String")
                                                .stringValue(eventType.name())
                                        .build(),
                                "requestId", MessageAttributeValue.builder()
                                                .dataType("String")
                                                .stringValue(ThreadContext.get("requestId"))
                                        .build(),
                                "traceId", MessageAttributeValue.builder()
                                                .dataType("String")
                                                .stringValue(Objects.requireNonNull(
                                                        AWSXRay.getCurrentSegment().getTraceId().toString())
                                                )
                                        .build()
                        ))
                        .topicArn(this.productEventsTopic.topicArn())
                .build());
    }

}
