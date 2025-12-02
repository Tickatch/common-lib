package io.github.tickatch.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * MdcUtils 단위 테스트.
 */
@DisplayName("MdcUtils 테스트")
class MdcUtilsTest {

    @BeforeEach
    void setUp() {
        // 각 테스트 전 MDC 초기화
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 MDC 초기화
        MDC.clear();
    }

    // ========================================
    // 상수 테스트
    // ========================================

    @Nested
    @DisplayName("상수 테스트")
    class ConstantsTest {

        @Test
        @DisplayName("REQUEST_ID 상수가 정의되어 있다")
        void requestIdConstant_isDefined() {
            assertThat(MdcUtils.REQUEST_ID).isEqualTo("requestId");
        }

        @Test
        @DisplayName("USER_ID 상수가 정의되어 있다")
        void userIdConstant_isDefined() {
            assertThat(MdcUtils.USER_ID).isEqualTo("userId");
        }
    }

    // ========================================
    // 범용 MDC 메서드 테스트
    // ========================================

    @Nested
    @DisplayName("put() 테스트")
    class PutTest {

        @Test
        @DisplayName("키-값 쌍을 MDC에 저장한다")
        void put_storesValueInMdc() {
            // when
            MdcUtils.put("testKey", "testValue");

            // then
            assertThat(MDC.get("testKey")).isEqualTo("testValue");
        }

        @Test
        @DisplayName("키가 null이면 저장하지 않는다")
        void put_withNullKey_doesNotStore() {
            // when
            MdcUtils.put(null, "testValue");

            // then
            // null 키로 조회하면 NPE 발생하므로, 다른 방법으로 검증
            // MDC에 아무것도 저장되지 않았음을 확인
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        }

        @Test
        @DisplayName("값이 null이면 저장하지 않는다")
        void put_withNullValue_doesNotStore() {
            // when
            MdcUtils.put("testKey", null);

            // then
            assertThat(MDC.get("testKey")).isNull();
        }

        @Test
        @DisplayName("기존 값을 덮어쓴다")
        void put_overwritesExistingValue() {
            // given
            MdcUtils.put("key", "oldValue");

            // when
            MdcUtils.put("key", "newValue");

            // then
            assertThat(MDC.get("key")).isEqualTo("newValue");
        }
    }

    @Nested
    @DisplayName("get() 테스트")
    class GetTest {

        @Test
        @DisplayName("MDC에서 값을 조회한다")
        void get_returnsStoredValue() {
            // given
            MDC.put("testKey", "testValue");

            // when
            String value = MdcUtils.get("testKey");

            // then
            assertThat(value).isEqualTo("testValue");
        }

        @Test
        @DisplayName("존재하지 않는 키는 null을 반환한다")
        void get_withNonExistentKey_returnsNull() {
            // when
            String value = MdcUtils.get("nonExistent");

            // then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("키가 null이면 null을 반환한다")
        void get_withNullKey_returnsNull() {
            // when
            String value = MdcUtils.get(null);

            // then
            assertThat(value).isNull();
        }
    }

    @Nested
    @DisplayName("remove() 테스트")
    class RemoveTest {

