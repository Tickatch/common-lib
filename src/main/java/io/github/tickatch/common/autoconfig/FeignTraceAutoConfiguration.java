package io.github.tickatch.common.autoconfig;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.tickatch.common.logging.MdcFilter;
import io.github.tickatch.common.logging.MdcUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Feign Client를 통한 서비스 간 통신 시 추적 정보를 자동 전파하는 AutoConfiguration.
 *
 * <p>이 설정은 다음 조건을 충족할 때 자동으로 활성화된다:
 * <ul>
 *   <li>{@link RequestInterceptor} 클래스가 클래스패스에 존재할 것 (spring-cloud-openfeign 의존성)</li>
 * </ul>
 *
 * <p>전파하는 헤더:
 * <ul>
 *   <li>{@code X-Trace-Id} — 분산 추적 ID (MDC의 requestId)</li>
 *   <li>{@code X-User-Id} — 사용자 ID (MDC의 userId)</li>
 * </ul>
 *
 * <p>분산 추적 흐름:
 * <pre>
 * [Service A]                          [Service B]
 *     │                                     │
 *     │ MDC: traceId = "abc-123"            │
 *     │      userId = "user-456"            │
 *     │                                     │
 *     └─── Feign Call ──────────────────────┘
 *          Headers:
 *            X-Trace-Id: abc-123
 *            X-User-Id: user-456
 *                                           │
 *                                           ▼
 *                                     MdcFilter에서
 *                                     X-Trace-Id 수신
 *                                     → MDC에 저장
 * </pre>
 *
 * <p>이 AutoConfiguration을 사용하면 별도 설정 없이 Feign 호출 시
 * 자동으로 traceId와 userId가 전파된다.
 *
 * @author Tickatch
 * @since 0.0.1
 * @see MdcFilter
 * @see MdcUtils
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignTraceAutoConfiguration {

  /**
   * MDC 컨텍스트를 Feign 요청 헤더로 전파하는 인터셉터.
   *
   * <p>모든 Feign 요청에 X-Trace-Id와 X-User-Id 헤더를 자동으로 추가한다.
   * MDC에 해당 값이 없으면 헤더를 추가하지 않는다.
   *
   * @return {@link RequestInterceptor} 인스턴스
   */
  @Bean
  public RequestInterceptor mdcPropagationInterceptor() {
    return new MdcPropagationInterceptor();
  }

  /**
   * MDC 컨텍스트를 Feign 요청 헤더로 전파하는 RequestInterceptor 구현.
   */
  static class MdcPropagationInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
      // Trace ID 전파
      propagateIfPresent(template, MdcUtils.REQUEST_ID, MdcFilter.HEADER_TRACE_ID);

      // User ID 전파
      propagateIfPresent(template, MdcUtils.USER_ID, MdcFilter.HEADER_USER_ID);
    }

    /**
     * MDC에 값이 있으면 요청 헤더에 추가한다.
     *
     * @param template Feign 요청 템플릿
     * @param mdcKey MDC 키
     * @param headerName HTTP 헤더 이름
     */
    private void propagateIfPresent(RequestTemplate template, String mdcKey, String headerName) {
      String value = MdcUtils.get(mdcKey);
      if (StringUtils.hasText(value)) {
        template.header(headerName, value);
      }
    }
  }
}