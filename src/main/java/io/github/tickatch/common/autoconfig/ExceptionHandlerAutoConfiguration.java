package io.github.tickatch.common.autoconfig;

import io.github.tickatch.common.error.GlobalExceptionHandler;
import io.github.tickatch.common.message.DefaultMessageResolver;
import io.github.tickatch.common.message.MessageResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * 전역 예외 처리기를 자동으로 구성하는 AutoConfiguration.
 *
 * <p>이 설정은 다음 조건을 모두 충족할 때 활성화된다:
 * <ul>
 *   <li>Servlet 기반 Web Application일 것</li>
 *   <li>{@code tickatch.exception.enabled=true}이거나 설정이 없을 것 (기본 활성화)</li>
 *   <li>사용자가 {@code @RestControllerAdvice} 빈을 직접 정의하지 않았을 것</li>
 * </ul>
 *
 * <h2>제공 기능</h2>
 * <ul>
 *   <li>{@link GlobalExceptionHandler} - 전역 예외 처리</li>
 *   <li>{@link MessageResolver} - 에러 메시지 해석</li>
 * </ul>
 *
 * <h2>비활성화 방법</h2>
 * <pre>{@code
 * # application.yml
 * tickatch:
 *   exception:
 *     enabled: false
 * }</pre>
 *
 * <h2>커스텀 예외 처리기 사용</h2>
 * <p>{@code @RestControllerAdvice}가 붙은 빈을 직접 정의하면 이 AutoConfiguration은 비활성화된다:
 * <pre>{@code
 * @RestControllerAdvice
 * public class CustomExceptionHandler extends GlobalExceptionHandler {
 *     // 커스텀 예외 핸들러 추가
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see GlobalExceptionHandler
 * @see MessageResolver
 */
@AutoConfiguration
@Conditional(NoRestControllerAdviceCondition.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        prefix = "tickatch.exception",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ExceptionHandlerAutoConfiguration {

    /**
     * 기본 {@link MessageResolver} 빈을 등록한다.
     *
     * <p>Spring의 {@link MessageSource}를 사용하여 에러 코드를 메시지로 변환한다.
     * 사용자가 직접 {@link MessageResolver} 빈을 정의한 경우 이 빈은 생성되지 않는다.
     *
     * @param messageSource Spring MessageSource (messages.properties 등)
     * @return {@link DefaultMessageResolver} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean(MessageResolver.class)
    public MessageResolver messageResolver(MessageSource messageSource) {
        return new DefaultMessageResolver(messageSource);
    }

    /**
     * 기본 {@link GlobalExceptionHandler} 빈을 등록한다.
     *
     * <p>사용자가 직접 {@link GlobalExceptionHandler} 빈을 정의한 경우 이 빈은 생성되지 않는다.
     *
     * @param messageResolver 메시지 해석기
     * @return {@link GlobalExceptionHandler} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler(MessageResolver messageResolver) {
        return new GlobalExceptionHandler(messageResolver);
    }
}