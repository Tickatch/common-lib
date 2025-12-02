package io.github.tickatch.common.jpa;

import io.github.tickatch.common.security.AuthenticatedUser;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * JPA Auditing을 위한 현재 사용자 정보 제공자.
 *
 * <p>Spring Data JPA의 {@link AuditorAware} 인터페이스를 구현하여
 * {@code @CreatedBy}, {@code @LastModifiedBy} 어노테이션이 적용된 필드에
 * 현재 인증된 사용자 ID를 자동으로 설정한다.
 *
 * <p>인증 정보는 Spring Security의 {@link SecurityContextHolder}에서 가져오며,
 * 인증되지 않은 요청이나 시스템 작업의 경우 "SYSTEM"을 반환한다.
 *
 * <h2>동작 방식</h2>
 * <ul>
 *   <li>인증된 사용자가 있는 경우: {@link AuthenticatedUser#getUserId()}를 반환</li>
 *   <li>인증되지 않은 경우: "SYSTEM" 반환</li>
 *   <li>익명 사용자(Anonymous)인 경우: "SYSTEM" 반환</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @Entity
 * public class Order extends BaseEntity {
 *     // createdBy, updatedBy 필드가 자동으로 채워짐
 * }
 *
 * // 저장 시 현재 로그인한 사용자 ID가 createdBy에 설정됨
 * orderRepository.save(order);
 * }</pre>
 *
 * <p>이 클래스는 common-lib의 AutoConfiguration에 의해 자동으로 빈 등록된다.
 *
 * @author Tickatch
 * @since 0.0.1
 * @see BaseEntity
 * @see AuthenticatedUser
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    /** 인증되지 않은 요청에서 사용할 기본 감사자 ID. */
    private static final String SYSTEM_USER = "SYSTEM";

    /**
     * 현재 감사자(Auditor) 정보를 반환한다.
     *
     * <p>Spring Security의 {@link SecurityContextHolder}에서 현재 인증 정보를 조회하여
     * 사용자 ID를 반환한다. 인증 정보가 없거나 유효하지 않은 경우 "SYSTEM"을 반환한다.
     *
     * @return 현재 사용자 ID (Optional로 감싸서 반환)
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(getCurrentUserId())
                .or(() -> Optional.of(SYSTEM_USER));
    }

    /**
     * SecurityContext에서 현재 인증된 사용자 ID를 추출한다.
     *
     * @return 사용자 ID 문자열, 인증되지 않은 경우 null
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // AuthenticatedUser인 경우
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            String userId = authenticatedUser.getUserId();
            return StringUtils.hasText(userId) ? userId : null;
        }

        // 익명 사용자 또는 기타 (anonymousUser 문자열)
        if (principal instanceof String && "anonymousUser".equals(principal)) {
            return null;
        }

        // 기타 Principal 타입
        return principal.toString();
    }
}