package io.github.tickatch.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.tickatch.common.logging.MdcUtils;
import io.github.tickatch.common.util.JsonUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.util.StringUtils;

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
 * <p>traceId는 MDC에서 자동으로 추출되므로 별도로 지정하지 않아도 된다.
 * 이벤트 체이닝 시에도 {@link EventContext}를 사용하면 traceId가 자동으로 유지된다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * // HTTP 요청 컨텍스트에서 이벤트 발행 (traceId 자동 포함)
 * TicketCreatedEvent domainEvent = new TicketCreatedEvent(ticketId, eventName, quantity);
 * IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, "ticket-service");
 *
 * rabbitTemplate.convertAndSend(exchange, integrationEvent.getRoutingKey(), integrationEvent);
 *
 * // 수신 측에서 payload 복원
 * TicketCreatedEvent restored = integrationEvent.getPayloadAs(TicketCreatedEvent.class);
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see EventContext
 * @see MdcUtils
 */
@Getter
@Builder
@Jacksonized
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
   * payload를 지정된 클래스로 역직렬화한다.
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
   * payload를 지정된 클래스로 안전하게 역직렬화한다.
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
   * 이벤트 만료 여부를 확인한다.
   *
   * @return 만료되었으면 true
   */
  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  /**
   * 재시도 가능 여부를 확인한다.
   *
   * @return 재시도 가능하면 true
   */
  public boolean canRetry() {
    return retryCount < maxRetries && !isExpired();
  }

  /**
   * 재시도 이벤트를 생성한다.
   *
   * @return retryCount가 1 증가된 새 IntegrationEvent
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
  // traceId 결정 로직
  // ========================================

  /**
   * traceId를 결정한다.
   *
   * <p>우선순위:
   * <ol>
   *   <li>명시적으로 전달된 traceId</li>
   *   <li>MDC에 저장된 requestId</li>
   *   <li>둘 다 없으면 null</li>
   * </ol>
   *
   * @param explicitTraceId 명시적으로 전달된 traceId (nullable)
   * @return 결정된 traceId
   */
  private static String resolveTraceId(String explicitTraceId) {
    if (StringUtils.hasText(explicitTraceId)) {
      return explicitTraceId;
    }
    return MdcUtils.getRequestId();
  }

  // ========================================
  // DomainEvent → IntegrationEvent 변환
  // ========================================

  /**
   * DomainEvent를 IntegrationEvent로 변환한다.
   *
   * <p>MDC에 traceId(requestId)가 있으면 자동으로 포함된다.
   *
   * @param domainEvent 도메인 이벤트
   * @param sourceService 발행 서비스명
   * @return IntegrationEvent
   */
  public static IntegrationEvent from(DomainEvent domainEvent, String sourceService) {
    return IntegrationEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType(domainEvent.getEventType())
        .occurredAt(domainEvent.getOccurredAt())
        .sourceService(sourceService)
        .traceId(resolveTraceId(null))
        .version(domainEvent.getVersion())
        .payload(JsonUtils.toJson(domainEvent))
        .aggregateId(domainEvent.getAggregateId())
        .aggregateType(domainEvent.getAggregateType())
        .routingKey(domainEvent.getRoutingKey())
        .build();
  }

  /**
   * DomainEvent를 IntegrationEvent로 변환한다.
   *
   * <p>명시적으로 traceId를 지정할 수 있다. null이면 MDC에서 추출한다.
   *
   * @param domainEvent 도메인 이벤트
   * @param sourceService 발행 서비스명
   * @param traceId 명시적 traceId (null이면 MDC에서 추출)
   * @return IntegrationEvent
   */
  public static IntegrationEvent from(DomainEvent domainEvent, String sourceService, String traceId) {
    return IntegrationEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType(domainEvent.getEventType())
        .occurredAt(domainEvent.getOccurredAt())
        .sourceService(sourceService)
        .traceId(resolveTraceId(traceId))
        .version(domainEvent.getVersion())
        .payload(JsonUtils.toJson(domainEvent))
        .aggregateId(domainEvent.getAggregateId())
        .aggregateType(domainEvent.getAggregateType())
        .routingKey(domainEvent.getRoutingKey())
        .build();
  }

  /**
   * DomainEvent를 IntegrationEvent로 변환한다.
   *
   * <p>traceId와 routingKey를 명시적으로 지정할 수 있다.
   *
   * @param domainEvent 도메인 이벤트
   * @param sourceService 발행 서비스명
   * @param traceId 명시적 traceId (null이면 MDC에서 추출)
   * @param routingKey 라우팅 키
   * @return IntegrationEvent
   */
  public static IntegrationEvent from(DomainEvent domainEvent, String sourceService, String traceId, String routingKey) {
    return IntegrationEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType(domainEvent.getEventType())
        .occurredAt(domainEvent.getOccurredAt())
        .sourceService(sourceService)
        .traceId(resolveTraceId(traceId))
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

  /**
   * IntegrationEvent를 직접 생성한다.
   *
   * <p>MDC에 traceId가 있으면 자동으로 포함된다.
   *
   * @param eventType 이벤트 타입
   * @param sourceService 발행 서비스명
   * @param payload 페이로드 객체
   * @param routingKey 라우팅 키
   * @return IntegrationEvent
   */
  public static IntegrationEvent create(String eventType, String sourceService, Object payload, String routingKey) {
    return IntegrationEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType(eventType)
        .occurredAt(Instant.now())
        .sourceService(sourceService)
        .traceId(resolveTraceId(null))
        .version(1)
        .payload(JsonUtils.toJson(payload))
        .routingKey(routingKey)
        .build();
  }

  /**
   * TTL이 있는 IntegrationEvent를 직접 생성한다.
   *
   * @param eventType 이벤트 타입
   * @param sourceService 발행 서비스명
   * @param payload 페이로드 객체
   * @param routingKey 라우팅 키
   * @param ttlSeconds 만료 시간 (초)
   * @return IntegrationEvent
   */
  public static IntegrationEvent createWithTtl(String eventType, String sourceService, Object payload, String routingKey, long ttlSeconds) {
    return IntegrationEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType(eventType)
        .occurredAt(Instant.now())
        .sourceService(sourceService)
        .traceId(resolveTraceId(null))
        .version(1)
        .payload(JsonUtils.toJson(payload))
        .routingKey(routingKey)
        .expiresAt(Instant.now().plusSeconds(ttlSeconds))
        .build();
  }
}