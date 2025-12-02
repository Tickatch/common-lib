package io.github.tickatch.common.api;

import io.github.tickatch.common.error.FieldError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ApiResponse 단위 테스트.
 */
@DisplayName("ApiResponse 테스트")
class ApiResponseTest {

    // ========================================
    // 성공 응답 테스트
    // ========================================

    @Nested
    @DisplayName("success() 테스트")
    class SuccessTest {

        @Test
        @DisplayName("데이터와 함께 성공 응답을 생성한다")
        void success_withData_createsSuccessResponse() {
            // given
            TestData data = new TestData(1L, "테스트");

            // when
            ApiResponse<TestData> response = ApiResponse.success(data);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(data);
            assertThat(response.getMessage()).isNull();
            assertThat(response.getError()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
            assertThat(response.getTraceId()).isNull();
        }

        @Test
        @DisplayName("데이터와 메시지를 포함한 성공 응답을 생성한다")
        void success_withDataAndMessage_createsSuccessResponse() {
            // given
            TestData data = new TestData(1L, "테스트");
            String message = "작업이 완료되었습니다.";

            // when
            ApiResponse<TestData> response = ApiResponse.success(data, message);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(data);
            assertThat(response.getMessage()).isEqualTo(message);
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("데이터 없이 성공 응답을 생성한다")
        void success_withoutData_createsSuccessResponse() {
            // when
            ApiResponse<Void> response = ApiResponse.success();

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("메시지만 포함한 성공 응답을 생성한다")
        void successWithMessage_createsSuccessResponse() {
            // given
            String message = "처리되었습니다.";

            // when
            ApiResponse<Void> response = ApiResponse.successWithMessage(message);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNull();
            assertThat(response.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("null 데이터도 성공 응답으로 생성할 수 있다")
        void success_withNullData_createsSuccessResponse() {
            // when
            ApiResponse<TestData> response = ApiResponse.success(null);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("timestamp는 현재 시간 근처이다")
        void success_timestampIsRecent() {
            // given
            Instant before = Instant.now();

            // when
            ApiResponse<Void> response = ApiResponse.success();

            // then
            Instant after = Instant.now();
            assertThat(response.getTimestamp())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }
    }

    // ========================================
    // 실패 응답 테스트
    // ========================================

    @Nested
    @DisplayName("error() 테스트")
    class ErrorTest {

        @Test
        @DisplayName("코드, 메시지, 상태로 실패 응답을 생성한다")
        void error_withCodeMessageStatus_createsErrorResponse() {
            // when
            ApiResponse<Void> response = ApiResponse.error("NOT_FOUND", "리소스를 찾을 수 없습니다.", 404);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getData()).isNull();
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().getCode()).isEqualTo("NOT_FOUND");
            assertThat(response.getError().getMessage()).isEqualTo("리소스를 찾을 수 없습니다.");
            assertThat(response.getError().getStatus()).isEqualTo(404);
            assertThat(response.getError().getPath()).isNull();
            assertThat(response.getError().getFields()).isNull();
        }

        @Test
        @DisplayName("경로를 포함한 실패 응답을 생성한다")
        void error_withPath_createsErrorResponse() {
            // when
            ApiResponse<Void> response = ApiResponse.error(
                    "BAD_REQUEST",
                    "잘못된 요청입니다.",
                    400,
                    "/api/tickets"
            );

            // then
            assertThat(response.getError().getPath()).isEqualTo("/api/tickets");
        }

        @Test
        @DisplayName("필드 에러를 포함한 실패 응답을 생성한다")
        void error_withFieldErrors_createsErrorResponse() {
            // given
            List<FieldError> fieldErrors = List.of(
                    FieldError.of("email", "invalid", "이메일 형식이 올바르지 않습니다."),
                    FieldError.of("password", null, "비밀번호는 필수입니다.")
            );

            // when
            ApiResponse<Void> response = ApiResponse.error(
                    "VALIDATION_ERROR",
                    "입력값이 유효하지 않습니다.",
                    400,
                    "/api/users",
                    fieldErrors
            );

            // then
            assertThat(response.getError().getFields())
                    .hasSize(2)
                    .extracting(FieldError::getField)
                    .containsExactly("email", "password");
        }

        @Test
        @DisplayName("빈 필드 에러 목록은 null로 처리된다")
        void error_withEmptyFieldErrors_setsFieldsNull() {
            // when
            ApiResponse<Void> response = ApiResponse.error(
                    "VALIDATION_ERROR",
                    "입력값이 유효하지 않습니다.",
                    400,
                    "/api/users",
                    List.of()
            );

            // then
            assertThat(response.getError().getFields()).isNull();
        }

        @Test
        @DisplayName("null 필드 에러 목록은 null로 유지된다")
        void error_withNullFieldErrors_setsFieldsNull() {
            // when
            ApiResponse<Void> response = ApiResponse.error(
                    "VALIDATION_ERROR",
                    "입력값이 유효하지 않습니다.",
                    400,
                    "/api/users",
                    null
            );

            // then
            assertThat(response.getError().getFields()).isNull();
        }

        @Test
        @DisplayName("ErrorDetail로 실패 응답을 생성한다")
        void error_withErrorDetail_createsErrorResponse() {
            // given
            ApiResponse.ErrorDetail errorDetail = ApiResponse.ErrorDetail.of(
                    "INTERNAL_SERVER_ERROR",
                    "서버 오류가 발생했습니다.",
                    500
            );

            // when
            ApiResponse<Void> response = ApiResponse.error(errorDetail);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getError()).isEqualTo(errorDetail);
        }
    }

    // ========================================
    // traceId 포함 응답 테스트
    // ========================================

    @Nested
    @DisplayName("traceId 포함 응답 테스트")
    class TraceIdTest {

        @Test
        @DisplayName("traceId를 포함한 성공 응답을 생성한다")
        void successWithTrace_includesTraceId() {
            // given
            TestData data = new TestData(1L, "테스트");
            String traceId = "abc-123-def";

            // when
            ApiResponse<TestData> response = ApiResponse.successWithTrace(data, traceId);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(data);
            assertThat(response.getTraceId()).isEqualTo(traceId);
        }

        @Test
        @DisplayName("traceId를 포함한 실패 응답을 생성한다")
        void errorWithTrace_includesTraceId() {
            // given
            ApiResponse.ErrorDetail errorDetail = ApiResponse.ErrorDetail.of(
                    "ERROR",
                    "에러 발생",
                    500
            );
            String traceId = "xyz-789";

            // when
            ApiResponse<Void> response = ApiResponse.errorWithTrace(errorDetail, traceId);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getTraceId()).isEqualTo(traceId);
        }
    }

    // ========================================
    // ErrorDetail 테스트
    // ========================================

    @Nested
    @DisplayName("ErrorDetail 테스트")
    class ErrorDetailTest {

        @Test
        @DisplayName("코드, 메시지, 상태로 ErrorDetail을 생성한다")
        void of_withCodeMessageStatus_createsErrorDetail() {
            // when
            ApiResponse.ErrorDetail detail = ApiResponse.ErrorDetail.of(
                    "TEST_ERROR",
                    "테스트 에러",
                    400
            );

            // then
            assertThat(detail.getCode()).isEqualTo("TEST_ERROR");
            assertThat(detail.getMessage()).isEqualTo("테스트 에러");
            assertThat(detail.getStatus()).isEqualTo(400);
            assertThat(detail.getPath()).isNull();
            assertThat(detail.getFields()).isNull();
        }

        @Test
        @DisplayName("경로를 포함한 ErrorDetail을 생성한다")
        void of_withPath_createsErrorDetail() {
            // when
            ApiResponse.ErrorDetail detail = ApiResponse.ErrorDetail.of(
                    "TEST_ERROR",
                    "테스트 에러",
                    400,
                    "/api/test"
            );

            // then
            assertThat(detail.getPath()).isEqualTo("/api/test");
        }

        @Test
        @DisplayName("필드 에러를 포함한 ErrorDetail을 생성한다")
        void of_withFields_createsErrorDetail() {
            // given
            List<FieldError> fields = List.of(
                    FieldError.of("name", "에러 이유")
            );

            // when
            ApiResponse.ErrorDetail detail = ApiResponse.ErrorDetail.of(
                    "VALIDATION_ERROR",
                    "검증 실패",
                    400,
                    "/api/test",
                    fields
            );

            // then
            assertThat(detail.getFields()).hasSize(1);
        }
    }

    // ========================================
    // Serializable 테스트
    // ========================================

    @Nested
    @DisplayName("Serializable 테스트")
    class SerializableTest {

        @Test
        @DisplayName("ApiResponse는 Serializable이다")
        void apiResponse_isSerializable() {
            // given
            ApiResponse<String> response = ApiResponse.success("test");

            // then
            assertThat(response).isInstanceOf(java.io.Serializable.class);
        }

        @Test
        @DisplayName("ErrorDetail은 Serializable이다")
        void errorDetail_isSerializable() {
            // given
            ApiResponse.ErrorDetail detail = ApiResponse.ErrorDetail.of("CODE", "message", 400);

            // then
            assertThat(detail).isInstanceOf(java.io.Serializable.class);
        }
    }

    // ========================================
    // 다양한 데이터 타입 테스트
    // ========================================

    @Nested
    @DisplayName("다양한 데이터 타입 테스트")
    class DataTypeTest {

        @Test
        @DisplayName("List 데이터를 포함한 응답을 생성한다")
        void success_withListData() {
            // given
            List<String> data = List.of("a", "b", "c");

            // when
            ApiResponse<List<String>> response = ApiResponse.success(data);

            // then
            assertThat(response.getData()).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("문자열 데이터를 포함한 응답을 생성한다")
        void success_withStringData() {
            // when
            ApiResponse<String> response = ApiResponse.success("테스트 문자열");

            // then
            assertThat(response.getData()).isEqualTo("테스트 문자열");
        }

        @Test
        @DisplayName("숫자 데이터를 포함한 응답을 생성한다")
        void success_withNumberData() {
            // when
            ApiResponse<Long> response = ApiResponse.success(12345L);

            // then
            assertThat(response.getData()).isEqualTo(12345L);
        }
    }

    // ========================================
    // 테스트용 DTO
    // ========================================

    record TestData(Long id, String name) {}
}