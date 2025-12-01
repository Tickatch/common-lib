package io.github.tickatch.common.autoconfig;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * {@link RestControllerAdvice} 빈이 없을 때만 조건을 만족하는 Condition 클래스.
 *
 * <p>이 조건은 스프링 컨텍스트 내에 {@link RestControllerAdvice} 어노테이션이 붙은 빈이
 * 존재하지 않을 때만 {@code true}를 반환한다.
 *
 * <p>즉, 사용자가 직접 전역 예외 처리기(ControllerAdvice)를 구현했을 경우
 * 라이브러리가 제공하는 기본 예외 처리기 AutoConfiguration은 비활성화된다.
 *
 * <p>Spring Boot의 "사용자가 명시적으로 제공한 빈이 있다면 기본 설정을 끈다"는 철학을 따른다.
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @AutoConfiguration
 * @Conditional(NoRestControllerAdviceCondition.class)
 * public class ExceptionHandlerAutoConfiguration {
 *     // ...
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 */
public class NoRestControllerAdviceCondition implements Condition {

    /**
     * AutoConfiguration 매칭 여부를 판단한다.
     *
     * @param context 현재 스프링 컨텍스트에 접근할 수 있는 Context
     * @param metadata 어노테이션 메타 정보 (사용되지 않음)
     * @return {@code true} - {@link RestControllerAdvice} 빈이 없는 경우,
     *         {@code false} - 하나라도 존재하면 AutoConfiguration 비활성화
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (context.getBeanFactory() == null) {
            return false;
        }

        String[] beanNames = context.getBeanFactory()
                .getBeanNamesForAnnotation(RestControllerAdvice.class);

        return beanNames.length == 0;
    }
}