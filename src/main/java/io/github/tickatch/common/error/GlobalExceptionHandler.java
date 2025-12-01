package io.github.tickatch.common.error;

import io.github.tickatch.common.api.ApiResponse;
import io.github.tickatch.common.message.MessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 전역 예외 핸들러.
 *
 * <p>모든 예외를 일관된 형식으로 처리하며, {@link MessageResolver}를 통해 동적 메시지를 생성한다.
 *
 * <p>처리하는 예외 유형:
 * <ul>
 *   <li><b>비즈니스 예외</b> - {@link BusinessException}</li>
 *   <li><b>검증 예외</b> - {@link MethodArgumentNotValidException}, {@link BindException},
 *       {@link HandlerMethodValidationException} (Spring 6+)</li>
 *   <li><b>요청 예외</b> - JSON 파싱 실패, 파라미터 누락, 타입 불일치 등</li>
 *   <li><b>인증/인가 예외</b> - {@link AccessDeniedException}</li>
 *   <li><b>리소스 예외</b> - 엔드포인트 없음, 리소스 없음</li>
 *   <li><b>기타 예외</b> - 미처리 예외는 500 에러로 처리</li>
 * </ul>
 *
 * <p>각 서비스에서 이 핸들러를 상속하여 도메인별 예외 처리 추가 가능:
 * <pre>{@code
 * @RestControllerAdvice
 * public class TicketExceptionHandler extends GlobalExceptionHandler {
 *
 *     public TicketExceptionHandler(MessageResolver messageResolver) {
 *         super(messageResolver);
 *     }
 *
 *     @ExceptionHandler(TicketNotFoundException.class)
 *     public ResponseEntity<ApiResponse<Void>> handleTicketNotFound(
 *             HttpServletRequest request, TicketNotFoundException e) {
 *         // 커스텀 처리
 *     }
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageResolver messageResolver;

    // ========================================
    // 비즈니스 예외 처리
    // ========================================

    /**
     * {@link BusinessException} 처리.
     *
     * <p>커스텀 비즈니스 예외는 {@link ErrorCode} 및 errorArgs를 기반으로 메시지를 생성한다.
     *
     * @param request HTTP 요청
     * @param e 발생한 비즈니스 예외
     * @return 에러 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            HttpServletRequest request,
            BusinessException e) {

        String code = e.getCode();
        String message = messageResolver.resolve(code, e.getErrorArgs());

        log.warn("비즈니스 예외: {} - {} (path: {})", code, message, request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                e.getStatus(),
                request.getRequestURI()
        );

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    // ========================================
    // Validation 예외 처리
    // ========================================

    /**
     * @Valid 검증 실패 처리.
     *
     * <p>@RequestBody와 @Valid 조합 사용 시 발생하는 검증 실패를 처리한다.
     *
     * @param request HTTP 요청
     * @param e 검증 실패 예외
     * @return 필드 에러 목록을 포함한 에러 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            HttpServletRequest request,
            MethodArgumentNotValidException e) {

        List<FieldError> fieldErrors = ValidationErrorParser.from(e);

        String code = GlobalErrorCode.VALIDATION_ERROR.getCode();
        String message = messageResolver.resolve(code);

        log.warn("검증 실패: {} (path: {})", fieldErrors, request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 바인딩 예외 처리.
     *
     * <p>@ModelAttribute 사용 시 발생하는 검증 실패를 처리한다.
     *
     * @param request HTTP 요청
     * @param e 바인딩 예외
     * @return 필드 에러 목록을 포함한 에러 응답
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            HttpServletRequest request,
            BindException e) {

        List<FieldError> fieldErrors = ValidationErrorParser.from(e);

        String code = GlobalErrorCode.VALIDATION_ERROR.getCode();
        String message = messageResolver.resolve(code);

        log.warn("바인딩 실패: {} (path: {})", fieldErrors, request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 메서드 파라미터 검증 실패 처리 (Spring 6+).
     *
     * <p>@RequestParam, @PathVariable, @RequestHeader 등에 적용된
     * Bean Validation 제약 조건 위반 시 발생하는 예외를 처리한다.
     *
     * <p>예시:
     * <pre>{@code
     * @GetMapping("/users/{id}")
     * public User getUser(@PathVariable @Min(1) Long id) { ... }
     * // id가 0 이하일 경우 이 핸들러가 처리
     * }</pre>
     *
     * @param request HTTP 요청
     * @param e 메서드 파라미터 검증 실패 예외
     * @return 필드 에러 목록을 포함한 에러 응답
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            HttpServletRequest request,
            HandlerMethodValidationException e) {

        List<FieldError> fieldErrors = ValidationErrorParser.from(e);

        String code = GlobalErrorCode.VALIDATION_ERROR.getCode();
        String message = messageResolver.resolve(code);

        log.warn("파라미터 검증 실패: {} (path: {})", fieldErrors, request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    // ========================================
    // 요청 관련 예외 처리
    // ========================================

    /**
     * JSON 파싱 실패 처리.
     *
     * <p>요청 본문의 JSON 형식이 잘못되었거나 필수 필드가 누락된 경우 발생한다.
     *
     * @param request HTTP 요청
     * @param e JSON 파싱 예외
     * @return 에러 응답
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpServletRequest request,
            HttpMessageNotReadableException e) {

        String code = GlobalErrorCode.INVALID_JSON.getCode();
        String message = messageResolver.resolve(code);

        log.warn("JSON 파싱 실패: {} (path: {})", e.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 필수 파라미터 누락 처리.
     *
     * <p>@RequestParam(required=true)로 지정된 파라미터가 누락된 경우 발생한다.
     *
     * @param request HTTP 요청
     * @param e 파라미터 누락 예외
     * @return 에러 응답
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            HttpServletRequest request,
            MissingServletRequestParameterException e) {

        String code = GlobalErrorCode.MISSING_PARAMETER.getCode();
        String message = messageResolver.resolve(code, e.getParameterName());

        log.warn("필수 파라미터 누락: {} (path: {})", e.getParameterName(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 파라미터 타입 불일치 처리.
     *
     * <p>예: "/users?id=abc" 요청 시 id(Long)가 "abc"로 변환될 수 없어 발생한다.
     *
     * @param request HTTP 요청
     * @param e 타입 불일치 예외
     * @return 에러 응답
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            HttpServletRequest request,
            MethodArgumentTypeMismatchException e) {

        String code = GlobalErrorCode.TYPE_MISMATCH.getCode();
        String message = messageResolver.resolve(code, e.getName());

        log.warn("파라미터 타입 불일치: {} (path: {})", e.getName(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 지원하지 않는 HTTP 메서드 처리.
     *
     * <p>컨트롤러에서 허용되지 않은 HTTP 메서드로 요청이 들어올 경우 발생한다.
     *
     * @param request HTTP 요청
     * @param e HTTP 메서드 예외
     * @return 에러 응답
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupported(
            HttpServletRequest request,
            HttpRequestMethodNotSupportedException e) {

        String code = GlobalErrorCode.METHOD_NOT_ALLOWED.getCode();
        String message = messageResolver.resolve(code, e.getMethod());

        log.warn("지원하지 않는 HTTP 메서드: {} (path: {})", e.getMethod(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * 지원하지 않는 미디어 타입 처리.
     *
     * <p>Content-Type이 컨트롤러에서 지원하지 않는 형식일 경우 발생한다.
     *
     * @param request HTTP 요청
     * @param e 미디어 타입 예외
     * @return 에러 응답
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupported(
            HttpServletRequest request,
            HttpMediaTypeNotSupportedException e) {

        String code = GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode();
        String message = messageResolver.resolve(code);

        log.warn("지원하지 않는 미디어 타입: {} (path: {})", e.getContentType(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    // ========================================
    // 리소스 관련 예외 처리
    // ========================================

    /**
     * 핸들러 없음 처리 (엔드포인트 없음).
     *
     * <p>요청 URL에 매핑된 컨트롤러 핸들러가 없을 경우 발생한다.
     * spring.mvc.throw-exception-if-no-handler-found=true 설정 필요.
     *
     * @param request HTTP 요청
     * @param e 핸들러 없음 예외
     * @return 에러 응답
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            HttpServletRequest request,
            NoHandlerFoundException e) {

        String code = GlobalErrorCode.ENDPOINT_NOT_FOUND.getCode();
        String message = messageResolver.resolve(code, e.getRequestURL());

        log.warn("엔드포인트 없음: {} {} (path: {})", e.getHttpMethod(), e.getRequestURL(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.NOT_FOUND.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 리소스 없음 처리 (Spring Boot 3.2+).
     *
     * <p>정적 리소스를 찾을 수 없을 때 발생한다.
     * Spring Boot 3.2부터 도입된 예외로, 존재하지 않는 경로 요청 시 발생할 수 있다.
     *
     * @param request HTTP 요청
     * @param e 리소스 없음 예외
     * @return 에러 응답
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            HttpServletRequest request,
            NoResourceFoundException e) {

        String code = GlobalErrorCode.NOT_FOUND.getCode();
        String message = messageResolver.resolve(code, "리소스");

        log.warn("리소스 없음: {} (path: {})", e.getResourcePath(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.NOT_FOUND.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ========================================
    // 인증/인가 예외 처리
    // ========================================

    /**
     * 접근 거부 예외 처리.
     *
     * <p>Spring Security에서 인증은 되었지만 특정 리소스에 대한 접근 권한이 부족할 때 발생한다.
     * 이 핸들러는 해당 예외를 가로채어 전역 에러 코드 {@code FORBIDDEN}에 매핑된 표준 응답을 반환한다.
     *
     * <p>클라이언트는 HTTP 403 상태 코드와 함께 전역 에러 메시지를 전달받는다.
     *
     * @param request HTTP 요청
     * @param e 접근 권한 부족 시 발생하는 예외
     * @return HTTP 403 Forbidden + 표준 에러 응답
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            HttpServletRequest request,
            AccessDeniedException e) {

        String code = GlobalErrorCode.FORBIDDEN.getCode();
        String message = messageResolver.resolve(code);

        log.warn("접근 거부: {} (path: {})", e.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.FORBIDDEN.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ========================================
    // 기타 예외 처리
    // ========================================

    /**
     * {@link IllegalArgumentException} 처리.
     *
     * <p>메서드에 잘못된 인자가 전달되었을 때 발생한다.
     *
     * @param request HTTP 요청
     * @param e 잘못된 인자 예외
     * @return 에러 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            HttpServletRequest request,
            IllegalArgumentException e) {

        String code = GlobalErrorCode.BAD_REQUEST.getCode();

        log.warn("잘못된 인자: {} (path: {})", e.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * {@link IllegalStateException} 처리.
     *
     * <p>객체의 상태가 메서드 호출에 적합하지 않을 때 발생한다.
     *
     * @param request HTTP 요청
     * @param e 잘못된 상태 예외
     * @return 에러 응답
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            HttpServletRequest request,
            IllegalStateException e) {

        String code = GlobalErrorCode.INVALID_STATE.getCode();

        log.warn("잘못된 상태: {} (path: {})", e.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                code,
                e.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * 모든 미처리 예외 처리 (최후의 방어선).
     *
     * <p>위에서 처리되지 않은 모든 예외는 이 핸들러에서 500 에러로 처리된다.
     * 예상하지 못한 런타임 오류를 공통 응답 포맷으로 반환하여 API 일관성을 유지한다.
     *
     * @param request HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(
            HttpServletRequest request,
            Exception e) {

        String code = GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode();
        String message = messageResolver.resolve(code);

        log.error("처리되지 않은 예외 (path: {}): ", request.getRequestURI(), e);

        ApiResponse<Void> response = ApiResponse.error(
                code,
                message,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}