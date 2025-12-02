package io.github.tickatch.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * IntegrationEvent 단위 테스트.
 */
@DisplayName("IntegrationEvent 테스트")
class IntegrationEventTest {

    // ========================================
    // from(DomainEvent, sourceService) 테스트
    // ========================================

    @Nested
    @DisplayName("from(DomainEvent, sourceService) 테스트")
    class FromDomainEventTest {

        @Test
        @DisplayName("DomainEvent에서 IntegrationEvent를 생성한다")
        void from_createsIntegrationEvent() {
            // given
            TestDomainEvent domainEvent = new TestDomainEvent(123L, "테스트 이벤트");

            // when
            IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, "ticket-service");

            // then
            assertThat(integrationEvent.getEventId()).isNotNull();
            assertThat(integrationEvent.getEventType()).isEqualTo("TestDomainEvent");
            assertThat(integrationEvent.getOccurredAt()).isEqualTo(domainEvent.getOccurredAt());
            assertThat(integrationEvent.getSourceService()).isEqualTo("ticket-service");
            assertThat(integrationEvent.getVersion()).isEqualTo(domainEvent.getVersion());
            assertThat(integrationEvent.getPayload()).isEqualTo(domainEvent);
            assertThat(integrationEvent.getAggregateId()).isEqualTo("123");
            assertThat(integrationEvent.getAggregateType()).isEqualTo("TestAggregate");
            assertThat(integrationEvent.getRoutingKey()).isEqualTo("testaggregate.TestDomainEvent");
        }

        @Test
        @DisplayName("새로운 eventId가 생성된다")
        void from_generatesNewEventId() {
            // given
            TestDomainEvent domainEvent = new TestDomainEvent(123L, "테스트");

            // when
            IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, "service");