        @Test
        @DisplayName("MDC에서 값을 제거한다")
        void remove_removesValue() {
            // given
            MDC.put("testKey", "testValue");

            // when
            MdcUtils.remove("testKey");

            // then
            assertThat(MDC.get("testKey")).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 키를 제거해도 예외가 발생하지 않는다")
        void remove_withNonExistentKey_doesNotThrow() {
            // when & then
            assertThatCode(() -> MdcUtils.remove("nonExistent"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("키가 null이면 무시한다")
        void remove_withNullKey_doesNothing() {
            // given
            MDC.put("testKey", "testValue");

            // when
            MdcUtils.remove(null);

            // then
            assertThat(MDC.get("testKey")).isEqualTo("testValue");
        }
    }

    @Nested
    @DisplayName("clear() 테스트")
    class ClearTest {

        @Test
        @DisplayName("MDC의 모든 값을 클리어한다")
        void clear_removesAllValues() {
            // given
            MDC.put("key1", "value1");
            MDC.put("key2", "value2");
            MDC.put("key3", "value3");

            // when
            MdcUtils.clear();

            // then
            assertThat(MDC.get("key1")).isNull();
            assertThat(MDC.get("key2")).isNull();
            assertThat(MDC.get("key3")).isNull();
        }

        @Test
        @DisplayName("빈 MDC에서 clear해도 예외가 발생하지 않는다")
        void clear_onEmptyMdc_doesNotThrow() {
            // when & then
            assertThatCode(MdcUtils::clear)
                    .doesNotThrowAnyException();
        }
    }

    // ========================================
    // Request ID 관련 테스트
    // ========================================

    @Nested
    @DisplayName("setRequestId() 테스트")
    class SetRequestIdTest {

        @Test
        @DisplayName("requestId를 MDC에 저장한다")
        void setRequestId_storesInMdc() {
            // given
            String requestId = "abc-123-def";

            // when
            MdcUtils.setRequestId(requestId);

            // then
            assertThat(MDC.get(MdcUtils.REQUEST_ID)).isEqualTo(requestId);
        }

        @Test
        @DisplayName("UUID 형식의 requestId를 저장한다")
        void setRequestId_withUuid_storesInMdc() {
            // given
            String requestId = UUID.randomUUID().toString();

            // when
            MdcUtils.setRequestId(requestId);

            // then
            assertThat(MDC.get(MdcUtils.REQUEST_ID)).isEqualTo(requestId);
        }
    }

    @Nested
    @DisplayName("getRequestId() 테스트")
    class GetRequestIdTest {

        @Test
        @DisplayName("저장된 requestId를 반환한다")
        void getRequestId_returnsStoredValue() {
            // given
            String requestId = "test-request-id";
            MDC.put(MdcUtils.REQUEST_ID, requestId);

            // when
            String result = MdcUtils.getRequestId();

            // then
            assertThat(result).isEqualTo(requestId);
        }

        @Test
        @DisplayName("requestId가 없으면 null을 반환한다")
        void getRequestId_whenNotSet_returnsNull() {
            // when
            String result = MdcUtils.getRequestId();

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getRequestUuid() 테스트")
    class GetRequestUuidTest {

        @Test
        @DisplayName("유효한 UUID 형식의 requestId를 UUID로 변환한다")
        void getRequestUuid_withValidUuid_returnsUuid() {
            // given
            UUID originalUuid = UUID.randomUUID();
            MDC.put(MdcUtils.REQUEST_ID, originalUuid.toString());

            // when
            UUID result = MdcUtils.getRequestUuid();

            // then
            assertThat(result).isEqualTo(originalUuid);
        }

        @Test
        @DisplayName("requestId가 없으면 null을 반환한다")
        void getRequestUuid_whenNotSet_returnsNull() {
            // when
            UUID result = MdcUtils.getRequestUuid();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열이면 null을 반환한다")
        void getRequestUuid_withEmptyString_returnsNull() {
            // given
            MDC.put(MdcUtils.REQUEST_ID, "");

            // when
            UUID result = MdcUtils.getRequestUuid();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("공백 문자열이면 null을 반환한다")
        void getRequestUuid_withBlankString_returnsNull() {
            // given
            MDC.put(MdcUtils.REQUEST_ID, "   ");

            // when
            UUID result = MdcUtils.getRequestUuid();

            // then
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-a-uuid", "12345", "invalid-format"})
        @DisplayName("유효하지 않은 UUID 형식이면 null을 반환한다")
        void getRequestUuid_withInvalidUuid_returnsNull(String invalidUuid) {
            // given
            MDC.put(MdcUtils.REQUEST_ID, invalidUuid);

            // when
            UUID result = MdcUtils.getRequestUuid();

            // then
            assertThat(result).isNull();
        }
    }

    // ========================================
    // User ID 관련 테스트
    // ========================================

    @Nested
    @DisplayName("setUserId() 테스트")
    class SetUserIdTest {

        @Test
        @DisplayName("userId를 문자열로 변환하여 MDC에 저장한다")
        void setUserId_storesAsStringInMdc() {
            // given
            Long userId = 12345L;

            // when
            MdcUtils.setUserId(userId);

            // then
            assertThat(MDC.get(MdcUtils.USER_ID)).isEqualTo("12345");
        }

        @Test
        @DisplayName("userId가 null이면 저장하지 않는다")
        void setUserId_withNull_doesNotStore() {
            // when
            MdcUtils.setUserId(null);

            // then
            assertThat(MDC.get(MdcUtils.USER_ID)).isNull();
        }

        @Test
        @DisplayName("0 값도 저장한다")
        void setUserId_withZero_stores() {
            // when
            MdcUtils.setUserId(0L);

            // then
            assertThat(MDC.get(MdcUtils.USER_ID)).isEqualTo("0");
        }

        @Test
        @DisplayName("음수 값도 저장한다")
        void setUserId_withNegative_stores() {
            // when
            MdcUtils.setUserId(-1L);

            // then
            assertThat(MDC.get(MdcUtils.USER_ID)).isEqualTo("-1");
        }

        @Test
        @DisplayName("큰 값도 저장한다")
        void setUserId_withLargeValue_stores() {
            // given
            Long largeId = Long.MAX_VALUE;

            // when
            MdcUtils.setUserId(largeId);

            // then
            assertThat(MDC.get(MdcUtils.USER_ID)).isEqualTo(String.valueOf(Long.MAX_VALUE));
        }
    }

    @Nested
    @DisplayName("getUserId() 테스트")
    class GetUserIdTest {

        @Test
        @DisplayName("저장된 userId를 Long으로 반환한다")
        void getUserId_returnsStoredValue() {
            // given
            MDC.put(MdcUtils.USER_ID, "12345");

            // when
            Long result = MdcUtils.getUserId();

            // then
            assertThat(result).isEqualTo(12345L);
        }

        @Test
        @DisplayName("userId가 없으면 null을 반환한다")
        void getUserId_whenNotSet_returnsNull() {
            // when
            Long result = MdcUtils.getUserId();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열이면 null을 반환한다")
        void getUserId_withEmptyString_returnsNull() {
            // given
            MDC.put(MdcUtils.USER_ID, "");

            // when
            Long result = MdcUtils.getUserId();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("공백 문자열이면 null을 반환한다")
        void getUserId_withBlankString_returnsNull() {
            // given
            MDC.put(MdcUtils.USER_ID, "   ");

            // when
            Long result = MdcUtils.getUserId();

            // then
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-a-number", "12.34", "abc123"})
        @DisplayName("숫자로 변환할 수 없는 값이면 null을 반환한다")
        void getUserId_withInvalidNumber_returnsNull(String invalidNumber) {
            // given
            MDC.put(MdcUtils.USER_ID, invalidNumber);

            // when
            Long result = MdcUtils.getUserId();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("음수 값도 반환한다")
        void getUserId_withNegativeValue_returnsNegative() {
            // given
            MDC.put(MdcUtils.USER_ID, "-999");

            // when
            Long result = MdcUtils.getUserId();

            // then
            assertThat(result).isEqualTo(-999L);
        }

        @Test
        @DisplayName("Long 범위를 초과하면 null을 반환한다")
        void getUserId_withOverflow_returnsNull() {
            // given
            MDC.put(MdcUtils.USER_ID, "99999999999999999999999999999");

            // when
            Long result = MdcUtils.getUserId();

            // then
            assertThat(result).isNull();
        }
    }

    // ========================================
    // 통합 시나리오 테스트
    // ========================================

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("requestId와 userId를 함께 설정하고 조회한다")
        void setAndGet_bothRequestIdAndUserId() {
            // given
            String requestId = UUID.randomUUID().toString();
            Long userId = 42L;

            // when
            MdcUtils.setRequestId(requestId);
            MdcUtils.setUserId(userId);

            // then
            assertThat(MdcUtils.getRequestId()).isEqualTo(requestId);
            assertThat(MdcUtils.getUserId()).isEqualTo(userId);
            assertThat(MdcUtils.getRequestUuid()).isNotNull();
        }

        @Test
        @DisplayName("clear() 후에는 모든 값이 null이다")
        void clear_removesAllCustomValues() {
            // given
            MdcUtils.setRequestId("test-request-id");
            MdcUtils.setUserId(123L);
            MdcUtils.put("customKey", "customValue");

            // when
            MdcUtils.clear();

            // then
            assertThat(MdcUtils.getRequestId()).isNull();
            assertThat(MdcUtils.getUserId()).isNull();
            assertThat(MdcUtils.get("customKey")).isNull();
        }

        @Test
        @DisplayName("여러 커스텀 키-값을 설정하고 조회한다")
        void multipleCustomKeys() {
            // when
            MdcUtils.put("traceId", "trace-123");
            MdcUtils.put("spanId", "span-456");
            MdcUtils.put("service", "ticket-service");

            // then
            assertThat(MdcUtils.get("traceId")).isEqualTo("trace-123");
            assertThat(MdcUtils.get("spanId")).isEqualTo("span-456");
            assertThat(MdcUtils.get("service")).isEqualTo("ticket-service");
        }

        @Test
        @DisplayName("특정 키만 제거하면 다른 키는 유지된다")
        void remove_onlyRemovesSpecificKey() {
            // given
            MdcUtils.setRequestId("request-id");
            MdcUtils.setUserId(123L);

            // when
            MdcUtils.remove(MdcUtils.REQUEST_ID);

            // then
            assertThat(MdcUtils.getRequestId()).isNull();
            assertThat(MdcUtils.getUserId()).isEqualTo(123L);
        }
    }
}