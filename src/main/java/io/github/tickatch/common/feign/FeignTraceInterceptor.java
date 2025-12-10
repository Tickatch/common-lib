package io.github.tickatch.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.tickatch.common.logging.MdcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feign 요청 시 MDC(Mapped Diagnostic Context)에 저장된 추적 정보를
 * HTTP 헤더로 자동 전파하는 인터셉터.
 *
 * <p>MSA 환경에서는 서비스 간 호출이 연속되기 때문에,
 * 동일한 요청 흐름을 추적하려면 traceId / userId를 다음 서비스로 전달해야 한다.
 * 이 클래스는 FeignClient가 외부 서비스로 요청을 보낼 때
 * MDC에 담긴 값을 다음 헤더로 전달한다:
 *
 * <ul>
 *   <li>X-Trace-Id — 요청 단위 추적 ID (MdcFilter에서 설정)</li>
 *   <li>X-User-Id — 인증된 사용자 ID (Gateway 또는 MdcFilter에서 설정)</li>
 * </ul>
 *
 * <p>동작 흐름:
 * <pre>
 * [Client] → [Gateway] → [Service A] → (Feign) → [Service B]
 *
 * - MdcFilter: 들어오는 요청에서 traceId/userId를 MDC에 저장
 * - FeignTraceInterceptor: 나가는 Feign 요청에 traceId/userId를 헤더로 추가
 * - Service B: MdcFilter가 헤더 정보를 다시 MDC에 주입
 * </pre>
 *
 * <p>이 인터셉터가 없으면 서비스 A에서 B로 요청 시 traceId가 전달되지 않으므로
 * 서비스 간 로그 체이닝(Log Tracing)이 끊기게 된다.
 * 따라서 MSA 구조에서 분산 추적을 유지하려면 필수 구성 요소이다.
 *
 * @author Tickatch
 * @since 0.0.1
 * @see io.github.tickatch.common.logging.MdcFilter
 * @see io.github.tickatch.common.logging.MdcUtils
 * @see FeignHeaderAutoConfiguration
 */
public class FeignTraceInterceptor implements RequestInterceptor {
  private static final Logger log = LoggerFactory.getLogger(FeignTraceInterceptor.class);

  @Override
  public void apply(RequestTemplate template) {
    String traceId = MdcUtils.getRequestId();
    String userId = MdcUtils.getUserId();

    if (traceId != null) {
      template.header("X-Trace-Id", traceId);
      log.debug("Feign request - propagating X-Trace-Id: {}", traceId);
    }

    if (userId != null) {
      template.header("X-User-Id", userId);
      log.debug("Feign request - propagating X-User-Id: {}", userId);
    }
  }
}