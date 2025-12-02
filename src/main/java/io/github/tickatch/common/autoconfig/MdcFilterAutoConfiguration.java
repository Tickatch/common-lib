package io.github.tickatch.common.autoconfig;

import io.github.tickatch.common.logging.MdcFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * MDC 필터를 자동으로 등록하는 AutoConfiguration.
 *
 * <p>이 설정은 Servlet 기반 Web Application에서 자동으로 활성화된다.
 * {@link MdcFilter}를 가장 높은 우선순위로 등록하여 모든 요청에 대해
 * 추적 ID(traceId)와 사용자 ID(userId)를 MDC에 설정한다.
 *
 * <p>활성화 조건:
 * <ul>
 *   <li>Servlet 기반 Web Application일 것</li>
 *   <li>사용자가 직접 {@link MdcFilter} 빈을 정의하지 않았을 것</li>
 * </ul>
 *
 * <p>필터 실행 순서:
 * <pre>
 * 요청 → MdcFilter (HIGHEST_PRECEDENCE) → LoginFilter → ... → Controller
 *        ↑ 가장 먼저 실행
 * </pre>
 *
 * <p>커스텀 MdcFilter가 필요한 경우 직접 빈을 정의하면 된다:
 * <pre>{@code
 * @Configuration
 * public class CustomMdcFilterConfig {
 *
 *     @Bean
 *     public FilterRegistrationBean<MdcFilter> mdcFilterRegistration() {
 *         FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>();
 *         registration.setFilter(new CustomMdcFilter());
 *         registration.addUrlPatterns("/api/*");  // 특정 경로만 적용
 *         registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
 *         return registration;
 *     }
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see MdcFilter
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MdcFilterAutoConfiguration {

  /**
   * MDC 필터를 최우선 순위로 등록한다.
   *
   * <p>모든 URL 패턴(/**)에 대해 적용되며,
   * {@link Ordered#HIGHEST_PRECEDENCE}로 설정하여 다른 필터보다 먼저 실행된다.
   *
   * @return {@link FilterRegistrationBean} 인스턴스
   */
  @Bean
  @ConditionalOnMissingBean(MdcFilter.class)
  public FilterRegistrationBean<MdcFilter> mdcFilterRegistration() {
    FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new MdcFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.setName("mdcFilter");
    return registration;
  }
}