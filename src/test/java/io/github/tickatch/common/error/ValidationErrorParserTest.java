package io.github.tickatch.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ValidationErrorParser 단위 테스트.
 */
@DisplayName("ValidationErrorParser 테스트")
class ValidationErrorParserTest {

    @Nested
    @DisplayName("from(MethodArgumentNotValidException) 테스트")
    class FromMethodArgumentNotValidExceptionTest {

        @Test
        @DisplayName("필드 에러를 FieldError 리스트로 변환한다")
        void from_convertsFieldErrors() {
            // given
            org.springframework.validation.FieldError springError = mock(org.springframework.validation.FieldError.class);
            when(springError.getField()).thenReturn("email");
            when(springError.getRejectedValue()).thenReturn("invalid");
            when(springError.getDefaultMessage()).thenReturn("이메일 형식이 올바르지 않습니다");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(springError));
            when(bindingResult.getGlobalErrors()).thenReturn(List.of());

            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

            // when
            List<FieldError> errors = ValidationErrorParser.from(exception);

            // then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getField()).isEqualTo("email");
            assertThat(errors.get(0).getValue()).isEqualTo("invalid");
            assertThat(errors.get(0).getReason()).isEqualTo("이메일 형식이 올바르지 않습니다");
        }

        @Test
        @DisplayName("여러 필드 에러를 변환한다")
        void from_convertsMultipleFieldErrors() {
            // given
            org.springframework.validation.FieldError error1 = mock(org.springframework.validation.FieldError.class);
            when(error1.getField()).thenReturn("email");
            when(error1.getRejectedValue()).thenReturn("bad");
            when(error1.getDefaultMessage()).thenReturn("이메일 오류");

            org.springframework.validation.FieldError error2 = mock(org.springframework.validation.FieldError.class);
            when(error2.getField()).thenReturn("password");
            when(error2.getRejectedValue()).thenReturn(null);
            when(error2.getDefaultMessage()).thenReturn("비밀번호 필수");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));
            when(bindingResult.getGlobalErrors()).thenReturn(List.of());

            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

            // when
            List<FieldError> errors = ValidationErrorParser.from(exception);

            // then
            assertThat(errors).hasSize(2);
            assertThat(errors).extracting(FieldError::getField)
                    .containsExactly("email", "password");
        }

        @Test
        @DisplayName("글로벌 에러도 포함한다")
        void from_includesGlobalErrors() {
            // given
            ObjectError globalError = mock(ObjectError.class);
            when(globalError.getDefaultMessage()).thenReturn("전역 오류입니다");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());
            when(bindingResult.getGlobalErrors()).thenReturn(List.of(globalError));

            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

            // when
            List<FieldError> errors = ValidationErrorParser.from(exception);

            // then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getField()).isEqualTo("global");
            assertThat(errors.get(0).getReason()).isEqualTo("전역 오류입니다");
        }

        @Test
        @DisplayName("에러가 없으면 빈 리스트를 반환한다")
        void from_withNoErrors_returnsEmptyList() {
            // given
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());
            when(bindingResult.getGlobalErrors()).thenReturn(List.of());

            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

            // when
            List<FieldError> errors = ValidationErrorParser.from(exception);

            // then
            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("from(BindException) 테스트")
    class FromBindExceptionTest {

        @Test
        @DisplayName("BindException에서 필드 에러를 변환한다")
        void from_convertsBindExceptionErrors() {
            // given
            org.springframework.validation.FieldError springError = mock(org.springframework.validation.FieldError.class);
            when(springError.getField()).thenReturn("name");
            when(springError.getRejectedValue()).thenReturn("");
            when(springError.getDefaultMessage()).thenReturn("이름은 필수입니다");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(springError));
            when(bindingResult.getGlobalErrors()).thenReturn(List.of());

            org.springframework.validation.BindException exception = mock(org.springframework.validation.BindException.class);
            when(exception.getBindingResult()).thenReturn(bindingResult);

            // when
            List<FieldError> errors = ValidationErrorParser.from(exception);

            // then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getField()).isEqualTo("name");
        }
    }

    @Nested
    @DisplayName("from(BindingResult) 테스트")
    class FromBindingResultTest {

        @Test
        @DisplayName("BindingResult에서 직접 변환한다")
        void from_convertsBindingResultDirectly() {
            // given
            org.springframework.validation.FieldError springError = mock(org.springframework.validation.FieldError.class);
            when(springError.getField()).thenReturn("age");
            when(springError.getRejectedValue()).thenReturn(-1);
            when(springError.getDefaultMessage()).thenReturn("나이는 0 이상이어야 합니다");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(springError));
            when(bindingResult.getGlobalErrors()).thenReturn(List.of());

            // when
            List<FieldError> errors = ValidationErrorParser.from(bindingResult);

            // then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getValue()).isEqualTo(-1);
        }

        @Test
        @DisplayName("필드 에러와 글로벌 에러를 함께 반환한다")
        void from_combinesFieldAndGlobalErrors() {
            // given
            org.springframework.validation.FieldError fieldError = mock(org.springframework.validation.FieldError.class);
            when(fieldError.getField()).thenReturn("field1");
            when(fieldError.getRejectedValue()).thenReturn("val");
            when(fieldError.getDefaultMessage()).thenReturn("필드 오류");

            ObjectError globalError = mock(ObjectError.class);
            when(globalError.getDefaultMessage()).thenReturn("글로벌 오류");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
            when(bindingResult.getGlobalErrors()).thenReturn(List.of(globalError));

            // when
            List<FieldError> errors = ValidationErrorParser.from(bindingResult);

            // then
            assertThat(errors).hasSize(2);
            assertThat(errors).extracting(FieldError::getField)
                    .containsExactly("field1", "global");
        }
    }

    @Nested
    @DisplayName("from(HandlerMethodValidationException) 테스트")
    class FromHandlerMethodValidationExceptionTest {

        @Test
        @DisplayName("파라미터 검증 에러를 변환한다")
        void from_convertsParameterValidationErrors() {
            // given
            MethodParameter methodParameter = mock(MethodParameter.class);
            when(methodParameter.getParameterName()).thenReturn("id");

            org.springframework.context.MessageSourceResolvable resolvableError = mock(org.springframework.context.MessageSourceResolvable.class);
            when(resolvableError.getDefaultMessage()).thenReturn("1 이상이어야 합니다");

            ParameterValidationResult validationResult = mock(ParameterValidationResult.class);
            when(validationResult.getMethodParameter()).thenReturn(methodParameter);
            when(validationResult.getArgument()).thenReturn(0);
            when(validationResult.getResolvableErrors()).thenReturn(List.of(resolvableError));

            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            when(exception.getParameterValidationResults()).thenReturn(List.of(validationResult));

            // when
            List<FieldError> errors = ValidationErrorParser.from(exception);

            // then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getField()).isEqualTo("id");
            assertThat(errors.get(0).getValue()).isEqualTo(0);
            assertThat(errors.get(0).getReason()).isEqualTo("1 이상이어야 합니다");
        }

        @Test
        @DisplayName("resolvableErrors가 비어있으면 기본 메시지를 사용한다")
        void from_usesDefaultMessageWhenNoResolvableErrors() {
            // given
            MethodParameter methodParameter = mock(MethodParameter.class);
            when(methodParameter.getParameterName()).thenReturn("size");

            ParameterValidationResult validationResult = mock(ParameterValidationResult.class);
            when(validationResult.getMethodParameter()).thenReturn(methodParameter);
            when(validationResult.getArgument()).thenReturn(-5);
            when(validationResult.getResolvableErrors()).thenReturn(List.of());

            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            when(exception.getParameterValidationResults()).thenReturn(List.of(validationResult));

            // when
            List<FieldError> errors = ValidationErrorParser.from(exception);

            // then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getReason()).isEqualTo("유효하지 않은 값입니다.");
        }
    }

    @Nested
    @DisplayName("유틸리티 클래스 테스트")
    class UtilityClassTest {

        @Test
        @DisplayName("인스턴스화할 수 없다")
        void cannotInstantiate() throws Exception {
            var constructor = ValidationErrorParser.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            // private 생성자가 있음을 확인
            assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())).isTrue();
        }
    }
}