package io.github.tickatch.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // ========================================
    // 상수 테스트
    // ========================================

    @Nested
    @DisplayName("상수 테스트")
    class ConstantsTest {

        @Test
        @DisplayName("REQUEST_ID 상수 값을 확인한다")
        void requestId_constant() {
            assertThat(MdcUtils.REQUEST_ID).isEqualTo("requestId");
        }

        @Test
        @DisplayName("USER_ID 상수 값을 확인한다")
        void userId_constant() {
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
        @DisplayName("키와 값을 MDC에 저장한다")
        void put_storesValue() {
            // when
            MdcUtils.put("customKey", "customValue");

            // then
            assertThat(MDC.get("customKey")).isEqualTo("customValue");
        }

        @Test
        @DisplayName("null 키는 무시한다")
        void put_withNullKey_ignores() {
            // when
            MdcUtils.put(null, "value");

            // then - 예외 없이 통과
            assertThat(MDC.getCopyOfContextMap()).isNull();
        }

        @Test
        @DisplayName("null 값은 무시한다")
        void put_withNullValue_ignores() {
            // when
            MdcUtils.put("key", null);

            // then
            assertThat(MDC.get("key")).isNull();
        }

        @Test
        @DisplayName("기존 값을 덮어쓴다")
        void put_overwrites() {
            // given
            MdcUtils.put("key", "first");

            // when
            MdcUtils.put("key", "second");

            // then
            assertThat(MDC.get("key")).isEqualTo("second");
        }
    }

    @Nested
    @DisplayName("get() 테스트")
    class GetTest {

        @Test
        @DisplayName("저장된 값을 조회한다")
        void get_returnsValue() {
            // given
            MDC.put("testKey", "testValue");

            // when
            String result = MdcUtils.get("testKey");

            // then
            assertThat(result).isEqualTo("testValue");
        }

        @Test
        @DisplayName("존재하지 않는 키는 null을 반환한다")
        void get_withNonExistentKey_returnsNull() {
            // when
            String result = MdcUtils.get("nonExistent");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null 키는 null을 반환한다")
        void get_withNullKey_returnsNull() {
            // when
            String result = MdcUtils.get(null);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("remove() 테스트")
    class RemoveTest {

        @Test
        @DisplayName("키를 제거한다")
        void remove_removesKey() {
            // given
            MDC.put("toRemove", "value");

            // when
            MdcUtils.remove("toRemove");

            // then
            assertThat(MDC.get("toRemove")).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 키 제거는 예외 없이 처리된다")
        void remove_nonExistentKey_noException() {
            // when & then
            assertThatCode(() -> MdcUtils.remove("nonExistent")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null 키 제거는 예외 없이 처리된다")
        void remove_nullKey_noException() {
            // when & then
            assertThatCode(() -> MdcUtils.remove(null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("clear() 테스트")
    class ClearTest {

        @Test
        @DisplayName("모든 MDC 값을 클리어한다")
        void clear_removesAllValues() {
            // given
            MDC.put("key1", "value1");
            MDC.put("key2", "value2");

            // when
            MdcUtils.clear();

            // then
            assertThat(MDC.get("key1")).isNull();
            assertThat(MDC.get("key2")).isNull();
        }
    }

    // ========================================
    // Request ID 테스트
    // ========================================

    @Nested
    @DisplayName("Request ID 테스트")
    class RequestIdTest {

        @Test
        @DisplayName("requestId를 저장한다")
        void setRequestId_storesValue() {
            // given
            String requestId = UUID.randomUUID().toString();

            // when
            MdcUtils.setRequestId(requestId);

            // then
            assertThat(MDC.get("requestId")).isEqualTo(requestId);
        }

        @Test
        @DisplayName("requestId를 조회한다")
        void getRequestId_returnsValue() {
            // given
            String requestId = "req-12345";
            MDC.put("requestId", requestId);

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
        @DisplayName("유효한 UUID를 파싱한다")
        void getRequestUuid_withValidUuid_returnsUuid() {
            // given
            UUID uuid = UUID.randomUUID();
            MdcUtils.setRequestId(uuid.toString());

            // when
            UUID result = MdcUtils.getRequestUuid();

            // then
            assertThat(result).isEqualTo(uuid);
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
            MdcUtils.setRequestId("");

            // when
            UUID result = MdcUtils.getRequestUuid();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("유효하지 않은 UUID면 null을 반환한다")
        void getRequestUuid_withInvalidUuid_returnsNull() {
            // given
            MDC.put("requestId", "not-a-valid-uuid");

            // when
            UUID result = MdcUtils.getRequestUuid();

            // then
            assertThat(result).isNull();
        }
    }

    // ========================================
    // User ID 테스트 (String)
    // ========================================

    @Nested
    @DisplayName("User ID 테스트")
    class UserIdTest {

        @Test
        @DisplayName("userId를 저장한다")
        void setUserId_storesValue() {
            // given
            String userId = UUID.randomUUID().toString();

            // when
            MdcUtils.setUserId(userId);

            // then
            assertThat(MDC.get("userId")).isEqualTo(userId);
        }

        @Test
        @DisplayName("userId를 조회한다")
        void getUserId_returnsValue() {
            // given
            String userId = "usr-abc-123";
            MDC.put("userId", userId);

            // when
            String result = MdcUtils.getUserId();

            // then
            assertThat(result).isEqualTo(userId);
        }

        @Test
        @DisplayName("userId가 없으면 null을 반환한다")
        void getUserId_whenNotSet_returnsNull() {
            // when
            String result = MdcUtils.getUserId();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열 userId는 저장하지 않는다")
        void setUserId_withEmptyString_ignores() {
            // when
            MdcUtils.setUserId("");

            // then
            assertThat(MDC.get("userId")).isNull();
        }

        @Test
        @DisplayName("공백만 있는 userId는 저장하지 않는다")
        void setUserId_withWhitespace_ignores() {
            // when
            MdcUtils.setUserId("   ");

            // then
            assertThat(MDC.get("userId")).isNull();
        }

        @Test
        @DisplayName("null userId는 저장하지 않는다")
        void setUserId_withNull_ignores() {
            // when
            MdcUtils.setUserId(null);

            // then
            assertThat(MDC.get("userId")).isNull();
        }

        @Test
        @DisplayName("빈 문자열이 저장되어 있으면 getUserId()는 null을 반환한다")
        void getUserId_withEmptyStringStored_returnsNull() {
            // given
            MDC.put("userId", "");

            // when
            String result = MdcUtils.getUserId();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("공백만 저장되어 있으면 getUserId()는 null을 반환한다")
        void getUserId_withWhitespaceStored_returnsNull() {
            // given
            MDC.put("userId", "   ");

            // when
            String result = MdcUtils.getUserId();

            // then
            assertThat(result).isNull();
        }
    }

    // ========================================
    // 통합 시나리오 테스트
    // ========================================

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("requestId와 userId를 동시에 설정한다")
        void setBothRequestIdAndUserId() {
            // given
            String requestId = UUID.randomUUID().toString();
            String userId = "usr-test-123";

            // when
            MdcUtils.setRequestId(requestId);
            MdcUtils.setUserId(userId);

            // then
            assertThat(MdcUtils.getRequestId()).isEqualTo(requestId);
            assertThat(MdcUtils.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("clear() 후 모든 값이 null이다")
        void afterClear_allValuesAreNull() {
            // given
            MdcUtils.setRequestId("req-123");
            MdcUtils.setUserId("usr-456");
            MdcUtils.put("custom", "value");

            // when
            MdcUtils.clear();

            // then
            assertThat(MdcUtils.getRequestId()).isNull();
            assertThat(MdcUtils.getUserId()).isNull();
            assertThat(MdcUtils.get("custom")).isNull();
        }

        @Test
        @DisplayName("여러 커스텀 키를 설정하고 조회한다")
        void multipleCustomKeys() {
            // when
            MdcUtils.put("key1", "value1");
            MdcUtils.put("key2", "value2");
            MdcUtils.put("key3", "value3");

            // then
            assertThat(MdcUtils.get("key1")).isEqualTo("value1");
            assertThat(MdcUtils.get("key2")).isEqualTo("value2");
            assertThat(MdcUtils.get("key3")).isEqualTo("value3");
        }

        @Test
        @DisplayName("특정 키만 제거한다")
        void removeSpecificKey() {
            // given
            MdcUtils.setRequestId("req-123");
            MdcUtils.setUserId("usr-456");

            // when
            MdcUtils.remove("requestId");

            // then
            assertThat(MdcUtils.getRequestId()).isNull();
            assertThat(MdcUtils.getUserId()).isEqualTo("usr-456");
        }
    }
}