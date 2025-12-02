package io.github.tickatch.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FieldError 단위 테스트.
 */
@DisplayName("FieldError 테스트")
class FieldErrorTest {

    // ========================================
    // of(String, Object, String) 테스트
    // ========================================

    @Nested
    @DisplayName("of(field, value, reason) 테스트")
    class OfWithValueTest {

        @Test
        @DisplayName("필드명, 값, 이유로 FieldError를 생성한다")
        void of_createsFieldErrorWithAllFields() {
            // when
            FieldError error = FieldError.of("email", "invalid-email", "이메일 형식이 올바르지 않습니다.");

            // then
            assertThat(error.getField()).isEqualTo("email");
            assertThat(error.getValue()).isEqualTo("invalid-email");
            assertThat(error.getReason()).isEqualTo("이메일 형식이 올바르지 않습니다.");
        }

        @Test
        @DisplayName("null 값도 허용된다")
        void of_allowsNullValue() {
            // when
            FieldError error = FieldError.of("password", null, "비밀번호는 필수입니다.");

            // then
            assertThat(error.getField()).isEqualTo("password");
            assertThat(error.getValue()).isNull();
            assertThat(error.getReason()).isEqualTo("비밀번호는 필수입니다.");
        }

        @Test
        @DisplayName("숫자 값도 저장할 수 있다")
        void of_allowsNumericValue() {
            // when
            FieldError error = FieldError.of("age", -5, "나이는 0 이상이어야 합니다.");

            // then
            assertThat(error.getValue()).isEqualTo(-5);
        }

        @Test
        @DisplayName("객체 값도 저장할 수 있다")
        void of_allowsObjectValue() {
            // given
            Object complexValue = new Object[] {"a", "b", "c"};

            // when
            FieldError error = FieldError.of("items", complexValue, "유효하지 않은 항목입니다.");

            // then
            assertThat(error.getValue()).isEqualTo(complexValue);
        }
    }

    // ========================================
    // of(String, String) 테스트
    // ========================================

    @Nested
    @DisplayName("of(field, reason) 테스트")
    class OfWithoutValueTest {

        @Test
        @DisplayName("필드명과 이유로 FieldError를 생성한다 (값은 null)")
        void of_createsFieldErrorWithoutValue() {
            // when
            FieldError error = FieldError.of("username", "사용자명은 필수입니다.");

            // then
            assertThat(error.getField()).isEqualTo("username");
            assertThat(error.getValue()).isNull();
            assertThat(error.getReason()).isEqualTo("사용자명은 필수입니다.");
        }
    }

    // ========================================
    // global() 테스트
    // ========================================

    @Nested
    @DisplayName("global() 테스트")
    class GlobalTest {

        @Test
        @DisplayName("전역 에러를 생성한다 (field='global', value=null)")
        void global_createsGlobalError() {
            // when
            FieldError error = FieldError.global("요청 데이터가 올바르지 않습니다.");

            // then
            assertThat(error.getField()).isEqualTo("global");
            assertThat(error.getValue()).isNull();
            assertThat(error.getReason()).isEqualTo("요청 데이터가 올바르지 않습니다.");
        }

        @Test
        @DisplayName("여러 전역 에러를 생성할 수 있다")
        void global_canCreateMultipleErrors() {
            // when
            FieldError error1 = FieldError.global("첫 번째 에러");
            FieldError error2 = FieldError.global("두 번째 에러");

            // then
            assertThat(error1.getField()).isEqualTo("global");
            assertThat(error2.getField()).isEqualTo("global");
            assertThat(error1.getReason()).isEqualTo("첫 번째 에러");
            assertThat(error2.getReason()).isEqualTo("두 번째 에러");
        }
    }

    // ========================================
    // from(FieldError) 테스트
    // ========================================

    @Nested
    @DisplayName("from(Spring FieldError) 테스트")
    class FromSpringFieldErrorTest {

        @Test
        @DisplayName("Spring FieldError를 FieldError로 변환한다")
        void from_convertsSpringFieldError() {
            // given
            org.springframework.validation.FieldError springError = mock(org.springframework.validation.FieldError.class);
            when(springError.getField()).thenReturn("email");
            when(springError.getRejectedValue()).thenReturn("bad-email");
            when(springError.getDefaultMessage()).thenReturn("이메일 형식이 올바르지 않습니다.");

            // when
            FieldError error = FieldError.from(springError);

            // then
            assertThat(error.getField()).isEqualTo("email");
            assertThat(error.getValue()).isEqualTo("bad-email");
            assertThat(error.getReason()).isEqualTo("이메일 형식이 올바르지 않습니다.");
        }

