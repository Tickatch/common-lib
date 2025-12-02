package io.github.tickatch.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.tickatch.common.util.JsonUtils;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 서비스 간 통합 이벤트.
 *
 * <p>RabbitMQ를 통해 서비스 간 전달되는 이벤트 래퍼.
 * DomainEvent를 감싸서 추가 메타데이터(트레이싱, 소스 서비스 등)를 포함.
 *
 * <p>사용 예시:
 * <pre>{@code
 * TicketCreatedEvent domainEvent = new TicketCreatedEvent(ticketId, eventName, quantity);
 * IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, "ticket-service", traceId);
 *
 * rabbitTemplate.convertAndSend(exchange, integrationEvent.getRoutingKey(), integrationEvent);
 *
 * // 수신 측에서 payload 복원
 * TicketCreatedEvent restored = integrationEvent.getPayloadAs(TicketCreatedEvent.class);
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntegrationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 통합 이벤트 고유 ID */
    private final String eventId;

    /** 이벤트 타입 */
    private final String eventType;

    /** 이벤트 발생 시간 */
    private final Instant occurredAt;

    /** 이벤트 발행 서비스 */
    private final String sourceService;

    /** 분산 추적 ID */
    private final String traceId;

    /** Span ID */
    private final String spanId;

    /** 이벤트 버전 */
    private final int version;

    /** 이벤트 페이로드 (JSON 문자열) */
    private final String payload;

    /** 추가 메타데이터 */
    private final Map<String, String> metadata;

    /** Aggregate ID */
    private final String aggregateId;

    /** Aggregate 타입 */
    private final String aggregateType;

    /** RabbitMQ 라우팅 키 */
    private final String routingKey;

    /** 대상 Exchange 이름 */
    private final String exchange;

    /** 재시도 횟수 */
    @Builder.Default
    private final int retryCount = 0;

    /** 최대 재시도 횟수 */
    @Builder.Default
    private final int maxRetries = 3;

    /** 이벤트 만료 시간 */
    private final Instant expiresAt;

    // ========================================
    // Payload 역직렬화
    // ========================================

    /**
     * payload를 지정된 클래스로 역직렬화
     *
     * @param clazz 변환할 클래스 타입
     * @param <T> 반환 타입
     * @return 역직렬화된 객체
     * @throws JsonUtils.JsonConversionException 역직렬화 실패 시
     */
    public <T> T getPayloadAs(Class<T> clazz) {
        return JsonUtils.fromJson(this.payload, clazz);
    }

    /**
     * payload를 지정된 클래스로 안전하게 역직렬화
     *
     * @param clazz 변환할 클래스 타입
     * @param <T> 반환 타입
     * @return 역직렬화된 객체를 담은 Optional, 실패 시 empty
     */
    public <T> Optional<T> getPayloadAsSafe(Class<T> clazz) {
        return JsonUtils.fromJsonSafe(this.payload, clazz);
    }

    // ========================================
    // 유틸리티 메서드
    // ========================================

    /**
     * 이벤트 만료 여부 확인
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * 재시도 가능 여부 확인
     */
    public boolean canRetry() {
        return retryCount < maxRetries && !isExpired();
    }

    /**
     * 재시도 이벤트 생성
     */
    public IntegrationEvent retry() {
        return IntegrationEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .occurredAt(this.occurredAt)
                .sourceService(this.sourceService)
                .traceId(this.traceId)
                .spanId(this.spanId)
                .version(this.version)
                .payload(this.payload)
                .metadata(this.metadata)
                .aggregateId(this.aggregateId)
                .aggregateType(this.aggregateType)
                .routingKey(this.routingKey)
                .exchange(this.exchange)
                .retryCount(this.retryCount + 1)
                .maxRetries(this.maxRetries)
                .expiresAt(this.expiresAt)
                .build();
    }

    // ========================================
    // DomainEvent → IntegrationEvent 변환
    // ========================================

    public static IntegrationEvent from(DomainEvent domainEvent, String sourceService) {
        return IntegrationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(domainEvent.getEventType())
                .occurredAt(domainEvent.getOccurredAt())
                .sourceService(sourceService)
                .version(domainEvent.getVersion())
                .payload(JsonUtils.toJson(domainEvent))
                .aggregateId(domainEvent.getAggregateId())
                .aggregateType(domainEvent.getAggregateType())
                .routingKey(domainEvent.getRoutingKey())
                .build();
    }

    public static IntegrationEvent from(DomainEvent domainEvent, String sourceService, String traceId) {
        return IntegrationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(domainEvent.getEventType())
                .occurredAt(domainEvent.getOccurredAt())
                .sourceService(sourceService)
                .traceId(traceId)
                .version(domainEvent.getVersion())
                .payload(JsonUtils.toJson(domainEvent))
                .aggregateId(domainEvent.getAggregateId())
                .aggregateType(domainEvent.getAggregateType())
                .routingKey(domainEvent.getRoutingKey())
                .build();
    }

    public static IntegrationEvent from(DomainEvent domainEvent, String sourceService, String traceId, String routingKey) {
        return IntegrationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(domainEvent.getEventType())
                .occurredAt(domainEvent.getOccurredAt())
                .sourceService(sourceService)
                .traceId(traceId)
                .version(domainEvent.getVersion())
                .payload(JsonUtils.toJson(domainEvent))
                .aggregateId(domainEvent.getAggregateId())
                .aggregateType(domainEvent.getAggregateType())
                .routingKey(routingKey)
                .build();
    }

    // ========================================
    // 직접 생성
    // ========================================

    public static IntegrationEvent create(String eventType, String sourceService, Object payload, String routingKey) {
        return IntegrationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .occurredAt(Instant.now())
                .sourceService(sourceService)
                .version(1)
                .payload(JsonUtils.toJson(payload))
                .routingKey(routingKey)
                .build();
    }

    public static IntegrationEvent createWithTtl(String eventType, String sourceService, Object payload, String routingKey, long ttlSeconds) {
        return IntegrationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .occurredAt(Instant.now())
                .sourceService(sourceService)
                .version(1)
                .payload(JsonUtils.toJson(payload))
                .routingKey(routingKey)
                .expiresAt(Instant.now().plusSeconds(ttlSeconds))
                .build();
    }
}