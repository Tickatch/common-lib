package io.github.tickatch.common.event;

import io.github.tickatch.common.logging.MdcUtils;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * IntegrationEvent 처리 시 MDC 컨텍스트를 관리하는 유틸리티.
 *
 * <p>이벤트 리스너에서 수신한 이벤트의 traceId를 MDC에 설정하고,
 * 처리 완료 후 자동으로 정리한다. 이를 통해 이벤트 체이닝 시에도
 * 동일한 traceId가 유지되어 전체 흐름을 추적할 수 있다.
 *
 * <p>주요 기능:
 * <ul>
 *   <li>수신한 이벤트의 traceId를 MDC에 복원</li>
 *   <li>이벤트 체이닝 시 traceId 자동 유지</li>
 *   <li>처리 완료 후 MDC 자동 정리</li>
 * </ul>
 *
 * <p>사용 예시 - 반환값 없음:
 * <pre>{@code
 * @RabbitListener(queues = "ticket.queue")
 * public void handle(IntegrationEvent event) {
 *     EventContext.run(event, e -> {
 *         TicketCreatedEvent payload = e.getPayloadAs(TicketCreatedEvent.class);
 *         // 처리 로직...
 *     });
 * }
 * }</pre>
 *
 * <p>사용 예시 - 반환값 있음:
 * <pre>{@code
 * @RabbitListener(queues = "order.queue")
 * public String handleWithResult(IntegrationEvent event) {
 *     return EventContext.execute(event, e -> {
 *         OrderCreatedEvent payload = e.getPayloadAs(OrderCreatedEvent.class);
 *         return processOrder(payload);
 *     });
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see IntegrationEvent
 * @see MdcUtils
 */
public final class EventContext {

  /** 이벤트 발행 서비스를 저장하는 MDC 키 */
  public static final String SOURCE_SERVICE = "sourceService";

  /** 이벤트 타입을 저장하는 MDC 키 */
  public static final String EVENT_TYPE = "eventType";

  /**
   * 인스턴스 생성 방지를 위한 private 생성자.
   */
  private EventContext() {
    // 유틸리티 클래스 - 인스턴스 생성 방지
  }

  // ========================================
  // 실행 메서드 - 반환값 없음 (Consumer)
  // ========================================

  /**
   * 이벤트의 traceId를 MDC에 설정하고 작업을 실행한 후 MDC를 정리한다.
   *
   * <p>이 메서드 내에서 발행되는 이벤트는 동일한 traceId를 가진다.
   *
   * @param event 수신한 IntegrationEvent
   * @param action 실행할 작업
   */
  public static void run(IntegrationEvent event, Consumer<IntegrationEvent> action) {
    try {
      setupMdc(event);
      action.accept(event);
    } finally {
      clearMdc();
    }
  }

  /**
   * 새로운 traceId로 작업을 실행한다.
   *
   * <p>스케줄러나 배치 작업 등 새로운 흐름을 시작할 때 사용한다.
   *
   * @param action 실행할 작업
   */
  public static void runWithNewTrace(Runnable action) {
    try {
      MdcUtils.setRequestId(UUID.randomUUID().toString());
      action.run();
    } finally {
      clearMdc();
    }
  }

  // ========================================
  // 실행 메서드 - 반환값 있음 (Function)
  // ========================================

  /**
   * 이벤트의 traceId를 MDC에 설정하고 작업을 실행한 후 결과를 반환하고 MDC를 정리한다.
   *
   * @param event 수신한 IntegrationEvent
   * @param action 실행할 작업
   * @param <R> 반환 타입
   * @return 작업 결과
   */
  public static <R> R execute(IntegrationEvent event, Function<IntegrationEvent, R> action) {
    try {
      setupMdc(event);
      return action.apply(event);
    } finally {
      clearMdc();
    }
  }

  /**
   * 새로운 traceId로 작업을 실행하고 결과를 반환한다.
   *
   * @param action 실행할 작업
   * @param <R> 반환 타입
   * @return 작업 결과
   */
  public static <R> R executeWithNewTrace(Supplier<R> action) {
    try {
      MdcUtils.setRequestId(UUID.randomUUID().toString());
      return action.get();
    } finally {
      clearMdc();
    }
  }

  // ========================================
  // MDC 설정/정리 메서드 (수동 관리용)
  // ========================================

  /**
   * 이벤트의 traceId를 MDC에 설정한다.
   *
   * <p>이벤트에 traceId가 없으면 새로 생성한다.
   * 추가로 sourceService와 eventType도 MDC에 설정한다.
   *
   * @param event IntegrationEvent (null 가능)
   */
  public static void setupMdc(IntegrationEvent event) {
    if (event == null) {
      MdcUtils.setRequestId(UUID.randomUUID().toString());
      return;
    }

    // traceId 설정 (있으면 사용, 없으면 새로 생성)
    String traceId = StringUtils.hasText(event.getTraceId())
        ? event.getTraceId()
        : UUID.randomUUID().toString();
    MdcUtils.setRequestId(traceId);

    // sourceService 설정 (디버깅 용도)
    if (StringUtils.hasText(event.getSourceService())) {
      MdcUtils.put(SOURCE_SERVICE, event.getSourceService());
    }

    // eventType 설정 (디버깅 용도)
    if (StringUtils.hasText(event.getEventType())) {
      MdcUtils.put(EVENT_TYPE, event.getEventType());
    }
  }

  /**
   * MDC를 정리한다.
   *
   * <p>이벤트 처리가 완료된 후 반드시 호출하여 메모리 누수 및 정보 오염을 방지해야 한다.
   */
  public static void clearMdc() {
    MDC.clear();
  }

  // ========================================
  // 조회 메서드
  // ========================================

  /**
   * 현재 MDC의 traceId를 반환한다.
   *
   * <p>이벤트 체이닝 시 현재 traceId를 확인할 때 사용한다.
   *
   * @return 현재 traceId, 없으면 null
   */
  public static String currentTraceId() {
    return MdcUtils.getRequestId();
  }

  /**
   * 현재 MDC에 traceId가 있는지 확인한다.
   *
   * @return traceId가 있으면 true
   */
  public static boolean hasTraceId() {
    return StringUtils.hasText(MdcUtils.getRequestId());
  }

  /**
   * 현재 MDC의 sourceService를 반환한다.
   *
   * @return sourceService, 없으면 null
   */
  public static String currentSourceService() {
    return MdcUtils.get(SOURCE_SERVICE);
  }

  /**
   * 현재 MDC의 eventType을 반환한다.
   *
   * @return eventType, 없으면 null
   */
  public static String currentEventType() {
    return MdcUtils.get(EVENT_TYPE);
  }
}