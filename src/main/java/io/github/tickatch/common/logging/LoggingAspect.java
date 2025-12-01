package io.github.tickatch.common.logging;

import io.github.tickatch.common.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;

/**
 * 애플리케이션 전반의 컨트롤러 및 메서드 실행을 AOP로 로깅하는 Aspect 클래스.
 *
 * <p>두 가지 로깅 포인트컷을 제공한다:
 * <ul>
 *   <li>RestController 내의 모든 요청에 대해 HTTP 메서드, URI, 메서드명, 파라미터, 응답 결과를 로깅</li>
 *   <li>{@code @LogExecution} 애노테이션이 적용된 메서드의 진입/종료 로깅</li>
 * </ul>
 *
 * <p>사용 방법 - 각 서비스에서 빈으로 등록:
 * <pre>{@code
 * @Configuration
 * public class LoggingConfig {
 *
 *     @Bean
 *     public LogManager logManager() {
 *         return new LogManager();
 *     }
 *
 *     @Bean
 *     public LoggingAspect loggingAspect(LogManager logManager) {
 *         return new LoggingAspect(logManager);
 *     }
 * }
 * }</pre>
 *
 * <p>로그 출력 예시:
 * <pre>
 * INFO  GET /api/tickets/123 - Request ID: abc-123, User ID: 42, Method: TicketController.getTicket, Params: {id: 123}
 * INFO  GET /api/tickets/123 - Request ID: abc-123, User ID: 42, Method: TicketController.getTicket, Return: {"id":123,"name":"콘서트"}
 * </pre>
 *
 * <p>주의사항:
 * <ul>
 *   <li>민감한 정보가 포함된 응답은 로그에 노출될 수 있으므로 주의</li>
 *   <li>빈번한 요청이 있는 엔드포인트에서는 로그 양이 증가할 수 있음</li>
 *   <li>각 서비스에서 필요에 따라 로깅 범위를 조절할 수 있음</li>
 * </ul>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see LogExecution
 * @see LogManager
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {

    private static final String NOT_APPLICABLE = "N/A";

    private final LogManager logManager;

    /**
     * RestController 범위 내의 모든 메서드 실행 시점에 대해 진입과 종료를 로깅한다.
     *
     * @param pjp 호출 대상 JoinPoint
     * @return 실제 메서드 실행 결과
     * @throws Throwable 내부 메서드 예외 발생 시 전달
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logController(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = getCurrentHttpRequest();

        String httpMethod = request != null ? request.getMethod() : NOT_APPLICABLE;
        String requestUri = request != null ? extractPath(request.getRequestURL().toString()) : NOT_APPLICABLE;
        String methodInfo = extractMethodInfo(pjp);
        String logMessage = buildLogMessage((MethodSignature) pjp.getSignature(), pjp.getArgs());

        // 메서드 진입 로그
        logManager.logControllerEntry(httpMethod, requestUri, methodInfo, logMessage);

        Object result = pjp.proceed();

        // 메서드 종료 로그
        String resultJson = toJsonSafe(result);
        logManager.logControllerExit(httpMethod, requestUri, methodInfo, resultJson);

        return result;
    }

    /**
     * {@code @LogExecution} 애노테이션이 적용된 메서드의 진입과 종료를 로깅한다.
     *
     * @param pjp 호출 대상 JoinPoint
     * @return 실제 메서드 실행 결과
     * @throws Throwable 내부 메서드 예외 발생 시 전달
     */
    @Around("@annotation(io.github.tickatch.common.logging.LogExecution)")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        String methodInfo = extractMethodInfo(pjp);
        String logMessage = buildLogMessage((MethodSignature) pjp.getSignature(), pjp.getArgs());

        // 메서드 진입 로그
        logManager.logMethodEntry(methodInfo, logMessage);

        Object result = pjp.proceed();

        // 메서드 종료 로그
        String resultJson = toJsonSafe(result);
        logManager.logMethodExit(methodInfo, resultJson);

        return result;
    }

    /**
     * 현재 HTTP 요청 객체를 조회한다.
     *
     * @return HttpServletRequest 현재 요청, 없으면 null
     */
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 전체 클래스명에서 단순 클래스명만 추출한다.
     *
     * @param fullClassName 전체 클래스명 (패키지 포함)
     * @return 단순 클래스명
     */
    private String extractSimpleClassName(String fullClassName) {
        int lastDotIndex = fullClassName.lastIndexOf(".");
        return lastDotIndex != -1 ? fullClassName.substring(lastDotIndex + 1) : fullClassName;
    }

    /**
     * 전체 URL 문자열에서 경로(path)를 추출한다.
     *
     * @param fullUrl 전체 URL
     * @return URL 경로, 추출 실패 시 원본 문자열
     */
    private String extractPath(String fullUrl) {
        try {
            URI uri = new URI(fullUrl);
            return uri.getPath();
        } catch (Exception e) {
            log.warn("URL 경로 추출 실패: {}", fullUrl);
            return fullUrl;
        }
    }

    /**
     * ProceedingJoinPoint로부터 클래스명과 메서드명을 결합한 식별 문자열을 생성한다.
     *
     * @param joinPoint 호출 대상 JoinPoint
     * @return ClassName.methodName 형식의 메서드 정보
     */
    private String extractMethodInfo(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return extractSimpleClassName(signature.getDeclaringTypeName()) + "." + signature.getName();
    }

    /**
     * 메서드 시그니처와 인자를 기반으로 파라미터 로깅용 문자열을 생성한다.
     *
     * @param signature 메서드 시그니처
     * @param args 메서드 호출 인자 배열
     * @return ", Params: {name1: value1, ...}" 형식의 파라미터 정보 (인자가 없으면 빈 문자열)
     */
    private String buildLogMessage(MethodSignature signature, Object[] args) {
        String[] parameterNames = signature.getParameterNames();

        if (parameterNames == null || parameterNames.length == 0) {
            return "";
        }

        StringBuilder logMessage = new StringBuilder(", Params: {");
        for (int i = 0; i < parameterNames.length; i++) {
            logMessage.append(parameterNames[i]).append(": ").append(args[i]);
            if (i < parameterNames.length - 1) {
                logMessage.append(", ");
            }
        }
        logMessage.append("}");

        return logMessage.toString();
    }

    /**
     * 객체를 JSON 문자열로 안전하게 변환한다.
     *
     * <p>변환 실패 시 클래스명을 반환한다.
     *
     * @param object 변환할 객체
     * @return JSON 문자열 또는 클래스명
     */
    private String toJsonSafe(Object object) {
        if (object == null) {
            return "null";
        }

        try {
            return JsonUtils.toJson(object);
        } catch (Exception e) {
            return object.getClass().getName();
        }
    }
}