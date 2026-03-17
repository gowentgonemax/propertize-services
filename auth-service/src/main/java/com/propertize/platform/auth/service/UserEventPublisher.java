package com.propertize.platform.auth.service;

import com.propertize.platform.auth.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing user-related events to Kafka.
 * These events are consumed by propertize-service to maintain user profile
 * synchronization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.user-events:user-events}")
    private String userEventsTopic;

    /**
     * Publish user event to Kafka
     */
    public void publishUserEvent(UserEvent event) {
        // Set event metadata if not already set
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        log.info("Publishing user event: type={}, userId={}, eventId={}",
                event.getEventType(), event.getUserId(), event.getEventId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(userEventsTopic,
                event.getUserId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published user event: eventId={}, partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish user event: eventId={}, error={}",
                        event.getEventId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * Helper method to create and publish user created event
     */
    public void publishUserCreated(Long userId, String username, String email,
            Boolean enabled, String correlationId) {
        UserEvent event = UserEvent.builder()
                .eventType(UserEvent.EventType.USER_CREATED)
                .userId(userId)
                .username(username)
                .email(email)
                .enabled(enabled)
                .correlationId(correlationId)
                .build();
        publishUserEvent(event);
    }

    /**
     * Helper method to create and publish user updated event
     */
    public void publishUserUpdated(Long userId, String username, String email,
            Boolean enabled, String correlationId) {
        UserEvent event = UserEvent.builder()
                .eventType(UserEvent.EventType.USER_UPDATED)
                .userId(userId)
                .username(username)
                .email(email)
                .enabled(enabled)
                .correlationId(correlationId)
                .build();
        publishUserEvent(event);
    }

    /**
     * Helper method to create and publish user deleted event
     */
    public void publishUserDeleted(Long userId, String reason, String correlationId) {
        UserEvent event = UserEvent.builder()
                .eventType(UserEvent.EventType.USER_DELETED)
                .userId(userId)
                .reason(reason)
                .correlationId(correlationId)
                .build();
        publishUserEvent(event);
    }
}
