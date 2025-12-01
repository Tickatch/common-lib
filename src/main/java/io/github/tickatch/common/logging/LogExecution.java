package io.github.tickatch.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 시점의 로깅을 처리하기 위한 커스텀 애노테이션.
 *
 * <p>이 애노테이션이 적용된 메서드는 {@link LoggingAspect}에 의해 AOP로 감싸져
 * 메서드 시작과 종료 시점에 자동으로 로그가 기록된다.
 *
 * <p>기록되는 정보:
 * <ul>
 *   <li>메서드 진입 시: 클래스명, 메서드명, 파라미터 정보</li>
 *   <li>메서드 종료 시: 클래스명, 메서드명, 반환값 (JSON 형식)</li>
 * </ul>
 *
 * <p>사용 예시:
 * <pre>{@code
 * @Service
 * public class PaymentService {
 *
 *     @LogExecution
 *     public PaymentResult processPayment(PaymentRequest request) {
 *         // 비즈니스 로직
 *         return result;
 *     }
 * }
 * }</pre>
 *
 * <p>출력 예시:
 * <pre>
 * INFO  Request ID: abc-123, User ID: 42, Method: PaymentService.processPayment, Params: {request: PaymentRequest(...)}
 * INFO  Request ID: abc-123, User ID: 42, Method: PaymentService.processPayment, Return: {"status":"SUCCESS",...}
 * </pre>
 *
 * <p>주의사항:
 * <ul>
 *   <li>민감한 정보(비밀번호, 카드번호 등)를 포함하는 파라미터나 반환값이 있는 메서드에는 사용 주의</li>
 *   <li>빈번하게 호출되는 메서드에 적용하면 로그 양이 증가하여 성능에 영향을 줄 수 있음</li>
 * </ul>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see LoggingAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
}