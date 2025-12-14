package io.github.tickatch.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 유형을 정의하는 열거형.
 *
 * <p>API Gateway에서 JWT 토큰의 userType 클레임을 기반으로 사용자 유형을 구분한다.
 * 동일한 이메일로 서로 다른 유형의 계정을 분리하여 가입할 수 있다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * // 컨트롤러에서 사용자 유형 확인
 * @GetMapping("/dashboard")
 * public Object dashboard(@AuthenticationPrincipal AuthenticatedUser user) {
 *     if (user.getUserType().isAdmin()) {
 *         return adminService.getDashboard();
 *     } else if (user.getUserType().isSeller()) {
 *         return sellerService.getDashboard();
 *     }
 *     return customerService.getDashboard();
 * }
 *
 * // enum 직접 비교
 * if (user.getUserType() == UserType.SELLER) {
 *     // 판매자 전용 로직
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see AuthenticatedUser
 * @see LoginFilter
 */
@Getter
@RequiredArgsConstructor
public enum UserType {

  /** 일반 구매자 */
  CUSTOMER("구매자"),

  /** 판매자 */
  SELLER("판매자"),

  /** 관리자 */
  ADMIN("관리자");

  /** 유형 설명 */
  private final String description;

  /**
   * 구매자인지 확인한다.
   *
   * @return {@link #CUSTOMER}이면 true, 그 외 false
   */
  public boolean isCustomer() {
    return this == CUSTOMER;
  }

  /**
   * 판매자인지 확인한다.
   *
   * @return {@link #SELLER}이면 true, 그 외 false
   */
  public boolean isSeller() {
    return this == SELLER;
  }

  /**
   * 관리자인지 확인한다.
   *
   * @return {@link #ADMIN}이면 true, 그 외 false
   */
  public boolean isAdmin() {
    return this == ADMIN;
  }
}