package io.github.tickatch.common.logging;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * MDC (Mapped Diagnostic Context) 유틸리티 클래스.
 *
 * <p>로그 추적을 위한 요청 ID 및 사용자 정보를 MDC에 저장하고 조회하는 기능을 제공한다.
 * MDC에 저장된 값은 로그 패턴에서 {@code %X{requestId}}, {@code %X{userId}} 형식으로 출력할 수 있다.
 *
 * <p>주요 기능:
 * <ul>
 *   <li>요청 ID(requestId/traceId) 관리 - 분산 시스템에서 요청 추적용</li>
 *   <li>사용자 ID(userId) 관리 - 로그에서 사용자 식별용</li>
 *   <li>범용 키-값 저장/조회/삭제</li>
 * </ul>
 *
 * <p>logback.xml 설정 예시:
 * <pre>{@code
 * <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId}] [%X{userId}] %-5level %logger{36} - %msg%n</pattern>
 * }</pre>
 *
 * <p>사용 예시:
 * <pre>{@code
 * // 요청 시작 시 (MdcFilter에서 자동 설정됨)
 * MdcUtils.setRequestId(UUID.randomUUID().toString());
 * MdcUtils.setUserId("550e8400-e29b-41d4-a716-446655440000");
 *
 * // 로그 출력 시 자동으로 requestId, userId 포함됨
 * log.info("주문 처리 시작");
 *
 * // 요청 종료 시 (MdcFilter에서 자동 정리됨)
 * MdcUtils.clear();
 * }</pre>
 *
 * <p>분산 추적 흐름:
 * <pre>
 * HTTP 요청 → MdcFilter → Controller → Service → Feign Call
 *              ↓                                      ↓
 *         MDC에 traceId 설정              FeignTraceInterceptor가
 *                                        X-Trace-Id 헤더로 전파
 * </pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see MdcFilter
 * @see MdcFilterAutoConfiguration
 */
public final class MdcUtils {

  /** 요청 ID를 저장하는 MDC 키. 분산 추적에서 traceId로도 사용됨. */
  public static final String REQUEST_ID = "requestId";

  /** 요청 ID의 별칭. REQUEST_ID와 동일한 값을 참조함. */
  public static final String TRACE_ID = REQUEST_ID;

  /** 사용자 ID를 저장하는 MDC 키. */
  public static final String USER_ID = "userId";

  /**
   * 인스턴스 생성 방지를 위한 private 생성자.
   */
  private MdcUtils() {
    // 유틸리티 클래스 - 인스턴스 생성 방지
  }

  // ========================================
  // 범용 MDC 메서드
  // ========================================

  /**
   * MDC에 키-값 쌍을 저장한다.
   *
   * <p>키 또는 값이 null인 경우 저장하지 않는다.
   *
   * @param key MDC 키
   * @param value 저장할 값
   */
  public static void put(String key, String value) {
    if (key != null && value != null) {
      MDC.put(key, value);
    }
  }

  /**
   * MDC에서 지정된 키의 값을 조회한다.
   *
   * @param key 조회할 MDC 키
   * @return 해당 키의 값, 없거나 키가 null인 경우 null
   */
  public static String get(String key) {
    return key != null ? MDC.get(key) : null;
  }

  /**
   * MDC에서 지정된 키의 값을 제거한다.
   *
   * @param key 제거할 MDC 키
   */
  public static void remove(String key) {
    if (key != null) {
      MDC.remove(key);
    }
  }

  /**
   * MDC의 모든 키-값 쌍을 클리어한다.
   *
   * <p>요청 처리가 완료된 후 반드시 호출하여 메모리 누수 및 정보 오염을 방지해야 한다.
   * 일반적으로 {@link MdcFilter}에서 자동으로 호출된다.
   */
  public static void clear() {
    MDC.clear();
  }

  // ========================================
  // 요청 ID / Trace ID 관련
  // ========================================

  /**
   * MDC에 요청 ID(traceId)를 저장한다.
   *
   * @param requestId 요청 ID 문자열 (일반적으로 UUID)
   */
  public static void setRequestId(String requestId) {
    put(REQUEST_ID, requestId);
  }

  /**
   * MDC에서 요청 ID(traceId)를 조회한다.
   *
   * @return 요청 ID 문자열, 없으면 null
   */
  public static String getRequestId() {
    return get(REQUEST_ID);
  }

  /**
   * MDC에서 요청 ID를 UUID 형식으로 조회한다.
   *
   * @return 요청 ID UUID, 없거나 형식이 올바르지 않으면 null
   */
  public static UUID getRequestUuid() {
    String requestId = get(REQUEST_ID);
    if (!StringUtils.hasText(requestId)) {
      return null;
    }
    try {
      return UUID.fromString(requestId);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * MDC에 요청 ID가 있는지 확인한다.
   *
   * @return 요청 ID가 있으면 true
   */
  public static boolean hasRequestId() {
    return StringUtils.hasText(get(REQUEST_ID));
  }

  /**
   * MDC에 요청 ID가 없으면 새로 생성하여 설정한다.
   *
   * @return 현재 또는 새로 생성된 요청 ID
   */
  public static String getOrCreateRequestId() {
    String requestId = get(REQUEST_ID);
    if (!StringUtils.hasText(requestId)) {
      requestId = UUID.randomUUID().toString();
      setRequestId(requestId);
    }
    return requestId;
  }

  // ========================================
  // 사용자 ID (User ID) 관련
  // ========================================

  /**
   * MDC에 사용자 ID를 저장한다.
   *
   * <p>userId가 null이거나 빈 문자열인 경우 저장하지 않는다.
   *
   * @param userId 사용자 ID (UUID 문자열)
   */
  public static void setUserId(String userId) {
    if (StringUtils.hasText(userId)) {
      put(USER_ID, userId);
    }
  }

  /**
   * MDC에서 사용자 ID를 조회한다.
   *
   * @return 사용자 ID (UUID 문자열), 없거나 빈 문자열이면 null
   */
  public static String getUserId() {
    String userId = get(USER_ID);
    if (!StringUtils.hasText(userId)) {
      return null;
    }
    return userId;
  }

  /**
   * MDC에 사용자 ID가 있는지 확인한다.
   *
   * @return 사용자 ID가 있으면 true
   */
  public static boolean hasUserId() {
    return StringUtils.hasText(get(USER_ID));
  }
}