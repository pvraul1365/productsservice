package net.javaguides.productsservice.events.services;

import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.javaguides.productsservice.events.dto.EventType;
import net.javaguides.productsservice.events.dto.ProductEventDto;
import net.javaguides.productsservice.events.dto.ProductFailureEventDto;
import net.javaguides.productsservice.products.model.Product;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * EventsPublisher
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 08/03/2026 - 10:51
 * @since 1.17
 */
@Profile("local")
@Service
public class LocalEventsPublisher implements IEventsPublisher {

    private final SnsAsyncClient snsAsyncClient;
    private String topicArn; // Almacenamos el ARN completo aquí
    private final ObjectMapper objectMapper;

    // Usamos @Value directamente en el parámetro del constructor
    public LocalEventsPublisher(SnsAsyncClient snsAsyncClient,
                                ObjectMapper objectMapper,
                                @Value("${aws.sns.topic.name:product-events}") String topicName) {
        this.snsAsyncClient = snsAsyncClient;
        this.objectMapper = objectMapper;

        // Recuperamos el ARN dinámicamente al arrancar
        try {
            // .join() es necesario porque es un cliente Async y estamos en el constructor
            this.topicArn = snsAsyncClient.createTopic(t -> t.name(topicName))
                    .thenApply(r -> r.topicArn())
                    .join();

            System.out.println(">>> [EventsPublisher] Conectado al tópico: " + this.topicArn);
        } catch (Exception e) {
            // Si falla (por ejemplo, LocalStack no responde), usamos el formato estándar
            this.topicArn = "arn:aws:sns:us-east-1:000000000000:" + topicName;
            System.err.println(">>> [EventsPublisher] Error recuperando ARN, usando fallback: " + this.topicArn);
        }
    }

    @Override
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

    @Override
    public CompletableFuture<PublishResponse> sendProductFailureEvent(final ProductFailureEventDto productFailureEventDto) throws JsonProcessingException {
        return this.sendEvent(objectMapper.writeValueAsString(productFailureEventDto), EventType.PRODUCT_FAILURE);
    }

    private CompletableFuture<PublishResponse> sendEvent(final String data, final EventType eventType) {
        // 1. Extraemos el valor y nos aseguramos de que NUNCA sea nulo o vacío
        String rawRequestId = ThreadContext.get("requestId");
        String safeRequestId = (rawRequestId != null && !rawRequestId.isBlank())
                ? rawRequestId
                : "local-dev-" + UUID.randomUUID();

        // 2. Hacemos lo mismo para el traceId por si X-Ray no está activo
        String safeTraceId = "no-trace";
        try {
            if (AWSXRay.getCurrentSegment() != null) {
                safeTraceId = AWSXRay.getCurrentSegment().getTraceId().toString();
            }
        } catch (Exception e) {
            // En local esto fallará a menudo, así que mantenemos el "no-trace"
        }

        return this.snsAsyncClient.publish(PublishRequest.builder()
                .message(data)
                .messageAttributes(Map.of(
                        "eventType", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(eventType.name())
                                .build(),
                        "requestId", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(safeRequestId) // <--- USAR EL VALOR SEGURO
                                .build(),
                        "traceId", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(safeTraceId)   // <--- USAR EL VALOR SEGURO
                                .build()
                ))
                .topicArn(this.topicArn)
                .build());
    }

}
