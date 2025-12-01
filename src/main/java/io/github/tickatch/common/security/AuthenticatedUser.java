package io.github.tickatch.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 인증 모델을 표현하는 사용자 정보 클래스.
 *
 * <p>API Gateway에서 전달된 사용자 ID를 기반으로 애플리케이션 내부에서
 * 인증된 사용자로 동작할 수 있도록 구현된 {@link UserDetails} 구현체이다.
 *
 * <p>이 클래스는 최소한의 정보(userId)만 포함하며, 필요에 따라 확장 가능하다.
 * 비밀번호 기반 인증을 사용하지 않으므로 {@link #getPassword()}는 빈 문자열을 반환한다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * // 컨트롤러에서 현재 사용자 조회
 * @GetMapping("/me")
 * public UserInfo getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
 *     Long userId = user.getUserId();
 *     return userService.findById(userId);
 * }
 *
 * // SecurityContext에서 직접 조회
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
 * Long userId = user.getUserId();
 * }</pre>
 *
 * @param userId 사용자 ID (API Gateway에서 전달된 값)
 * @author Tickatch
 * @since 0.0.1
 * @see LoginFilter
 * @see BaseSecurityConfig
 */
public record AuthenticatedUser(Long userId) implements UserDetails {

    /**
     * {@link AuthenticatedUser} 객체를 생성하는 정적 팩토리 메서드.
     *
     * @param userId 사용자 ID
     * @return 생성된 {@link AuthenticatedUser} 인스턴스
     */
    public static AuthenticatedUser of(Long userId) {
        return new AuthenticatedUser(userId);
    }

    /**
     * 사용자 ID를 반환한다.
     *
     * @return 사용자 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 사용자 권한 목록을 반환한다.
     *
     * <p>기본적으로 빈 목록을 반환한다. 권한 기반 접근 제어가 필요한 경우
     * 이 클래스를 확장하여 역할(role) 정보를 추가할 수 있다.
     *
     * @return 빈 권한 목록
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    /**
     * 비밀번호를 반환한다.
     *
     * <p>비밀번호 기반 인증을 사용하지 않기 때문에 빈 문자열을 반환한다.
     *
     * @return 빈 문자열
     */
    @Override
    public String getPassword() {
        return "";
    }

    /**
     * Spring Security가 사용하는 사용자명을 반환한다.
     *
     * <p>사용자 ID를 문자열로 변환하여 반환한다.
     *
     * @return 사용자 ID 문자열
     */
    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    /**
     * 계정 만료 여부를 반환한다.
     *
     * @return 항상 true (만료되지 않음)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정 잠금 여부를 반환한다.
     *
     * @return 항상 true (잠기지 않음)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 자격 증명 만료 여부를 반환한다.
     *
     * @return 항상 true (만료되지 않음)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 계정 활성화 여부를 반환한다.
     *
     * @return 항상 true (활성화됨)
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}