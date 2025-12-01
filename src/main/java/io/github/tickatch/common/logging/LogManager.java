package io.github.tickatch.common.logging;

import lombok.extern.slf4j.Slf4j;

/**
 * 로깅 기능을 중앙에서 관리하는 클래스.
 *
 * <p>컨트롤러 요청 진입/종료 및 {@code @LogExecution} 애노테이션이 적용된 메서드의
 * 로깅을 일관된 형식으로 처리한다. MDC에 저장된 요청 ID 및 사용자 ID를 함께 출력한다.
 *
 * <p>로그 형식:
 * <ul>
 *   <li>컨트롤러 진입: {@code GET /api/tickets - Request ID: xxx, User ID: xxx, Method: xxx, Params: {...}}</li>
 *   <li>컨트롤러 종료: {@code GET /api/tickets - Request ID: xxx, User ID: xxx, Method: xxx, Return: {...}}</li>
 *   <li>메서드 진입: {@code Request ID: xxx, User ID: xxx, Method: xxx, Params: {...}}</li>
 *   <li>메서드 종료: {@code Request ID: xxx, User ID: xxx, Method: xxx, Return: {...}}</li>
 * </ul>
 *
 * <p>사용 예시:
 * <pre>{@code
 * @Aspect
 * @Component
 * @RequiredArgsConstructor
 * public class CustomLoggingAspect {
 *
 *     private final LogManager logManager;
 *
 *     @Around("...")
 *     public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
 *         logManager.logMethodEntry("MyService.doSomething", "Params: {...}");
 *         Object result = pjp.proceed();
 *         logManager.logMethodExit("MyService.doSomething", "{...}");
 *         return result;
 *     }
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see LoggingAspect
 * @see MdcUtils
 */
@Slf4j
public class LogManager {

    /**
     * 컨트롤러 진입 시점에 로그를 기록한다.
     *
     * @param httpMethod HTTP 메서드 (GET, POST 등)
     * @param requestUri 요청 URI
     * @param methodInfo 호출된 메서드 정보 (ClassName.methodName)
     * @param logMessage 추가 로그 메시지 (파라미터 정보 등)
     */
    public void logControllerEntry(
            String httpMethod,
            String requestUri,
            String methodInfo,
            String logMessage) {

        log.info("{} {} - {}{}", httpMethod, requestUri, formatCoreMessage(methodInfo), logMessage);
    }

    /**
     * 컨트롤러 종료 시점에 로그를 기록한다.
     *
     * @param httpMethod HTTP 메서드 (GET, POST 등)
     * @param requestUri 요청 URI
     * @param methodInfo 호출된 메서드 정보 (ClassName.methodName)
     * @param resultJson 반환된 결과 (JSON 또는 클래스명)
     */
    public void logControllerExit(
            String httpMethod,
            String requestUri,
            String methodInfo,
            String resultJson) {

        log.info("{} {} - {}, Return: {}", httpMethod, requestUri, formatCoreMessage(methodInfo), resultJson);
    }

    /**
     * {@code @LogExecution} 애노테이션이 적용된 메서드 진입 시점에 로그를 기록한다.
     *
     * @param methodInfo 호출된 메서드 정보 (ClassName.methodName)
     * @param logMessage 추가 로그 메시지 (파라미터 정보 등)
     */
    public void logMethodEntry(String methodInfo, String logMessage) {
        log.info("{}{}", formatCoreMessage(methodInfo), logMessage);
    }

    /**
     * {@code @LogExecution} 애노테이션이 적용된 메서드 종료 시점에 로그를 기록한다.
     *
     * @param methodInfo 호출된 메서드 정보 (ClassName.methodName)
     * @param resultJson 반환된 결과 (JSON 또는 클래스명)
     */
    public void logMethodExit(String methodInfo, String resultJson) {
        log.info("{}, Return: {}", formatCoreMessage(methodInfo), resultJson);
    }

    /**
     * 예외 발생 시 MDC에 저장된 요청 정보와 함께 ERROR 레벨로 로그를 기록한다.
     *
     * @param e 발생한 예외
     */
    public void logException(Exception e) {
        log.error("Request ID: {}, User ID: {}",
                MdcUtils.getRequestId(),
                MdcUtils.getUserId(),
                e);
    }

    /**
     * 예외 발생 시 추가 컨텍스트와 함께 ERROR 레벨로 로그를 기록한다.
     *
     * @param context 추가 컨텍스트 정보 (예: 메서드명, 작업 설명)
     * @param e 발생한 예외
     */
    public void logException(String context, Exception e) {
        log.error("Request ID: {}, User ID: {}, Context: {}",
                MdcUtils.getRequestId(),
                MdcUtils.getUserId(),
                context,
                e);
    }

    /**
     * 공통 로그 메시지 형식을 생성한다.
     *
     * @param methodInfo 메서드 정보
     * @return 포맷팅된 메시지
     */
    private String formatCoreMessage(String methodInfo) {
        return String.format("Request ID: %s, User ID: %s, Method: %s",
                MdcUtils.getRequestId(),
                MdcUtils.getUserId(),
                methodInfo);
    }
}