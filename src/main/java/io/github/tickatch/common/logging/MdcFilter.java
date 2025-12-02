package io.github.tickatch.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청 단위로 MDC(Mapped Diagnostic Context)를 초기화하고 관리하는 필터.
 *
 * <p>각 HTTP 요청마다 고유한 traceId를 관리하고, 헤더에서 사용자 ID를 추출하여 MDC에 저장한다.
 * 이를 통해 분산 시스템에서 요청 추적성(Traceability)을 향상시킬 수 있다.
 *
 * <p>traceId 결정 로직:
 * <ol>
 *   <li>{@code X-Trace-Id} 헤더가 있으면 해당 값 사용 (Feign 통신 등에서 전달됨)</li>
 *   <li>헤더가 없으면 새로운 UUID 생성</li>
 * </ol>
 *
 * <p>{@link OncePerRequestFilter}를 상속하여 요청당 한 번만 실행되며,
 * 요청 처리가 완료된 후에는 MDC를 반드시 초기화하여 메모리 누수 및 정보 오염을 방지한다.
 *
 * <p>처리하는 헤더:
 * <ul>
 *   <li>{@code X-Trace-Id} — 분산 추적 ID (상위 서비스에서 전달, 없으면 새로 생성)</li>
 *   <li>{@code X-User-Id} — 사용자 ID (API Gateway에서 전달, UUID 문자열)</li>
 * </ul>
 *
 * <p>응답 헤더:
 * <ul>
 *   <li>{@code X-Trace-Id} — 현재 요청의 traceId (프론트엔드/디버깅용)</li>
 * </ul>
 *
 * <p>로그 출력 예시 (logback.xml 패턴 설정 시):
 * <pre>
 * 2025-01-15 10:30:00.123 [http-nio-8080-exec-1] [abc-123-def] [user-456] INFO  c.e.TicketController - 티켓 조회
 * </pre>
 *
 * <p>분산 추적 흐름:
 * <pre>
 * [Client] → [API Gateway] → [Service A] → [Service B]
 *              traceId 생성    traceId 수신   traceId 수신
 *              (abc-123)      (abc-123)     (abc-123)
 *                              ↓              ↓
 *                         Feign에서 X-Trace-Id 헤더로 전파
 * </pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see MdcUtils
 * @see MdcFilterAutoConfiguration
 */
public class MdcFilter extends OncePerRequestFilter {

  /** 분산 추적 ID를 전달하는 HTTP 헤더 이름. */
  public static final String HEADER_TRACE_ID = "X-Trace-Id";

  /** 사용자 ID를 전달하는 HTTP 헤더 이름. */
  public static final String HEADER_USER_ID = "X-User-Id";

  /**
   * 요청마다 traceId와 userId를 MDC에 저장하고, 요청 처리가 끝나면 MDC를 초기화한다.
   *
   * <p>응답 헤더에도 traceId를 포함시켜 클라이언트에서 추적할 수 있도록 한다.
   *
   * @param request HTTP 요청
   * @param response HTTP 응답
   * @param filterChain 필터 체인
   * @throws ServletException 필터 처리 중 서블릿 예외 발생 시
   * @throws IOException 필터 처리 중 I/O 예외 발생 시
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      // 1. Trace ID: 헤더에 있으면 사용, 없으면 새로 생성
      String traceId = extractTraceId(request);
      MdcUtils.setRequestId(traceId);

      // 2. User ID: 헤더에서 추출
      String userId = extractUserId(request);
      if (userId != null) {
        MdcUtils.setUserId(userId);
      }

      // 3. 응답 헤더에 traceId 포함 (프론트엔드/디버깅용)
      response.setHeader(HEADER_TRACE_ID, traceId);

      filterChain.doFilter(request, response);
    } finally {
      // 메모리 누수 방지를 위해 MDC 클리어
      MDC.clear();
    }
  }

  /**
   * HTTP 헤더에서 Trace ID를 추출한다.
   *
   * <p>X-Trace-Id 헤더가 있으면 해당 값을 사용하고,
   * 없으면 새로운 UUID를 생성한다.
   *
   * @param request HTTP 요청
   * @return Trace ID 문자열
   */
  private String extractTraceId(HttpServletRequest request) {
    String traceIdHeader = request.getHeader(HEADER_TRACE_ID);

    if (StringUtils.hasText(traceIdHeader)) {
      return traceIdHeader;
    }

    // 헤더가 없으면 새로 생성
    return UUID.randomUUID().toString();
  }

  /**
   * HTTP 헤더에서 사용자 ID를 추출한다.
   *
   * @param request HTTP 요청
   * @return 사용자 ID (UUID 문자열), 헤더가 없거나 빈 문자열이면 null
   */
  private String extractUserId(HttpServletRequest request) {
    String userIdHeader = request.getHeader(HEADER_USER_ID);

    if (!StringUtils.hasText(userIdHeader)) {
      return null;
    }

    return userIdHeader;
  }
}