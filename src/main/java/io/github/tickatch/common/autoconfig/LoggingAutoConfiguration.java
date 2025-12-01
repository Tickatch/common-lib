package io.github.tickatch.common.autoconfig;

import io.github.tickatch.common.logging.LogManager;
import io.github.tickatch.common.logging.LoggingAspect;
import io.github.tickatch.common.logging.MdcFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * AOP 기반 로깅 및 MDC 필터를 자동으로 구성하는 AutoConfiguration.
 *
 * <p>이 설정은 다음 조건을 모두 충족할 때 활성화된다:
 * <ul>
 *   <li>Servlet 기반 Web Application일 것</li>
 *   <li>{@code tickatch.logging.enabled=true}이거나 설정이 없을 것 (기본 활성화)</li>
 * </ul>
 *
 * <h2>제공 기능</h2>
 * <ul>
 *   <li>{@link MdcFilter} - 요청별 requestId, userId를 MDC에 설정</li>
 *   <li>{@link LoggingAspect} - RestController 및 @LogExecution 메서드 자동 로깅</li>
 *   <li>{@link LogManager} - 일관된 로그 포맷 제공</li>
 * </ul>
 *
 * <h2>비활성화 방법</h2>
 * <pre>{@code
 * # application.yml
 * tickatch:
 *   logging:
 *     enabled: false
 * }</pre>
 *
 * <h2>로그 출력 예시</h2>
 * <pre>
 * INFO  GET /api/tickets/123 - Request ID: abc-123, User ID: 42, Method: TicketController.getTicket, Params: {id: 123}
 * INFO  GET /api/tickets/123 - Request ID: abc-123, User ID: 42, Method: TicketController.getTicket, Return: {"id":123}
 * </pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see LoggingAspect
 * @see MdcFilter
 * @see LogManager
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        prefix = "tickatch.logging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LoggingAutoConfiguration {

    /**
     * {@link LogManager} 빈을 등록한다.
     *
     * <p>로그 메시지 포맷팅을 담당하며, {@link LoggingAspect}에서 사용된다.
     *
     * @return {@link LogManager} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public LogManager logManager() {
        return new LogManager();
    }

    /**
     * {@link LoggingAspect} 빈을 등록한다.
     *
     * <p>RestController 및 {@code @LogExecution} 어노테이션이 적용된 메서드를
     * AOP로 감싸 진입/종료 로그를 자동으로 기록한다.
     *
     * @param logManager 로그 포맷팅을 위한 LogManager
     * @return {@link LoggingAspect} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public LoggingAspect loggingAspect(LogManager logManager) {
        return new LoggingAspect(logManager);
    }

    /**
     * {@link MdcFilter} 빈을 등록한다.
     *
     * <p>각 HTTP 요청에 대해 고유한 requestId를 생성하고,
     * 헤더에서 userId를 추출하여 MDC에 저장한다.
     *
     * @return {@link MdcFilter} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public MdcFilter mdcFilter() {
        return new MdcFilter();
    }

    /**
     * {@link MdcFilter}를 서블릿 필터로 등록한다.
     *
     * <p>다른 필터보다 먼저 실행되도록 높은 우선순위를 부여하여
     * 요청 처리 시작 시점에 MDC가 설정되도록 한다.
     *
     * @param mdcFilter MdcFilter 빈
     * @return 필터 등록 빈
     */
    @Bean
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration(MdcFilter mdcFilter) {
        FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>(mdcFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }
}