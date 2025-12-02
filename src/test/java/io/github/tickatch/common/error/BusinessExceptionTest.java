package io.github.tickatch.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * BusinessException 단위 테스트.
 */
@DisplayName("BusinessException 테스트")
class BusinessExceptionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("ErrorCode만으로 예외를 생성한다")
        void constructor_withErrorCode() {
            // when
            BusinessException exception = new BusinessException(GlobalErrorCode.BAD_REQUEST);

            // then
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.BAD_REQUEST);
            assertThat(exception.getCode()).isEqualTo("BAD_REQUEST");
            assertThat(exception.getStatus()).isEqualTo(400);
            assertThat(exception.getErrorArgs()).isEmpty();
        }

        @Test
        @DisplayName("ErrorCode와 메시지 인자로 예외를 생성한다")
        void constructor_withErrorCodeAndArgs() {
            // when
            BusinessException exception = new BusinessException(GlobalErrorCode.NOT_FOUND, "티켓", 123);

            // then
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.NOT_FOUND);
            assertThat(exception.getErrorArgs()).containsExactly("티켓", 123);
        }

        @Test
        @DisplayName("ErrorCode와 원인 예외로 예외를 생성한다")
        void constructor_withErrorCodeAndCause() {
            // given
            RuntimeException cause = new RuntimeException("원인 예외");

            // when
            BusinessException exception = new BusinessException(GlobalErrorCode.DATABASE_ERROR, cause);

            // then
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.DATABASE_ERROR);
        }

        @Test
        @DisplayName("ErrorCode, 원인, 메시지 인자로 예외를 생성한다")
        void constructor_withAllParams() {
            // given
            RuntimeException cause = new RuntimeException("DB 에러");

            // when
            BusinessException exception = new BusinessException(GlobalErrorCode.DATABASE_ERROR, cause, "users");

            // then
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getErrorArgs()).containsExactly("users");
        }

        @Test
        @DisplayName("null errorArgs는 빈 배열로 처리된다")
        void constructor_withNullArgs_becomesEmptyArray() {
            // when
            BusinessException exception = new BusinessException(GlobalErrorCode.BAD_REQUEST, (Object[]) null);

            // then
            assertThat(exception.getErrorArgs()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("getter 테스트")
    class GetterTest {

        @Test
        @DisplayName("getStatus()는 ErrorCode의 HTTP 상태를 반환한다")
        void getStatus_returnsHttpStatus() {
            // given
            BusinessException exception = new BusinessException(GlobalErrorCode.FORBIDDEN);

            // then
            assertThat(exception.getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("getCode()는 에러 코드 문자열을 반환한다")
        void getCode_returnsErrorCodeString() {
            // given
            BusinessException exception = new BusinessException(GlobalErrorCode.UNAUTHORIZED);

            // then
            assertThat(exception.getCode()).isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("getMessage()는 에러 코드를 반환한다")
        void getMessage_returnsErrorCode() {
            // given
            BusinessException exception = new BusinessException(GlobalErrorCode.VALIDATION_ERROR);

            // then
            assertThat(exception.getMessage()).isEqualTo("VALIDATION_ERROR");
        }
    }

    @Nested
    @DisplayName("상속 및 예외 계층 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("RuntimeException을 상속한다")
        void extendsRuntimeException() {
            // when
            BusinessException exception = new BusinessException(GlobalErrorCode.BAD_REQUEST);

            // then
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("throw/catch가 정상 동작한다")
        void throwAndCatch() {
            // when & then
            assertThatThrownBy(() -> {
                throw new BusinessException(GlobalErrorCode.NOT_FOUND, "리소스");
            })
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode()).isEqualTo("NOT_FOUND");
                    });
        }
    }

    @Nested
    @DisplayName("다양한 ErrorCode 테스트")
    class VariousErrorCodeTest {

        @Test
        @DisplayName("4xx 클라이언트 에러")
        void clientErrors() {
            assertThat(new BusinessException(GlobalErrorCode.BAD_REQUEST).getStatus()).isEqualTo(400);
            assertThat(new BusinessException(GlobalErrorCode.UNAUTHORIZED).getStatus()).isEqualTo(401);
            assertThat(new BusinessException(GlobalErrorCode.FORBIDDEN).getStatus()).isEqualTo(403);
            assertThat(new BusinessException(GlobalErrorCode.NOT_FOUND).getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("5xx 서버 에러")
        void serverErrors() {
            assertThat(new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR).getStatus()).isEqualTo(500);
            assertThat(new BusinessException(GlobalErrorCode.SERVICE_UNAVAILABLE).getStatus()).isEqualTo(503);
        }
    }
}