        @Test
        @DisplayName("rejectedValue가 null인 Spring FieldError도 변환한다")
        void from_handlesNullRejectedValue() {
            // given
            org.springframework.validation.FieldError springError = mock(org.springframework.validation.FieldError.class);
            when(springError.getField()).thenReturn("password");
            when(springError.getRejectedValue()).thenReturn(null);
            when(springError.getDefaultMessage()).thenReturn("비밀번호는 필수입니다.");

            // when
            FieldError error = FieldError.from(springError);

            // then
            assertThat(error.getField()).isEqualTo("password");
            assertThat(error.getValue()).isNull();
            assertThat(error.getReason()).isEqualTo("비밀번호는 필수입니다.");
        }

        @Test
        @DisplayName("defaultMessage가 null인 경우도 처리한다")
        void from_handlesNullDefaultMessage() {
            // given
            org.springframework.validation.FieldError springError = mock(org.springframework.validation.FieldError.class);
            when(springError.getField()).thenReturn("field");
            when(springError.getRejectedValue()).thenReturn("value");
            when(springError.getDefaultMessage()).thenReturn(null);

            // when
            FieldError error = FieldError.from(springError);

            // then
            assertThat(error.getField()).isEqualTo("field");
            assertThat(error.getReason()).isNull();
        }
    }

    // ========================================
    // 직렬화 관련 테스트
    // ========================================

    @Nested
    @DisplayName("직렬화 테스트")
    class SerializationTest {

        @Test
        @DisplayName("Serializable 인터페이스를 구현한다")
        void fieldError_isSerializable() {
            // given
            FieldError error = FieldError.of("email", "test", "에러 메시지");

            // then
            assertThat(error).isInstanceOf(java.io.Serializable.class);
        }
    }

    // ========================================
    // Getter 테스트
    // ========================================

    @Nested
    @DisplayName("Getter 테스트")
    class GetterTest {

        @Test
        @DisplayName("getField()는 필드명을 반환한다")
        void getField_returnsFieldName() {
            // given
            FieldError error = FieldError.of("testField", "value", "reason");

            // when & then
            assertThat(error.getField()).isEqualTo("testField");
        }

        @Test
        @DisplayName("getValue()는 값을 반환한다")
        void getValue_returnsValue() {
            // given
            FieldError error = FieldError.of("field", "testValue", "reason");

            // when & then
            assertThat(error.getValue()).isEqualTo("testValue");
        }

        @Test
        @DisplayName("getReason()은 이유를 반환한다")
        void getReason_returnsReason() {
            // given
            FieldError error = FieldError.of("field", "value", "테스트 이유");

            // when & then
            assertThat(error.getReason()).isEqualTo("테스트 이유");
        }
    }

    // ========================================
    // 경계값 테스트
    // ========================================

    @Nested
    @DisplayName("경계값 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("빈 문자열 필드명도 허용된다")
        void of_allowsEmptyFieldName() {
            // when
            FieldError error = FieldError.of("", "value", "reason");

            // then
            assertThat(error.getField()).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 reason도 허용된다")
        void of_allowsEmptyReason() {
            // when
            FieldError error = FieldError.of("field", "value", "");

            // then
            assertThat(error.getReason()).isEmpty();
        }

        @Test
        @DisplayName("매우 긴 문자열도 처리한다")
        void of_handlesLongStrings() {
            // given
            String longString = "a".repeat(10000);

            // when
            FieldError error = FieldError.of(longString, longString, longString);

            // then
            assertThat(error.getField()).hasSize(10000);
            assertThat(error.getValue()).isEqualTo(longString);
            assertThat(error.getReason()).hasSize(10000);
        }

        @Test
        @DisplayName("특수 문자가 포함된 값도 처리한다")
        void of_handlesSpecialCharacters() {
            // given
            String specialChars = "특수문자: <>&\"'\\n\\t{}[]";

            // when
            FieldError error = FieldError.of("field", specialChars, specialChars);

            // then
            assertThat(error.getValue()).isEqualTo(specialChars);
            assertThat(error.getReason()).isEqualTo(specialChars);
        }

        @Test
        @DisplayName("한글 필드명과 메시지도 처리한다")
        void of_handlesKoreanCharacters() {
            // when
            FieldError error = FieldError.of("이메일주소", "잘못된값", "올바른 이메일 형식이 아닙니다.");

            // then
            assertThat(error.getField()).isEqualTo("이메일주소");
            assertThat(error.getValue()).isEqualTo("잘못된값");
            assertThat(error.getReason()).isEqualTo("올바른 이메일 형식이 아닙니다.");
        }
    }
}