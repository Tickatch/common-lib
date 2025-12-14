package io.github.tickatch.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security 인증 모델을 표현하는 사용자 정보 클래스.
 *
 * <p>API Gateway에서 전달된 사용자 ID와 사용자 유형을 기반으로 애플리케이션 내부에서
 * 인증된 사용자로 동작할 수 있도록 구현된 {@link UserDetails} 구현체이다.
 *
 * <p>이 클래스는 최소한의 정보(userId, userType)만 포함하며, 필요에 따라 확장 가능하다.
 * 비밀번호 기반 인증을 사용하지 않으므로 {@link #getPassword()}는 빈 문자열을 반환한다.
 *
 * <p>사용자 유형({@link UserType})에 따라 자동으로 권한(Role)이 부여된다:
 * <ul>
 *   <li>{@link UserType#CUSTOMER} → {@code ROLE_CUSTOMER}</li>
 *   <li>{@link UserType#SELLER} → {@code ROLE_SELLER}</li>
 *   <li>{@link UserType#ADMIN} → {@code ROLE_ADMIN}</li>
 * </ul>
 *
 * <p>사용 예시:
 * <pre>{@code
 * // 컨트롤러에서 현재 사용자 조회
 * @GetMapping("/me")
 * public UserInfo getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
 *     String userId = user.getUserId();
 *     UserType userType = user.getUserType();
 *
 *     if (userType.isAdmin()) {
 *         // 관리자 전용 로직
 *     }
 *
 *     return userService.findById(userId);
 * }
 *
 * // SecurityContext에서 직접 조회
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
 * String userId = user.getUserId();
 * UserType userType = user.getUserType();
 *
 * // @PreAuthorize를 사용한 권한 기반 접근 제어
 * @PreAuthorize("hasRole('ADMIN')")
 * @GetMapping("/admin/dashboard")
 * public AdminDashboard adminDashboard() { ... }
 *
 * @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
 * @GetMapping("/seller/stats")
 * public SellerStats sellerStats() { ... }
 * }</pre>
 *
 * @param userId 사용자 ID (API Gateway에서 전달된 UUID 문자열)
 * @param userType 사용자 유형 (API Gateway에서 전달된 {@link UserType})
 * @author Tickatch
 * @since 0.0.1
 * @see UserType
 * @see LoginFilter
 * @see BaseSecurityConfig
 */
public record AuthenticatedUser(String userId, UserType userType) implements UserDetails {

  /**
   * {@link AuthenticatedUser} 객체를 생성하는 정적 팩토리 메서드.
   *
   * @param userId 사용자 ID (UUID 문자열)
   * @param userType 사용자 유형 ({@link UserType} enum)
   * @return 생성된 {@link AuthenticatedUser} 인스턴스
   */
  public static AuthenticatedUser of(String userId, UserType userType) {
    return new AuthenticatedUser(userId, userType);
  }

  /**
   * 문자열 기반 사용자 유형으로 {@link AuthenticatedUser} 객체를 생성하는 정적 팩토리 메서드.
   *
   * <p>API Gateway에서 전달된 문자열 형태의 사용자 유형을 {@link UserType} enum으로 변환한다.
   * 변환에 실패하거나 null/빈 문자열인 경우 userType은 null로 설정된다.
   *
   * @param userId 사용자 ID (UUID 문자열)
   * @param userType 사용자 유형 (문자열, 예: "CUSTOMER", "SELLER", "ADMIN")
   * @return 생성된 {@link AuthenticatedUser} 인스턴스
   */
  public static AuthenticatedUser of(String userId, String userType) {
    UserType type = parseUserType(userType);
    return new AuthenticatedUser(userId, type);
  }

  /**
   * 사용자 ID만으로 {@link AuthenticatedUser} 객체를 생성하는 정적 팩토리 메서드.
   *
   * <p>하위 호환성을 위해 제공되며, userType은 null로 설정된다.
   *
   * @param userId 사용자 ID (UUID 문자열)
   * @return 생성된 {@link AuthenticatedUser} 인스턴스 (userType은 null)
   * @deprecated userType을 함께 전달하는 {@link #of(String, UserType)} 또는
   *             {@link #of(String, String)} 사용을 권장
   */
  @Deprecated
  public static AuthenticatedUser of(String userId) {
    return new AuthenticatedUser(userId, null);
  }

  /**
   * 문자열 형태의 사용자 유형을 {@link UserType} enum으로 변환한다.
   *
   * <p>null이거나 빈 문자열인 경우, 또는 유효하지 않은 값인 경우 null을 반환한다.
   *
   * @param userType 사용자 유형 문자열
   * @return 변환된 {@link UserType} 또는 null
   */
  private static UserType parseUserType(String userType) {
    if (userType == null || userType.isBlank()) {
      return null;
    }
    try {
      return UserType.valueOf(userType);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * 사용자 ID를 반환한다.
   *
   * @return 사용자 ID (UUID 문자열)
   */
  public String getUserId() {
    return userId;
  }

  /**
   * 사용자 유형을 반환한다.
   *
   * @return 사용자 유형 ({@link UserType}) 또는 null
   */
  public UserType getUserType() {
    return userType;
  }

  /**
   * 사용자 권한 목록을 반환한다.
   *
   * <p>사용자 유형({@link UserType})에 따라 자동으로 권한이 부여된다:
   * <ul>
   *   <li>{@link UserType#CUSTOMER} → {@code ROLE_CUSTOMER}</li>
   *   <li>{@link UserType#SELLER} → {@code ROLE_SELLER}</li>
   *   <li>{@link UserType#ADMIN} → {@code ROLE_ADMIN}</li>
   * </ul>
   *
   * <p>userType이 null인 경우 빈 목록을 반환한다.
   *
   * @return 사용자 권한 목록 (userType이 null이면 빈 목록)
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    if (userType == null) {
      return Collections.emptyList();
    }
    return List.of(new SimpleGrantedAuthority("ROLE_" + userType.name()));
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
   * <p>사용자 ID를 그대로 반환한다.
   *
   * @return 사용자 ID 문자열
   */
  @Override
  public String getUsername() {
    return userId;
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