            // then
            assertThat(integrationEvent.getEventId()).isNotEqualTo(domainEvent.getEventId());
            assertThatCode(() -> UUID.fromString(integrationEvent.getEventId())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("traceId는 null이다")
        void from_traceIdIsNull() {
            // given
            TestDomainEvent domainEvent = new TestDomainEvent(123L, "테스트");

            // when
            IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, "service");

            // then
            assertThat(integrationEvent.getTraceId()).isNull();
        }
    }

    // ========================================
    // from(DomainEvent, sourceService, traceId) 테스트
    // ========================================

    @Nested
    @DisplayName("from(DomainEvent, sourceService, traceId) 테스트")
    class FromDomainEventWithTraceIdTest {

        @Test
        @DisplayName("traceId를 포함한 IntegrationEvent를 생성한다")
        void from_includesTraceId() {
            // given
            TestDomainEvent domainEvent = new TestDomainEvent(123L, "테스트");
            String traceId = "trace-abc-123";

            // when
            IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, "service", traceId);

            // then
            assertThat(integrationEvent.getTraceId()).isEqualTo(traceId);
        }

        @Test
        @DisplayName("다른 필드들도 올바르게 설정된다")
        void from_setsOtherFields() {
            // given
            TestDomainEvent domainEvent = new TestDomainEvent(456L, "이벤트");
            String traceId = "trace-xyz";

            // when
            IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, "order-service", traceId);

            // then
            assertThat(integrationEvent.getSourceService()).isEqualTo("order-service");
            assertThat(integrationEvent.getAggregateId()).isEqualTo("456");
        }
    }

    // ========================================
    // from(DomainEvent, sourceService, traceId, routingKey) 테스트
    // ========================================

    @Nested
    @DisplayName("from(DomainEvent, sourceService, traceId, routingKey) 테스트")
    class FromDomainEventWithRoutingKeyTest {

        @Test
        @DisplayName("커스텀 routingKey를 설정한다")
        void from_setsCustomRoutingKey() {
            // given
            TestDomainEvent domainEvent = new TestDomainEvent(123L, "테스트");
            String customRoutingKey = "custom.routing.key";

            // when
            IntegrationEvent integrationEvent = IntegrationEvent.from(
                    domainEvent, "service", "trace-id", customRoutingKey
            );

            // then
            assertThat(integrationEvent.getRoutingKey()).isEqualTo(customRoutingKey);
        }

        @Test
        @DisplayName("DomainEvent의 기본 routingKey를 덮어쓴다")
        void from_overridesDomainEventRoutingKey() {
            // given
            TestDomainEvent domainEvent = new TestDomainEvent(123L, "테스트");
            String customRoutingKey = "override.routing.key";

            // when
            IntegrationEvent integrationEvent = IntegrationEvent.from(
                    domainEvent, "service", "trace", customRoutingKey
            );

            // then
            assertThat(integrationEvent.getRoutingKey()).isNotEqualTo(domainEvent.getRoutingKey());
            assertThat(integrationEvent.getRoutingKey()).isEqualTo(customRoutingKey);
        }
    }

    // ========================================
    // create() 테스트
    // ========================================

    @Nested
    @DisplayName("create() 테스트")
    class CreateTest {

        @Test
        @DisplayName("직접 IntegrationEvent를 생성한다")
        void create_createsEvent() {
            // given
            String eventType = "CustomEvent";
            String sourceService = "my-service";
            Object payload = Map.of("key", "value");
            String routingKey = "custom.route";

            // when
            IntegrationEvent event = IntegrationEvent.create(eventType, sourceService, payload, routingKey);

            // then
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getEventType()).isEqualTo(eventType);
            assertThat(event.getSourceService()).isEqualTo(sourceService);
            assertThat(event.getPayload()).isEqualTo(payload);
            assertThat(event.getRoutingKey()).isEqualTo(routingKey);
            assertThat(event.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("occurredAt이 현재 시간으로 설정된다")
        void create_setsOccurredAtToNow() {
            // given
            Instant before = Instant.now();

            // when
            IntegrationEvent event = IntegrationEvent.create("Type", "service", "payload", "key");

            // then
            Instant after = Instant.now();
            assertThat(event.getOccurredAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }
    }

    // ========================================
    // createWithTtl() 테스트
    // ========================================

    @Nested
    @DisplayName("createWithTtl() 테스트")
    class CreateWithTtlTest {

        @Test
        @DisplayName("TTL이 적용된 이벤트를 생성한다")
        void createWithTtl_setsExpiresAt() {
            // given
            long ttlSeconds = 60;
            Instant before = Instant.now();

            // when
            IntegrationEvent event = IntegrationEvent.createWithTtl(
                    "EventType", "service", "payload", "routingKey", ttlSeconds
            );

            // then
            Instant expectedExpiry = before.plusSeconds(ttlSeconds);
            assertThat(event.getExpiresAt()).isNotNull();
            assertThat(event.getExpiresAt()).isAfterOrEqualTo(expectedExpiry.minusSeconds(1));
            assertThat(event.getExpiresAt()).isBeforeOrEqualTo(expectedExpiry.plusSeconds(1));
        }

        @Test
        @DisplayName("다른 필드들도 올바르게 설정된다")
        void createWithTtl_setsOtherFields() {
            // when
            IntegrationEvent event = IntegrationEvent.createWithTtl(
                    "MyEvent", "my-service", "data", "my.route", 120
            );

            // then
            assertThat(event.getEventType()).isEqualTo("MyEvent");
            assertThat(event.getSourceService()).isEqualTo("my-service");
            assertThat(event.getPayload()).isEqualTo("data");
            assertThat(event.getRoutingKey()).isEqualTo("my.route");
            assertThat(event.getVersion()).isEqualTo(1);
        }
    }

    // ========================================
    // isExpired() 테스트
    // ========================================

    @Nested
    @DisplayName("isExpired() 테스트")
    class IsExpiredTest {

        @Test
        @DisplayName("expiresAt이 null이면 만료되지 않음")
        void isExpired_whenExpiresAtNull_returnsFalse() {
            // given
            IntegrationEvent event = IntegrationEvent.create("Type", "service", "payload", "key");

            // then
            assertThat(event.isExpired()).isFalse();
        }

        @Test
        @DisplayName("expiresAt이 미래이면 만료되지 않음")
        void isExpired_whenExpiresAtFuture_returnsFalse() {
            // given
            IntegrationEvent event = IntegrationEvent.createWithTtl("Type", "service", "payload", "key", 3600);

            // then
            assertThat(event.isExpired()).isFalse();
        }

        @Test
        @DisplayName("expiresAt이 과거이면 만료됨")
        void isExpired_whenExpiresAtPast_returnsTrue() {
            // given
            IntegrationEvent event = IntegrationEvent.builder()
                    .eventId("test")
                    .eventType("Type")
                    .occurredAt(Instant.now())
                    .sourceService("service")
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();

            // then
            assertThat(event.isExpired()).isTrue();
        }
    }

    // ========================================
    // canRetry() 테스트
    // ========================================

    @Nested
    @DisplayName("canRetry() 테스트")
    class CanRetryTest {

        @Test
        @DisplayName("retryCount < maxRetries이고 만료되지 않으면 재시도 가능")
        void canRetry_whenUnderMaxAndNotExpired_returnsTrue() {
            // given
            IntegrationEvent event = IntegrationEvent.builder()
                    .eventId("test")
                    .eventType("Type")
                    .occurredAt(Instant.now())
                    .sourceService("service")
                    .retryCount(2)
                    .maxRetries(3)
                    .build();

            // then
            assertThat(event.canRetry()).isTrue();
        }

        @Test
        @DisplayName("retryCount >= maxRetries이면 재시도 불가")
        void canRetry_whenAtMaxRetries_returnsFalse() {
            // given
            IntegrationEvent event = IntegrationEvent.builder()
                    .eventId("test")
                    .eventType("Type")
                    .occurredAt(Instant.now())
                    .sourceService("service")
                    .retryCount(3)
                    .maxRetries(3)
                    .build();

            // then
            assertThat(event.canRetry()).isFalse();
        }

        @Test
        @DisplayName("만료되면 재시도 불가")
        void canRetry_whenExpired_returnsFalse() {
            // given
            IntegrationEvent event = IntegrationEvent.builder()
                    .eventId("test")
                    .eventType("Type")
                    .occurredAt(Instant.now())
                    .sourceService("service")
                    .retryCount(0)
                    .maxRetries(3)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();

            // then
            assertThat(event.canRetry()).isFalse();
        }
    }

    // ========================================
    // retry() 테스트
    // ========================================

    @Nested
    @DisplayName("retry() 테스트")
    class RetryTest {

        @Test
        @DisplayName("retryCount를 증가시킨 새 이벤트를 생성한다")
        void retry_incrementsRetryCount() {
            // given
            IntegrationEvent original = IntegrationEvent.builder()
                    .eventId("original-id")
                    .eventType("Type")
                    .occurredAt(Instant.now())
                    .sourceService("service")
                    .retryCount(1)
                    .maxRetries(5)
                    .build();

            // when
            IntegrationEvent retried = original.retry();

            // then
            assertThat(retried.getRetryCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("다른 필드들은 유지된다")
        void retry_preservesOtherFields() {
            // given
            Instant occurredAt = Instant.now();
            IntegrationEvent original = IntegrationEvent.builder()
                    .eventId("original-id")
                    .eventType("OriginalType")
                    .occurredAt(occurredAt)
                    .sourceService("original-service")
                    .traceId("trace-123")
                    .spanId("span-456")
                    .version(2)
                    .payload("original-payload")
                    .metadata(Map.of("key", "value"))
                    .aggregateId("agg-789")
                    .aggregateType("AggType")
                    .routingKey("original.route")
                    .exchange("original-exchange")
                    .retryCount(0)
                    .maxRetries(5)
                    .expiresAt(occurredAt.plusSeconds(3600))
                    .build();

            // when
            IntegrationEvent retried = original.retry();

            // then
            assertThat(retried.getEventId()).isEqualTo("original-id");
            assertThat(retried.getEventType()).isEqualTo("OriginalType");
            assertThat(retried.getOccurredAt()).isEqualTo(occurredAt);
            assertThat(retried.getSourceService()).isEqualTo("original-service");
            assertThat(retried.getTraceId()).isEqualTo("trace-123");
            assertThat(retried.getSpanId()).isEqualTo("span-456");
            assertThat(retried.getVersion()).isEqualTo(2);
            assertThat(retried.getPayload()).isEqualTo("original-payload");
            assertThat(retried.getMetadata()).containsEntry("key", "value");
            assertThat(retried.getAggregateId()).isEqualTo("agg-789");
            assertThat(retried.getAggregateType()).isEqualTo("AggType");
            assertThat(retried.getRoutingKey()).isEqualTo("original.route");
            assertThat(retried.getExchange()).isEqualTo("original-exchange");
            assertThat(retried.getMaxRetries()).isEqualTo(5);
            assertThat(retried.getExpiresAt()).isEqualTo(occurredAt.plusSeconds(3600));
            assertThat(retried.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("원본 이벤트는 변경되지 않는다 (불변성)")
        void retry_doesNotModifyOriginal() {
            // given
            IntegrationEvent original = IntegrationEvent.builder()
                    .eventId("id")
                    .eventType("Type")
                    .occurredAt(Instant.now())
                    .sourceService("service")
                    .retryCount(0)
                    .build();

            // when
            IntegrationEvent retried = original.retry();

            // then
            assertThat(original.getRetryCount()).isEqualTo(0);
            assertThat(retried.getRetryCount()).isEqualTo(1);
            assertThat(retried).isNotSameAs(original);
        }
    }

    // ========================================
    // Builder 기본값 테스트
    // ========================================

    @Nested
    @DisplayName("Builder 기본값 테스트")
    class BuilderDefaultsTest {

        @Test
        @DisplayName("retryCount 기본값은 0이다")
        void builder_retryCountDefaultIsZero() {
            // when
            IntegrationEvent event = IntegrationEvent.builder()
                    .eventId("id")
                    .eventType("Type")
                    .occurredAt(Instant.now())
                    .sourceService("service")
                    .build();

            // then
            assertThat(event.getRetryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("maxRetries 기본값은 3이다")
        void builder_maxRetriesDefaultIsThree() {
            // when
            IntegrationEvent event = IntegrationEvent.builder()
                    .eventId("id")
                    .eventType("Type")
                    .occurredAt(Instant.now())
                    .sourceService("service")
                    .build();

            // then
            assertThat(event.getMaxRetries()).isEqualTo(3);
        }
    }

    // ========================================
    // Serializable 테스트
    // ========================================

    @Nested
    @DisplayName("Serializable 테스트")
    class SerializableTest {

        @Test
        @DisplayName("IntegrationEvent는 Serializable이다")
        void integrationEvent_isSerializable() {
            // when
            IntegrationEvent event = IntegrationEvent.create("Type", "service", "payload", "key");

            // then
            assertThat(event).isInstanceOf(java.io.Serializable.class);
        }
    }

    // ========================================
    // 시나리오 테스트
    // ========================================

    @Nested
    @DisplayName("시나리오 테스트")
    class ScenarioTest {

        @Test
        @DisplayName("DomainEvent -> IntegrationEvent 변환 후 RabbitMQ 전송 시나리오")
        void rabbitMqScenario() {
            // given
            TestDomainEvent domainEvent = new TestDomainEvent(123L, "콘서트 티켓 예매");
            String traceId = UUID.randomUUID().toString();

            // when
            IntegrationEvent integrationEvent = IntegrationEvent.from(
                    domainEvent, "ticket-service", traceId
            );

            // then
            assertThat(integrationEvent.getSourceService()).isEqualTo("ticket-service");
            assertThat(integrationEvent.getTraceId()).isEqualTo(traceId);
            assertThat(integrationEvent.getRoutingKey()).isEqualTo("testaggregate.TestDomainEvent");
            assertThat(integrationEvent.getPayload()).isInstanceOf(TestDomainEvent.class);
        }

        @Test
        @DisplayName("재시도 시나리오")
        void retryScenario() {
            // given
            IntegrationEvent event = IntegrationEvent.builder()
                    .eventId("event-001")
                    .eventType("OrderCreated")
                    .occurredAt(Instant.now())
                    .sourceService("order-service")
                    .maxRetries(3)
                    .build();

            // when - 3번 재시도
            IntegrationEvent retry1 = event.retry();
            IntegrationEvent retry2 = retry1.retry();
            IntegrationEvent retry3 = retry2.retry();

            // then
            assertThat(event.canRetry()).isTrue();
            assertThat(retry1.canRetry()).isTrue();
            assertThat(retry2.canRetry()).isTrue();
            assertThat(retry3.canRetry()).isFalse();  // maxRetries(3) 도달
            assertThat(retry3.getRetryCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("TTL 만료 시나리오")
        void ttlExpirationScenario() {
            // given - 이미 만료된 이벤트
            IntegrationEvent expiredEvent = IntegrationEvent.builder()
                    .eventId("event-expired")
                    .eventType("ExpiredEvent")
                    .occurredAt(Instant.now().minusSeconds(120))
                    .sourceService("service")
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();

            // given - 아직 유효한 이벤트
            IntegrationEvent validEvent = IntegrationEvent.createWithTtl(
                    "ValidEvent", "service", "payload", "key", 3600
            );

            // then
            assertThat(expiredEvent.isExpired()).isTrue();
            assertThat(expiredEvent.canRetry()).isFalse();
            assertThat(validEvent.isExpired()).isFalse();
            assertThat(validEvent.canRetry()).isTrue();
        }
    }

    // ========================================
    // 테스트용 DomainEvent 클래스
    // ========================================

    static class TestDomainEvent extends DomainEvent {
        private final Long id;
        private final String name;

        TestDomainEvent(Long id, String name) {
            super();
            this.id = id;
            this.name = name;
        }

        @Override
        public String getAggregateId() {
            return String.valueOf(id);
        }

        @Override
        public String getAggregateType() {
            return "TestAggregate";
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}