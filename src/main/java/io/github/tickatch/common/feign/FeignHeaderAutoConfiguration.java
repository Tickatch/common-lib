package io.github.tickatch.common.feign;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Feign 클라이언트의 MDC 추적 정보 전파를 자동 설정하는 AutoConfiguration.
 *
 * <p>이 설정은 common-lib를 의존하는 모든 서비스에서 자동으로 활성화되며,
 * Feign 관련 클래스가 classpath에 있을 때만 동작한다.
 *
 * <p>제공되는 Bean:
 * <ul>
 *   <li>{@link FeignTraceInterceptor} - MDC 값을 HTTP 헤더로 전파</li>
 * </ul>
 *
 * <p>활성화 조건:
 * <ul>
 *   <li>Feign 의존성이 프로젝트에 포함되어 있어야 함 (ConditionalOnClass)</li>
 *   <li>Spring Boot AutoConfiguration 메커니즘에 의해 자동 로드됨</li>
 * </ul>
 *
 * <p>등록 위치: {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 *
 * @author Tickatch
 * @since 0.0.1
 * @see FeignTraceInterceptor
 * @see io.github.tickatch.common.logging.MdcFilter
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignHeaderAutoConfiguration {

  @Bean
  public RequestInterceptor feignTraceInterceptor() {
    return new FeignTraceInterceptor();
  }
}