package io.github.tickatch.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/**
 * UuidUtils 단위 테스트.
 */
@DisplayName("UuidUtils 테스트")
class UuidUtilsTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private static final Pattern COMPACT_UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");

    // ========================================
    // 기본 UUID 생성 테스트
    // ========================================

    @Nested
    @DisplayName("generate() 테스트")
    class GenerateTest {

        @Test
        @DisplayName("표준 UUID 형식으로 생성된다")
        void generate_returnsValidUuidFormat() {
            // when
            String uuid = UuidUtils.generate();

            // then
            assertThat(uuid).matches(UUID_PATTERN);
        }

        @Test
        @DisplayName("매번 고유한 UUID를 생성한다")
        void generate_returnsUniqueValues() {
            // given
            Set<String> uuids = new HashSet<>();

            // when
            for (int i = 0; i < 1000; i++) {
                uuids.add(UuidUtils.generate());
            }

            // then
            assertThat(uuids).hasSize(1000);
        }
    }

    @Nested
    @DisplayName("generateCompact() 테스트")
    class GenerateCompactTest {

        @Test
        @DisplayName("하이픈 없는 32자리 UUID를 생성한다")
        void generateCompact_returnsCompactFormat() {
            // when
            String compactUuid = UuidUtils.generateCompact();

            // then
            assertThat(compactUuid)
                    .hasSize(32)
                    .matches(COMPACT_UUID_PATTERN);
        }

        @Test
        @DisplayName("매번 고유한 compact UUID를 생성한다")
        void generateCompact_returnsUniqueValues() {
            // given
            Set<String> uuids = new HashSet<>();

            // when
            for (int i = 0; i < 1000; i++) {
                uuids.add(UuidUtils.generateCompact());
            }

            // then
            assertThat(uuids).hasSize(1000);
        }
    }

    // ========================================
    // UUID 검증 테스트
    // ========================================

    @Nested
    @DisplayName("isValid() 테스트")
    class IsValidTest {

        @Test
        @DisplayName("유효한 UUID는 true를 반환한다")
        void isValid_withValidUuid_returnsTrue() {
            // given
            String validUuid = "550e8400-e29b-41d4-a716-446655440000";

            // when & then
            assertThat(UuidUtils.isValid(validUuid)).isTrue();
        }

        @Test
        @DisplayName("generate()로 생성한 UUID는 유효하다")
        void isValid_withGeneratedUuid_returnsTrue() {
            // given
            String generatedUuid = UuidUtils.generate();

            // when & then
            assertThat(UuidUtils.isValid(generatedUuid)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "invalid-uuid", "550e8400e29b41d4a716446655440000", "not-a-uuid-at-all"})
        @DisplayName("유효하지 않은 UUID는 false를 반환한다")
        void isValid_withInvalidUuid_returnsFalse(String invalidUuid) {
            // when & then
            assertThat(UuidUtils.isValid(invalidUuid)).isFalse();
        }

        @Test
        @DisplayName("대소문자 혼합 UUID도 유효하다")
        void isValid_withMixedCaseUuid_returnsTrue() {
            // given
            String mixedCaseUuid = "550E8400-E29B-41D4-A716-446655440000";

            // when & then
            assertThat(UuidUtils.isValid(mixedCaseUuid)).isTrue();
        }
    }

    @Nested
    @DisplayName("isValidAnyFormat() 테스트")
    class IsValidAnyFormatTest {

        @Test
        @DisplayName("표준 형식 UUID는 true를 반환한다")
        void isValidAnyFormat_withStandardFormat_returnsTrue() {
            // given
            String standardUuid = "550e8400-e29b-41d4-a716-446655440000";

            // when & then
            assertThat(UuidUtils.isValidAnyFormat(standardUuid)).isTrue();
        }

        @Test
        @DisplayName("compact 형식 UUID는 true를 반환한다")
        void isValidAnyFormat_withCompactFormat_returnsTrue() {
            // given
            String compactUuid = "550e8400e29b41d4a716446655440000";

            // when & then
            assertThat(UuidUtils.isValidAnyFormat(compactUuid)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "invalid", "12345"})
        @DisplayName("유효하지 않은 값은 false를 반환한다")
        void isValidAnyFormat_withInvalidValue_returnsFalse(String invalid) {
            // when & then
            assertThat(UuidUtils.isValidAnyFormat(invalid)).isFalse();
        }
    }

    @Nested
    @DisplayName("parse() 테스트")
    class ParseTest {

        @Test
        @DisplayName("유효한 UUID 문자열을 UUID 객체로 파싱한다")
        void parse_withValidUuid_returnsUuidObject() {
            // given
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";

            // when
            UUID uuid = UuidUtils.parse(uuidString);

            // then
            assertThat(uuid).isNotNull();
            assertThat(uuid.toString()).isEqualToIgnoringCase(uuidString);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"invalid-uuid"})
        @DisplayName("유효하지 않은 UUID는 예외를 발생시킨다")
        void parse_withInvalidUuid_throwsException(String invalidUuid) {
            // when & then
            assertThatThrownBy(() -> UuidUtils.parse(invalidUuid))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 UUID 형식");
        }
    }

    // ========================================
    // 형식 변환 테스트
    // ========================================

    @Nested
    @DisplayName("toStandardFormat() 테스트")
    class ToStandardFormatTest {

        @Test
        @DisplayName("compact UUID를 표준 형식으로 변환한다")
        void toStandardFormat_convertsCorrectly() {
            // given
            String compactUuid = "550e8400e29b41d4a716446655440000";

            // when
            String standardUuid = UuidUtils.toStandardFormat(compactUuid);

            // then
            assertThat(standardUuid).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        }

        @Test
        @DisplayName("잘못된 길이의 문자열은 예외를 발생시킨다")
        void toStandardFormat_withInvalidLength_throwsException() {
            // given
            String invalidLength = "12345";

            // when & then
            assertThatThrownBy(() -> UuidUtils.toStandardFormat(invalidLength))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 compact UUID 형식");
        }

        @Test
        @DisplayName("null 값은 예외를 발생시킨다")
        void toStandardFormat_withNull_throwsException() {
            // when & then
            assertThatThrownBy(() -> UuidUtils.toStandardFormat(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("toCompactFormat() 테스트")
    class ToCompactFormatTest {

        @Test
        @DisplayName("표준 UUID를 compact 형식으로 변환한다")
        void toCompactFormat_convertsCorrectly() {
            // given
            String standardUuid = "550e8400-e29b-41d4-a716-446655440000";

            // when
            String compactUuid = UuidUtils.toCompactFormat(standardUuid);

            // then
            assertThat(compactUuid)
                    .hasSize(32)
                    .isEqualTo("550e8400e29b41d4a716446655440000");
        }

        @Test
        @DisplayName("유효하지 않은 UUID는 예외를 발생시킨다")
        void toCompactFormat_withInvalidUuid_throwsException() {
            // given
            String invalidUuid = "not-a-uuid";

            // when & then
            assertThatThrownBy(() -> UuidUtils.toCompactFormat(invalidUuid))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 UUID 형식");
        }
    }

    // ========================================
    // 도메인 ID 생성 테스트
    // ========================================

    @Nested
    @DisplayName("generateDomainId() 테스트")
    class GenerateDomainIdTest {

        @Test
        @DisplayName("prefix와 8자리 ID로 도메인 ID를 생성한다")
        void generateDomainId_withPrefix_returnsCorrectFormat() {
            // when
            String domainId = UuidUtils.generateDomainId("USR");

            // then
            assertThat(domainId)
                    .startsWith("USR-")
                    .hasSize(12); // USR- (4) + 8자리
        }

        @Test
        @DisplayName("prefix는 대문자로 변환된다")
        void generateDomainId_convertsToUpperCase() {
            // when
            String domainId = UuidUtils.generateDomainId("usr");

            // then
            assertThat(domainId).startsWith("USR-");
        }

        @Test
        @DisplayName("지정된 길이로 도메인 ID를 생성한다")
        void generateDomainId_withLength_returnsCorrectLength() {
            // when
            String domainId = UuidUtils.generateDomainId("EVT", 12);

            // then
            assertThat(domainId)
                    .startsWith("EVT-")
                    .hasSize(16); // EVT- (4) + 12자리
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("prefix가 null이거나 빈 값이면 예외를 발생시킨다")
        void generateDomainId_withNullOrEmptyPrefix_throwsException(String prefix) {
            // when & then
            assertThatThrownBy(() -> UuidUtils.generateDomainId(prefix))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("prefix는 필수");
        }

        @ParameterizedTest
        @ValueSource(ints = {3, 33})
        @DisplayName("길이가 4-32 범위를 벗어나면 예외를 발생시킨다")
        void generateDomainId_withInvalidLength_throwsException(int length) {
            // when & then
            assertThatThrownBy(() -> UuidUtils.generateDomainId("USR", length))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("length는 4-32 사이");
        }
    }

    @Nested
    @DisplayName("generateTimestampId() 테스트")
    class GenerateTimestampIdTest {

        @Test
        @DisplayName("타임스탬프 기반 ID를 생성한다")
        void generateTimestampId_returnsCorrectFormat() {
            // when
            String timestampId = UuidUtils.generateTimestampId("ORD");

            // then
            assertThat(timestampId)
                    .startsWith("ORD-")
                    .matches("^ORD-\\d{14}-[0-9a-f]{6}$");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("prefix가 null이거나 빈 값이면 예외를 발생시킨다")
        void generateTimestampId_withNullOrEmptyPrefix_throwsException(String prefix) {
            // when & then
            assertThatThrownBy(() -> UuidUtils.generateTimestampId(prefix))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("prefix는 필수");
        }
    }

    @Nested
    @DisplayName("extractPrefix() 테스트")
    class ExtractPrefixTest {

        @Test
        @DisplayName("도메인 ID에서 prefix를 추출한다")
        void extractPrefix_extractsCorrectly() {
            // given
            String domainId = "USR-abc12345";

            // when
            String prefix = UuidUtils.extractPrefix(domainId);

            // then
            assertThat(prefix).isEqualTo("USR");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"noHyphen"})
        @DisplayName("유효하지 않은 도메인 ID는 예외를 발생시킨다")
        void extractPrefix_withInvalidDomainId_throwsException(String invalidId) {
            // when & then
            assertThatThrownBy(() -> UuidUtils.extractPrefix(invalidId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 도메인 ID 형식");
        }
    }

    @Nested
    @DisplayName("isValidDomainId() 테스트")
    class IsValidDomainIdTest {

        @Test
        @DisplayName("유효한 도메인 ID는 true를 반환한다")
        void isValidDomainId_withValidId_returnsTrue() {
            // given
            String domainId = "USR-abc12345";

            // when & then
            assertThat(UuidUtils.isValidDomainId(domainId, "USR")).isTrue();
        }

        @Test
        @DisplayName("expectedPrefix가 null이면 prefix 검증을 건너뛴다")
        void isValidDomainId_withNullExpectedPrefix_skipsValidation() {
            // given
            String domainId = "ANY-abc12345";

            // when & then
            assertThat(UuidUtils.isValidDomainId(domainId, null)).isTrue();
        }

        @Test
        @DisplayName("타임스탬프 형식 도메인 ID도 유효하다")
        void isValidDomainId_withTimestampFormat_returnsTrue() {
            // given
            String timestampId = "ORD-20250101120000-abc123";

            // when & then
            assertThat(UuidUtils.isValidDomainId(timestampId, "ORD")).isTrue();
        }

        @Test
        @DisplayName("prefix가 일치하지 않으면 false를 반환한다")
        void isValidDomainId_withMismatchedPrefix_returnsFalse() {
            // given
            String domainId = "USR-abc12345";

            // when & then
            assertThat(UuidUtils.isValidDomainId(domainId, "EVT")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "nohyphen"})
        @DisplayName("유효하지 않은 형식은 false를 반환한다")
        void isValidDomainId_withInvalidFormat_returnsFalse(String invalidId) {
            // when & then
            assertThat(UuidUtils.isValidDomainId(invalidId, "USR")).isFalse();
        }
    }

    // ========================================
    // 도메인별 ID 생성 메서드 테스트
    // ========================================

    @Nested
    @DisplayName("도메인별 ID 생성 메서드 테스트")
    class DomainSpecificIdTest {

        @Test
        @DisplayName("generateUserId()는 USR- prefix로 생성한다")
        void generateUserId_returnsCorrectPrefix() {
            assertThat(UuidUtils.generateUserId()).startsWith("USR-");
        }

        @Test
        @DisplayName("generateEventId()는 EVT- prefix로 생성한다")
        void generateEventId_returnsCorrectPrefix() {
            assertThat(UuidUtils.generateEventId()).startsWith("EVT-");
        }

        @Test
        @DisplayName("generateTicketId()는 TKT- prefix로 생성한다")
        void generateTicketId_returnsCorrectPrefix() {
            assertThat(UuidUtils.generateTicketId()).startsWith("TKT-");
        }

        @Test
        @DisplayName("generateOrderId()는 타임스탬프 형식으로 생성한다")
        void generateOrderId_returnsTimestampFormat() {
            String orderId = UuidUtils.generateOrderId();
            assertThat(orderId)
                    .startsWith("ORD-")
                    .matches("^ORD-\\d{14}-[0-9a-f]{6}$");
        }

        @Test
        @DisplayName("generatePaymentId()는 타임스탬프 형식으로 생성한다")
        void generatePaymentId_returnsTimestampFormat() {
            String paymentId = UuidUtils.generatePaymentId();
            assertThat(paymentId)
                    .startsWith("PAY-")
                    .matches("^PAY-\\d{14}-[0-9a-f]{6}$");
        }

        @Test
        @DisplayName("generateReservationId()는 타임스탬프 형식으로 생성한다")
        void generateReservationId_returnsTimestampFormat() {
            String reservationId = UuidUtils.generateReservationId();
            assertThat(reservationId)
                    .startsWith("RSV-")
                    .matches("^RSV-\\d{14}-[0-9a-f]{6}$");
        }

        @Test
        @DisplayName("generateSeatId()는 SEAT- prefix로 생성한다")
        void generateSeatId_returnsCorrectPrefix() {
            assertThat(UuidUtils.generateSeatId()).startsWith("SEAT-");
        }

        @Test
        @DisplayName("generateVenueId()는 VNU- prefix로 생성한다")
        void generateVenueId_returnsCorrectPrefix() {
            assertThat(UuidUtils.generateVenueId()).startsWith("VNU-");
        }

        @Test
        @DisplayName("generateNotificationId()는 NTF- prefix로 생성한다")
        void generateNotificationId_returnsCorrectPrefix() {
            assertThat(UuidUtils.generateNotificationId()).startsWith("NTF-");
        }
    }

    // ========================================
    // 상수 테스트
    // ========================================

    @Nested
    @DisplayName("PREFIX 상수 테스트")
    class PrefixConstantsTest {

        @Test
        @DisplayName("모든 PREFIX 상수가 정의되어 있다")
        void allPrefixConstantsAreDefined() {
            assertThat(UuidUtils.PREFIX_USER).isEqualTo("USR");
            assertThat(UuidUtils.PREFIX_EVENT).isEqualTo("EVT");
            assertThat(UuidUtils.PREFIX_TICKET).isEqualTo("TKT");
            assertThat(UuidUtils.PREFIX_ORDER).isEqualTo("ORD");
            assertThat(UuidUtils.PREFIX_PAYMENT).isEqualTo("PAY");
            assertThat(UuidUtils.PREFIX_RESERVATION).isEqualTo("RSV");
            assertThat(UuidUtils.PREFIX_SEAT).isEqualTo("SEAT");
            assertThat(UuidUtils.PREFIX_VENUE).isEqualTo("VNU");
            assertThat(UuidUtils.PREFIX_NOTIFICATION).isEqualTo("NTF");
        }
    }
}