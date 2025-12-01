package io.github.tickatch.common.error;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.stream.Stream;

/**
 * 다양한 검증 예외를 {@link FieldError} 리스트로 변환하는 공통 파서.
 *
 * <p>Spring의 검증 예외들을 일관된 {@link FieldError} 형식으로 변환하여
 * API 응답에서 통일된 에러 형식을 제공한다.
 *
 * <p>지원하는 예외 유형:
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} - @RequestBody + @Valid 검증 실패</li>
 *   <li>{@link org.springframework.validation.BindException} - @ModelAttribute 검증 실패</li>
 *   <li>{@link HandlerMethodValidationException} - @RequestParam, @PathVariable 검증 실패 (Spring 6+)</li>
 * </ul>
 *
 * <p>사용 예시:
 * <pre>{@code
 * @ExceptionHandler(MethodArgumentNotValidException.class)
 * public ResponseEntity<?> handle(MethodArgumentNotValidException e) {
 *     List<FieldError> errors = ValidationErrorParser.from(e);
 *     // ...
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 */
public final class ValidationErrorParser {

    private ValidationErrorParser() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    /**
     * {@link MethodArgumentNotValidException}에서 필드 에러 목록을 추출한다.
     *
     * <p>@RequestBody와 @Valid 조합 사용 시 발생하는 검증 실패를 처리한다.
     * 필드 레벨 에러와 글로벌(객체 레벨) 에러를 모두 포함한다.
     *
     * @param e MethodArgumentNotValidException 예외
     * @return 변환된 {@link FieldError} 리스트
     */
    public static List<FieldError> from(MethodArgumentNotValidException e) {
        return from(e.getBindingResult());
    }

    /**
     * {@link org.springframework.validation.BindException}에서 필드 에러 목록을 추출한다.
     *
     * <p>@ModelAttribute 사용 시 발생하는 검증 실패를 처리한다.
     *
     * @param e BindException 예외
     * @return 변환된 {@link FieldError} 리스트
     */
    public static List<FieldError> from(org.springframework.validation.BindException e) {
        return from(e.getBindingResult());
    }

    /**
     * {@link BindingResult}에서 필드 에러 목록을 추출한다.
     *
     * <p>필드 에러와 글로벌 에러를 합쳐서 반환한다.
     *
     * @param bindingResult 바인딩 결과
     * @return 변환된 {@link FieldError} 리스트
     */
    public static List<FieldError> from(BindingResult bindingResult) {
        return Stream.concat(
                // 필드 레벨 에러
                bindingResult.getFieldErrors().stream()
                        .map(error -> FieldError.of(
                                error.getField(),
                                error.getRejectedValue(),
                                error.getDefaultMessage()
                        )),
                // 글로벌(객체 레벨) 에러
                bindingResult.getGlobalErrors().stream()
                        .map(error -> FieldError.global(error.getDefaultMessage()))
        ).toList();
    }

    /**
     * {@link HandlerMethodValidationException}에서 필드 에러 목록을 추출한다.
     *
     * <p>Spring 6부터 도입된 메서드 파라미터 수준의 검증 실패를 처리한다.
     * 주로 @RequestParam, @PathVariable, @RequestHeader 등의 검증 실패 시 발생한다.
     *
     * <p>예시:
     * <pre>{@code
     * @GetMapping("/users/{id}")
     * public User getUser(@PathVariable @Min(1) Long id) { ... }
     * // id가 0 이하일 경우 HandlerMethodValidationException 발생
     * }</pre>
     *
     * @param e HandlerMethodValidationException 예외
     * @return 변환된 {@link FieldError} 리스트
     */
    public static List<FieldError> from(HandlerMethodValidationException e) {
        return e.getParameterValidationResults().stream()
                .map(result -> FieldError.of(
                        result.getMethodParameter().getParameterName(),
                        result.getArgument(),
                        result.getResolvableErrors().isEmpty()
                                ? "유효하지 않은 값입니다."
                                : result.getResolvableErrors().get(0).getDefaultMessage()
                ))
                .toList();
    }
}