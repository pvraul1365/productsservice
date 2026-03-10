package net.javaguides.productsservice.config;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import java.util.concurrent.ExecutionException;

/**
 * SnsLocalInitializer
 * <p>
 * Creates the SNS topic on startup when running locally (LocalStack).
 * Mirrors the CDK stack: topicName "product-events".
 *
 * @author architecture - pvraul
 * @version 10/03/2026 - 16:02
 * @since 1.17
 */
@Profile("local")
@Configuration
public class SnsLocalInitializer {

    private static final Logger log = LogManager.getLogger(SnsLocalInitializer.class);

    private final SnsAsyncClient snsAsyncClient;

    @Value("${aws.sns.endpoint:#{null}}")
    private String endpoint;

    @Value("${aws.sns.topic.name:product-events}")
    private String topicName;

    public SnsLocalInitializer(SnsAsyncClient snsAsyncClient) {
        this.snsAsyncClient = snsAsyncClient;
    }

    @PostConstruct
    public void init() {
        // Al igual que en Dynamo, si no hay endpoint local, salimos.
        if (endpoint == null || endpoint.isEmpty()) {
            log.info("Not local env — skipping SNS topic creation.");
            return;
        }

        try {
            // 1. Listar tópicos para ver si ya existe (buscamos por nombre al final del ARN)
            ListTopicsResponse listTopicsResponse = snsAsyncClient.listTopics().get();
            boolean topicExists = listTopicsResponse.topics().stream()
                    .map(Topic::topicArn)
                    .anyMatch(arn -> arn.endsWith(":" + topicName));

            if (topicExists) {
                log.info("SNS Topic '{}' already exists, skipping creation.", topicName);
                return;
            }

            // 2. Crear el tópico si no existe
            CreateTopicRequest createRequest = CreateTopicRequest.builder()
                    .name(topicName)
                    .build();

            snsAsyncClient.createTopic(createRequest).get();
            log.info("SNS Topic '{}' created successfully in LocalStack.", topicName);

        } catch (ExecutionException | InterruptedException e) {
            log.error("Failed to initialize SNS local topic '{}': {}", topicName, e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
