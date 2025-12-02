package io.github.tickatch.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * DomainEvent 단위 테스트.
 */
@DisplayName("DomainEvent 테스트")
class DomainEventTest {

    // ========================================
    // 기본 생성자 테스트
    // ========================================

    @Nested
    @DisplayName("기본 생성자 테스트")
    class DefaultConstructorTest {

        @Test
        @DisplayName("eventId가 UUID 형식으로 자동 생성된다")
        void constructor_generatesUuidEventId() {
            // when
            TestDomainEvent event = new TestDomainEvent("aggregateId");

            // then
            assertThat(event.getEventId()).isNotNull();
            assertThatCode(() -> UUID.fromString(event.getEventId())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("eventType이 클래스 이름으로 설정된다")
        void constructor_setsEventTypeToClassName() {
            // when
            TestDomainEvent event = new TestDomainEvent("aggregateId");

            // then
            assertThat(event.getEventType()).isEqualTo("TestDomainEvent");
        }

        @Test
        @DisplayName("occurredAt이 현재 시간으로 설정된다")
        void constructor_setsOccurredAtToNow() {
            // given
            Instant before = Instant.now();

            // when
            TestDomainEvent event = new TestDomainEvent("aggregateId");

            // then
            Instant after = Instant.now();
            assertThat(event.getOccurredAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("기본 version은 1이다")
        void constructor_setsDefaultVersionToOne() {
            // when
            TestDomainEvent event = new TestDomainEvent("aggregateId");

            // then
            assertThat(event.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("각 이벤트는 고유한 eventId를 갖는다")
        void constructor_generatesUniqueEventIds() {
            // when
            TestDomainEvent event1 = new TestDomainEvent("aggregateId");
            TestDomainEvent event2 = new TestDomainEvent("aggregateId");

            // then
            assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
        }
    }

    // ========================================
    // 버전 지정 생성자 테스트
    // ========================================

    @Nested
    @DisplayName("버전 지정 생성자 테스트")
    class VersionConstructorTest {

        @Test
        @DisplayName("지정한 버전으로 설정된다")
        void constructor_setsSpecifiedVersion() {
            // when
            VersionedDomainEvent event = new VersionedDomainEvent("aggregateId", 5);

            // then
            assertThat(event.getVersion()).isEqualTo(5);
        }

        @Test
        @DisplayName("버전이 0이어도 설정된다")
        void constructor_allowsVersionZero() {
            // when
            VersionedDomainEvent event = new VersionedDomainEvent("aggregateId", 0);

            // then
            assertThat(event.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("다른 메타데이터는 자동 생성된다")
        void constructor_generatesOtherMetadata() {
            // when
            VersionedDomainEvent event = new VersionedDomainEvent("aggregateId", 3);

            // then
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getEventType()).isEqualTo("VersionedDomainEvent");
            assertThat(event.getOccurredAt()).isNotNull();
        }
    }

    // ========================================
    // 복원용 생성자 테스트
    // ========================================

    @Nested
    @DisplayName("복원용 생성자 테스트")
    class RestorationConstructorTest {

        @Test
        @DisplayName("eventId와 occurredAt으로 복원한다")
        void constructor_restoresWithEventIdAndOccurredAt() {
            // given
            String eventId = "restored-event-id";
            Instant occurredAt = Instant.parse("2025-01-15T10:30:00Z");

            // when
            RestoredDomainEvent event = new RestoredDomainEvent("aggregateId", eventId, occurredAt);

            // then
            assertThat(event.getEventId()).isEqualTo(eventId);
            assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
            assertThat(event.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("eventId, occurredAt, version으로 복원한다")
        void constructor_restoresWithAllParams() {
            // given
            String eventId = "restored-event-id";
            Instant occurredAt = Instant.parse("2025-01-15T10:30:00Z");
            int version = 7;

            // when
            FullyRestoredDomainEvent event = new FullyRestoredDomainEvent(
                    "aggregateId", eventId, occurredAt, version
            );

            // then
            assertThat(event.getEventId()).isEqualTo(eventId);
            assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
            assertThat(event.getVersion()).isEqualTo(version);
        }

        @Test
        @DisplayName("eventType은 복원 시에도 클래스 이름으로 설정된다")
        void constructor_setsEventTypeOnRestore() {
            // given
            String eventId = "restored-event-id";
            Instant occurredAt = Instant.now();

            // when
            RestoredDomainEvent event = new RestoredDomainEvent("aggregateId", eventId, occurredAt);

            // then
            assertThat(event.getEventType()).isEqualTo("RestoredDomainEvent");
        }
    }

    // ========================================
    // Aggregate 메서드 테스트
    // ========================================

    @Nested
    @DisplayName("Aggregate 메서드 테스트")
    class AggregateMethodTest {

        @Test
        @DisplayName("getAggregateId() 기본값은 null이다")
        void getAggregateId_defaultReturnsNull() {
            // when
            DefaultAggregateEvent event = new DefaultAggregateEvent();

            // then
            assertThat(event.getAggregateId()).isNull();
        }

        @Test
        @DisplayName("getAggregateType() 기본값은 null이다")
        void getAggregateType_defaultReturnsNull() {
            // when
            DefaultAggregateEvent event = new DefaultAggregateEvent();

            // then
            assertThat(event.getAggregateType()).isNull();
        }

        @Test
        @DisplayName("getAggregateId()를 오버라이드할 수 있다")
        void getAggregateId_canBeOverridden() {
            // when
            TestDomainEvent event = new TestDomainEvent("my-aggregate-123");

            // then
            assertThat(event.getAggregateId()).isEqualTo("my-aggregate-123");
        }

        @Test
        @DisplayName("getAggregateType()을 오버라이드할 수 있다")
        void getAggregateType_canBeOverridden() {
            // when
            TestDomainEvent event = new TestDomainEvent("aggregateId");

            // then
            assertThat(event.getAggregateType()).isEqualTo("TestAggregate");
        }
    }

    // ========================================
    // getRoutingKey() 테스트
    // ========================================

    @Nested
    @DisplayName("getRoutingKey() 테스트")
    class RoutingKeyTest {

        @Test
        @DisplayName("aggregateType이 있으면 {aggregateType}.{eventType} 형식이다")
        void getRoutingKey_withAggregateType_returnsCompositeKey() {
            // when
            TestDomainEvent event = new TestDomainEvent("aggregateId");

            // then
            assertThat(event.getRoutingKey()).isEqualTo("testaggregate.TestDomainEvent");
        }

        @Test
        @DisplayName("aggregateType이 null이면 eventType만 반환한다")
        void getRoutingKey_withNullAggregateType_returnsEventType() {
            // when
            DefaultAggregateEvent event = new DefaultAggregateEvent();

            // then
            assertThat(event.getRoutingKey()).isEqualTo("defaultaggregateevent");
        }

        @Test
        @DisplayName("aggregateType이 빈 문자열이면 eventType만 반환한다")
        void getRoutingKey_withEmptyAggregateType_returnsEventType() {
            // when
            EmptyAggregateTypeEvent event = new EmptyAggregateTypeEvent();

            // then
            assertThat(event.getRoutingKey()).isEqualTo("emptyaggregatetypeevent");
        }
    }

    // ========================================
    // Serializable 테스트
    // ========================================

    @Nested
    @DisplayName("Serializable 테스트")
    class SerializableTest {

        @Test
        @DisplayName("DomainEvent는 Serializable이다")
        void domainEvent_isSerializable() {
            // when
            TestDomainEvent event = new TestDomainEvent("aggregateId");

            // then
            assertThat(event).isInstanceOf(java.io.Serializable.class);
        }
    }

    // ========================================
    // 다양한 이벤트 시나리오 테스트
    // ========================================

    @Nested
    @DisplayName("시나리오 테스트")
    class ScenarioTest {

        @Test
        @DisplayName("티켓 생성 이벤트 시나리오")
        void ticketCreatedEvent_scenario() {
            // when
            TicketCreatedEvent event = new TicketCreatedEvent(123L, "콘서트", 2);

            // then
            assertThat(event.getEventType()).isEqualTo("TicketCreatedEvent");
            assertThat(event.getAggregateId()).isEqualTo("123");
            assertThat(event.getAggregateType()).isEqualTo("Ticket");
            assertThat(event.getRoutingKey()).isEqualTo("ticket.TicketCreatedEvent");
            assertThat(event.getTicketId()).isEqualTo(123L);
            assertThat(event.getEventName()).isEqualTo("콘서트");
            assertThat(event.getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("주문 완료 이벤트 시나리오")
        void orderCompletedEvent_scenario() {
            // when
            OrderCompletedEvent event = new OrderCompletedEvent("ORD-2025-001", 50000L);

            // then
            assertThat(event.getEventType()).isEqualTo("OrderCompletedEvent");
            assertThat(event.getAggregateId()).isEqualTo("ORD-2025-001");
            assertThat(event.getAggregateType()).isEqualTo("Order");
            assertThat(event.getRoutingKey()).isEqualTo("order.OrderCompletedEvent");
        }
    }

    // ========================================
    // 테스트용 이벤트 클래스들
    // ========================================

    static class TestDomainEvent extends DomainEvent {
        private final String aggregateId;

        TestDomainEvent(String aggregateId) {
            super();
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public String getAggregateType() {
            return "TestAggregate";
        }
    }

    static class VersionedDomainEvent extends DomainEvent {
        private final String aggregateId;

        VersionedDomainEvent(String aggregateId, int version) {
            super(version);
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }

    static class RestoredDomainEvent extends DomainEvent {
        private final String aggregateId;

        RestoredDomainEvent(String aggregateId, String eventId, Instant occurredAt) {
            super(eventId, occurredAt);
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }

    static class FullyRestoredDomainEvent extends DomainEvent {
        private final String aggregateId;

        FullyRestoredDomainEvent(String aggregateId, String eventId, Instant occurredAt, int version) {
            super(eventId, occurredAt, version);
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }

    static class DefaultAggregateEvent extends DomainEvent {
        DefaultAggregateEvent() {
            super();
        }
        // getAggregateId(), getAggregateType() 오버라이드 안 함
    }

    static class EmptyAggregateTypeEvent extends DomainEvent {
        EmptyAggregateTypeEvent() {
            super();
        }

        @Override
        public String getAggregateType() {
            return "";
        }
    }

    static class TicketCreatedEvent extends DomainEvent {
        private final Long ticketId;
        private final String eventName;
        private final int quantity;

        TicketCreatedEvent(Long ticketId, String eventName, int quantity) {
            super();
            this.ticketId = ticketId;
            this.eventName = eventName;
            this.quantity = quantity;
        }

        @Override
        public String getAggregateId() {
            return String.valueOf(ticketId);
        }

        @Override
        public String getAggregateType() {
            return "Ticket";
        }

        public Long getTicketId() {
            return ticketId;
        }

        public String getEventName() {
            return eventName;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    static class OrderCompletedEvent extends DomainEvent {
        private final String orderId;
        private final Long totalAmount;

        OrderCompletedEvent(String orderId, Long totalAmount) {
            super();
            this.orderId = orderId;
            this.totalAmount = totalAmount;
        }

        @Override
        public String getAggregateId() {
            return orderId;
        }

        @Override
        public String getAggregateType() {
            return "Order";
        }

        public String getOrderId() {
            return orderId;
        }

        public Long getTotalAmount() {
            return totalAmount;
        }
    }
}