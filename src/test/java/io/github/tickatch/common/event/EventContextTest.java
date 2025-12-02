package io.github.tickatch.common.event;

import io.github.tickatch.common.logging.MdcUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * EventContext 단위 테스트.
 */
@DisplayName("EventContext 테스트")
class EventContextTest {

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  // ========================================
  // 상수 테스트
  // ========================================

  @Nested
  @DisplayName("상수 테스트")
  class ConstantsTest {

    @Test
    @DisplayName("SOURCE_SERVICE 상수 값을 확인한다")
    void sourceService_constant() {
      assertThat(EventContext.SOURCE_SERVICE).isEqualTo("sourceService");
    }

    @Test
    @DisplayName("EVENT_TYPE 상수 값을 확인한다")
    void eventType_constant() {
      assertThat(EventContext.EVENT_TYPE).isEqualTo("eventType");
    }
  }

  // ========================================
  // run(Consumer) 테스트
  // ========================================

  @Nested
  @DisplayName("run(Consumer) 테스트")
  class RunConsumerTest {

    @Test
    @DisplayName("이벤트의 traceId를 MDC에 설정한다")
    void run_setsTraceIdToMdc() {
      // given
      String traceId = UUID.randomUUID().toString();
      IntegrationEvent event = createEventWithTraceId(traceId);
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      // when
      EventContext.run(event, e -> {
        capturedTraceId.set(MdcUtils.getRequestId());
      });

      // then
      assertThat(capturedTraceId.get()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("실행 후 MDC가 클리어된다")
    void run_clearsMdcAfterExecution() {
      // given
      IntegrationEvent event = createEventWithTraceId("trace-123");

      // when
      EventContext.run(event, e -> {
        // 실행 중에는 MDC에 값이 있음
        assertThat(MdcUtils.getRequestId()).isEqualTo("trace-123");
      });

      // then - 실행 후 MDC 클리어됨
      assertThat(MdcUtils.getRequestId()).isNull();
    }

    @Test
    @DisplayName("예외 발생 시에도 MDC가 클리어된다")
    void run_clearsMdcOnException() {
      // given
      IntegrationEvent event = createEventWithTraceId("trace-123");

      // when & then
      assertThatThrownBy(() -> {
        EventContext.run(event, e -> {
          throw new RuntimeException("테스트 예외");
        });
      }).isInstanceOf(RuntimeException.class);

      assertThat(MdcUtils.getRequestId()).isNull();
    }

    @Test
    @DisplayName("sourceService를 MDC에 설정한다")
    void run_setsSourceServiceToMdc() {
      // given
      IntegrationEvent event = IntegrationEvent.builder()
          .eventId("id")
          .eventType("TestEvent")
          .occurredAt(Instant.now())
          .sourceService("order-service")
          .traceId("trace-123")
          .payload("{}")
          .build();
      AtomicReference<String> capturedSourceService = new AtomicReference<>();

      // when
      EventContext.run(event, e -> {
        capturedSourceService.set(MdcUtils.get(EventContext.SOURCE_SERVICE));
      });

      // then
      assertThat(capturedSourceService.get()).isEqualTo("order-service");
    }

    @Test
    @DisplayName("eventType을 MDC에 설정한다")
    void run_setsEventTypeToMdc() {
      // given
      IntegrationEvent event = IntegrationEvent.builder()
          .eventId("id")
          .eventType("OrderCreatedEvent")
          .occurredAt(Instant.now())
          .sourceService("service")
          .traceId("trace-123")
          .payload("{}")
          .build();
      AtomicReference<String> capturedEventType = new AtomicReference<>();

      // when
      EventContext.run(event, e -> {
        capturedEventType.set(MdcUtils.get(EventContext.EVENT_TYPE));
      });

      // then
      assertThat(capturedEventType.get()).isEqualTo("OrderCreatedEvent");
    }

    @Test
    @DisplayName("null 이벤트면 새 traceId를 생성한다")
    void run_withNullEvent_generatesNewTraceId() {
      // given
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      // when
      EventContext.run(null, e -> {
        capturedTraceId.set(MdcUtils.getRequestId());
      });

      // then
      assertThat(capturedTraceId.get()).isNotNull();
      assertThatCode(() -> UUID.fromString(capturedTraceId.get())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이벤트에 traceId가 없으면 새로 생성한다")
    void run_withoutTraceId_generatesNewTraceId() {
      // given
      IntegrationEvent event = IntegrationEvent.builder()
          .eventId("id")
          .eventType("TestEvent")
          .occurredAt(Instant.now())
          .sourceService("service")
          .payload("{}")
          // traceId 없음
          .build();
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      // when
      EventContext.run(event, e -> {
        capturedTraceId.set(MdcUtils.getRequestId());
      });

      // then
      assertThat(capturedTraceId.get()).isNotNull();
      assertThatCode(() -> UUID.fromString(capturedTraceId.get())).doesNotThrowAnyException();
    }
  }

  // ========================================
  // execute(Function) 테스트
  // ========================================

  @Nested
  @DisplayName("execute(Function) 테스트")
  class ExecuteFunctionTest {

    @Test
    @DisplayName("작업 결과를 반환한다")
    void execute_returnsResult() {
      // given
      IntegrationEvent event = createEventWithTraceId("trace-123");

      // when
      String result = EventContext.execute(event, e -> "결과값");

      // then
      assertThat(result).isEqualTo("결과값");
    }

    @Test
    @DisplayName("이벤트의 traceId를 MDC에 설정한다")
    void execute_setsTraceIdToMdc() {
      // given
      String traceId = UUID.randomUUID().toString();
      IntegrationEvent event = createEventWithTraceId(traceId);

      // when
      String capturedTraceId = EventContext.execute(event, e -> MdcUtils.getRequestId());

      // then
      assertThat(capturedTraceId).isEqualTo(traceId);
    }

    @Test
    @DisplayName("실행 후 MDC가 클리어된다")
    void execute_clearsMdcAfterExecution() {
      // given
      IntegrationEvent event = createEventWithTraceId("trace-123");

      // when
      EventContext.execute(event, e -> "result");

      // then
      assertThat(MdcUtils.getRequestId()).isNull();
    }

    @Test
    @DisplayName("예외 발생 시에도 MDC가 클리어된다")
    void execute_clearsMdcOnException() {
      // given
      IntegrationEvent event = createEventWithTraceId("trace-123");

      // when & then
      assertThatThrownBy(() -> {
        EventContext.execute(event, e -> {
          throw new RuntimeException("테스트 예외");
        });
      }).isInstanceOf(RuntimeException.class);

      assertThat(MdcUtils.getRequestId()).isNull();
    }
  }

  // ========================================
  // runWithNewTrace(Runnable) 테스트
  // ========================================

  @Nested
  @DisplayName("runWithNewTrace(Runnable) 테스트")
  class RunWithNewTraceRunnableTest {

    @Test
    @DisplayName("새 traceId를 생성한다")
    void runWithNewTrace_generatesNewTraceId() {
      // given
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      // when
      EventContext.runWithNewTrace(() -> {
        capturedTraceId.set(MdcUtils.getRequestId());
      });

      // then
      assertThat(capturedTraceId.get()).isNotNull();
      assertThatCode(() -> UUID.fromString(capturedTraceId.get())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실행 후 MDC가 클리어된다")
    void runWithNewTrace_clearsMdcAfterExecution() {
      // when
      EventContext.runWithNewTrace(() -> {
        assertThat(MdcUtils.getRequestId()).isNotNull();
      });

      // then
      assertThat(MdcUtils.getRequestId()).isNull();
    }

    @Test
    @DisplayName("기존 MDC 값을 덮어쓴다")
    void runWithNewTrace_overridesExistingMdc() {
      // given
      String existingTraceId = "existing-trace-id";
      MdcUtils.setRequestId(existingTraceId);
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      // when
      EventContext.runWithNewTrace(() -> {
        capturedTraceId.set(MdcUtils.getRequestId());
      });

      // then
      assertThat(capturedTraceId.get()).isNotEqualTo(existingTraceId);
    }
  }

  // ========================================
  // executeWithNewTrace(Supplier) 테스트
  // ========================================

  @Nested
  @DisplayName("executeWithNewTrace(Supplier) 테스트")
  class ExecuteWithNewTraceSupplierTest {

    @Test
    @DisplayName("작업 결과를 반환한다")
    void executeWithNewTrace_returnsResult() {
      // when
      Integer result = EventContext.executeWithNewTrace(() -> 42);

      // then
      assertThat(result).isEqualTo(42);
    }

    @Test
    @DisplayName("새 traceId를 생성한다")
    void executeWithNewTrace_generatesNewTraceId() {
      // when
      String traceId = EventContext.executeWithNewTrace(MdcUtils::getRequestId);

      // then
      assertThat(traceId).isNotNull();
      assertThatCode(() -> UUID.fromString(traceId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실행 후 MDC가 클리어된다")
    void executeWithNewTrace_clearsMdcAfterExecution() {
      // when
      EventContext.executeWithNewTrace(() -> "result");

      // then
      assertThat(MdcUtils.getRequestId()).isNull();
    }
  }

  // ========================================
  // setupMdc() / clearMdc() 테스트
  // ========================================

  @Nested
  @DisplayName("setupMdc() / clearMdc() 테스트")
  class ManualMdcTest {

    @Test
    @DisplayName("setupMdc()는 traceId를 MDC에 설정한다")
    void setupMdc_setsTraceId() {
      // given
      String traceId = "manual-trace-id";
      IntegrationEvent event = createEventWithTraceId(traceId);

      // when
      EventContext.setupMdc(event);

      // then
      assertThat(MdcUtils.getRequestId()).isEqualTo(traceId);

      // cleanup
      EventContext.clearMdc();
    }

    @Test
    @DisplayName("clearMdc()는 모든 MDC 값을 제거한다")
    void clearMdc_removesAllValues() {
      // given
      IntegrationEvent event = IntegrationEvent.builder()
          .eventId("id")
          .eventType("TestEvent")
          .occurredAt(Instant.now())
          .sourceService("service")
          .traceId("trace-123")
          .payload("{}")
          .build();
      EventContext.setupMdc(event);

      // when
      EventContext.clearMdc();

      // then
      assertThat(MdcUtils.getRequestId()).isNull();
      assertThat(MdcUtils.get(EventContext.SOURCE_SERVICE)).isNull();
      assertThat(MdcUtils.get(EventContext.EVENT_TYPE)).isNull();
    }
  }

  // ========================================
  // 조회 메서드 테스트
  // ========================================

  @Nested
  @DisplayName("조회 메서드 테스트")
  class QueryMethodsTest {

    @Test
    @DisplayName("currentTraceId()는 현재 traceId를 반환한다")
    void currentTraceId_returnsTraceId() {
      // given
      String traceId = "current-trace-id";
      MdcUtils.setRequestId(traceId);

      // when
      String result = EventContext.currentTraceId();

      // then
      assertThat(result).isEqualTo(traceId);
    }

    @Test
    @DisplayName("hasTraceId()는 traceId 존재 여부를 반환한다")
    void hasTraceId_returnsExistence() {
      // given - 없을 때
      assertThat(EventContext.hasTraceId()).isFalse();

      // when
      MdcUtils.setRequestId("trace-id");

      // then
      assertThat(EventContext.hasTraceId()).isTrue();
    }

    @Test
    @DisplayName("currentSourceService()는 sourceService를 반환한다")
    void currentSourceService_returnsValue() {
      // given
      MdcUtils.put(EventContext.SOURCE_SERVICE, "order-service");

      // when
      String result = EventContext.currentSourceService();

      // then
      assertThat(result).isEqualTo("order-service");
    }

    @Test
    @DisplayName("currentEventType()은 eventType을 반환한다")
    void currentEventType_returnsValue() {
      // given
      MdcUtils.put(EventContext.EVENT_TYPE, "OrderCreated");

      // when
      String result = EventContext.currentEventType();

      // then
      assertThat(result).isEqualTo("OrderCreated");
    }
  }

  // ========================================
  // 이벤트 체이닝 시나리오 테스트
  // ========================================

  @Nested
  @DisplayName("이벤트 체이닝 시나리오 테스트")
  class EventChainingScenarioTest {

    @Test
    @DisplayName("이벤트 수신 후 발행 시 같은 traceId가 유지된다")
    void eventChaining_maintainsSameTraceId() {
      // given - 첫 번째 이벤트 수신
      String originalTraceId = "original-trace-id";
      IntegrationEvent receivedEvent = createEventWithTraceId(originalTraceId);
      AtomicReference<String> newEventTraceId = new AtomicReference<>();

      // when - 이벤트 처리 중 새 이벤트 발행
      EventContext.run(receivedEvent, e -> {
        // IntegrationEvent.from()은 MDC에서 traceId를 자동 추출
        IntegrationEvent newEvent = IntegrationEvent.create(
            "NewEvent", "service", "payload", "key"
        );
        newEventTraceId.set(newEvent.getTraceId());
      });

      // then - 새 이벤트도 같은 traceId를 가짐
      assertThat(newEventTraceId.get()).isEqualTo(originalTraceId);
    }

    @Test
    @DisplayName("다중 이벤트 체이닝에서도 traceId가 유지된다")
    void multipleEventChaining_maintainsTraceId() {
      // given
      String originalTraceId = "chain-trace-id";
      IntegrationEvent event1 = createEventWithTraceId(originalTraceId);

      // when - 첫 번째 이벤트 처리
      AtomicReference<String> event2TraceId = new AtomicReference<>();
      EventContext.run(event1, e -> {
        IntegrationEvent event2 = IntegrationEvent.create("Event2", "service", "{}", "key");
        event2TraceId.set(event2.getTraceId());
      });

      // then - 두 번째 이벤트도 같은 traceId
      assertThat(event2TraceId.get()).isEqualTo(originalTraceId);

      // when - 두 번째 이벤트 처리 (별도 실행)
      IntegrationEvent event2 = createEventWithTraceId(event2TraceId.get());
      AtomicReference<String> event3TraceId = new AtomicReference<>();
      EventContext.run(event2, e -> {
        IntegrationEvent event3 = IntegrationEvent.create("Event3", "service", "{}", "key");
        event3TraceId.set(event3.getTraceId());
      });

      // then - 세 번째 이벤트도 같은 traceId
      assertThat(event3TraceId.get()).isEqualTo(originalTraceId);
    }
  }

  // ========================================
  // 헬퍼 메서드
  // ========================================

  private IntegrationEvent createEventWithTraceId(String traceId) {
    return IntegrationEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("TestEvent")
        .occurredAt(Instant.now())
        .sourceService("test-service")
        .traceId(traceId)
        .payload("{}")
        .build();
  }
}