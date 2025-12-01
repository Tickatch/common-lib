package io.github.tickatch.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * UUID 유틸리티.
 *
 * <p>UUID 생성 및 검증. 티케팅 도메인 ID 생성 지원.
 *
 * @author Tickatch
 * @since 0.0.1
 */
public final class UuidUtils {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private UuidUtils() {
        throw new AssertionError("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    // ========================================
    // 기본 UUID 생성
    // ========================================

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static String generateCompact() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // ========================================
    // UUID 검증
    // ========================================

    public static boolean isValid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        return UUID_PATTERN.matcher(uuid).matches();
    }

    public static boolean isValidAnyFormat(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        if (UUID_PATTERN.matcher(uuid).matches()) {
            return true;
        }
        return uuid.length() == 32 && uuid.matches("^[0-9a-fA-F]{32}$");
    }

    public static UUID parse(String uuid) {
        if (!isValid(uuid)) {
            throw new IllegalArgumentException("유효하지 않은 UUID 형식입니다: " + uuid);
        }
        return UUID.fromString(uuid);
    }

    // ========================================
    // 형식 변환
    // ========================================

    public static String toStandardFormat(String compactUuid) {
        if (compactUuid == null || compactUuid.length() != 32) {
            throw new IllegalArgumentException("유효하지 않은 compact UUID 형식입니다: " + compactUuid);
        }
        return String.format("%s-%s-%s-%s-%s",
                compactUuid.substring(0, 8),
                compactUuid.substring(8, 12),
                compactUuid.substring(12, 16),
                compactUuid.substring(16, 20),
                compactUuid.substring(20, 32));
    }

    public static String toCompactFormat(String uuid) {
        if (!isValid(uuid)) {
            throw new IllegalArgumentException("유효하지 않은 UUID 형식입니다: " + uuid);
        }
        return uuid.replace("-", "");
    }

    // ========================================
    // 도메인 ID 생성
    // ========================================

    /**
     * 도메인 ID 생성 (형식: PREFIX-xxxxxxxx)
     */
    public static String generateDomainId(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix는 필수입니다.");
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix.toUpperCase() + "-" + uuid.substring(0, 8);
    }

    /**
     * 도메인 ID 생성 (길이 지정)
     */
    public static String generateDomainId(String prefix, int length) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix는 필수입니다.");
        }
        if (length < 4 || length > 32) {
            throw new IllegalArgumentException("length는 4-32 사이여야 합니다.");
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix.toUpperCase() + "-" + uuid.substring(0, length);
    }

    /**
     * 타임스탬프 기반 도메인 ID 생성 (형식: PREFIX-yyyyMMddHHmmss-xxxxxx)
     */
    public static String generateTimestampId(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix는 필수입니다.");
        }
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return prefix.toUpperCase() + "-" + timestamp + "-" + uuid;
    }

    public static String extractPrefix(String domainId) {
        if (domainId == null || !domainId.contains("-")) {
            throw new IllegalArgumentException("유효하지 않은 도메인 ID 형식입니다: " + domainId);
        }
        return domainId.substring(0, domainId.indexOf("-"));
    }

    public static boolean isValidDomainId(String domainId, String expectedPrefix) {
        if (domainId == null || domainId.isBlank() || !domainId.contains("-")) {
            return false;
        }
        String[] parts = domainId.split("-", 2);
        if (parts.length != 2) {
            return false;
        }
        String prefix = parts[0];
        String idPart = parts[1];
        if (expectedPrefix != null && !prefix.equalsIgnoreCase(expectedPrefix)) {
            return false;
        }
        if (!prefix.matches("^[A-Z]+$")) {
            return false;
        }
        return idPart.matches("^[0-9a-fA-F]+$") || idPart.matches("^\\d{14}-[0-9a-fA-F]+$");
    }

    // ========================================
    // 티케팅 도메인 상수 및 메서드
    // ========================================

    public static final String PREFIX_USER = "USR";
    public static final String PREFIX_EVENT = "EVT";
    public static final String PREFIX_TICKET = "TKT";
    public static final String PREFIX_ORDER = "ORD";
    public static final String PREFIX_PAYMENT = "PAY";
    public static final String PREFIX_RESERVATION = "RSV";
    public static final String PREFIX_SEAT = "SEAT";
    public static final String PREFIX_VENUE = "VNU";
    public static final String PREFIX_NOTIFICATION = "NTF";

    public static String generateUserId() {
        return generateDomainId(PREFIX_USER);
    }

    public static String generateEventId() {
        return generateDomainId(PREFIX_EVENT);
    }

    public static String generateTicketId() {
        return generateDomainId(PREFIX_TICKET);
    }

    public static String generateOrderId() {
        return generateTimestampId(PREFIX_ORDER);
    }

    public static String generatePaymentId() {
        return generateTimestampId(PREFIX_PAYMENT);
    }

    public static String generateReservationId() {
        return generateTimestampId(PREFIX_RESERVATION);
    }

    public static String generateSeatId() {
        return generateDomainId(PREFIX_SEAT);
    }

    public static String generateVenueId() {
        return generateDomainId(PREFIX_VENUE);
    }

    public static String generateNotificationId() {
        return generateDomainId(PREFIX_NOTIFICATION);
    }
}