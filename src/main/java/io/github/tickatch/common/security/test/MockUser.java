package io.github.tickatch.common.security.test;

import io.github.tickatch.common.security.UserType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * 테스트 환경에서 Spring Security의 인증 정보를 설정하기 위한 커스텀 애노테이션.
 *
 * <p>Spring Security의 {@link org.springframework.security.test.context.support.WithSecurityContext}
 * 메커니즘을 활용하여, 테스트 실행 시 지정된 사용자 정보로 SecurityContext를 구성한다.
 *
 * <p>컨트롤러, 서비스, 리포지토리 테스트 등에서 인증이 필요한 로직을 검증할 때 편리하게 사용할 수 있으며,
 * 사용자 ID와 사용자 유형을 자유롭게 설정할 수 있다.
 *
 * <p>사용자 유형({@link UserType})에 따라 자동으로 권한(Role)이 부여된다:
 * <ul>
 *   <li>{@link UserType#CUSTOMER} → {@code ROLE_CUSTOMER}</li>
 *   <li>{@link UserType#SELLER} → {@code ROLE_SELLER}</li>
 *   <li>{@link UserType#ADMIN} → {@code ROLE_ADMIN}</li>
 * </ul>
 *
 * <p>사용 예:
 * <pre>{@code
 * // 기본 사용자 (CUSTOMER)
 * @Test
 * @MockUser(userId = "testUser")
 * void testCustomerAccess() {
 *     // 테스트 코드
 * }
 *
 * // 판매자 권한으로 테스트
 * @Test
 * @MockUser(userId = "seller123", userType = UserType.SELLER)
 * void testSellerAccess() {
 *     // 테스트 코드
 * }
 *
 * // 관리자 권한으로 테스트
 * @Test
 * @MockUser(userId = "admin", userType = UserType.ADMIN)
 * void testAdminAccess() {
 *     // 테스트 코드
 * }
 *
 * // @PreAuthorize와 함께 사용
 * @Test
 * @MockUser(userType = UserType.ADMIN)
 * void testAdminOnlyEndpoint() {
 *     // hasRole('ADMIN') 권한이 필요한 엔드포인트 테스트
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see UserType
 * @see WithMockUserSecurityContextFactory
 * @see io.github.tickatch.common.security.AuthenticatedUser
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserSecurityContextFactory.class)
public @interface MockUser {

  /**
   * 테스트 사용자 ID.
   *
   * <p>기본값은 "testUser"이다.
   *
   * @return 사용자 ID 문자열
   */
  String userId() default "testUser";

  /**
   * 테스트 사용자 유형.
   *
   * <p>기본값은 {@link UserType#CUSTOMER}이다.
   * 설정된 유형에 따라 해당 권한(ROLE_CUSTOMER, ROLE_SELLER, ROLE_ADMIN)이 자동으로 부여된다.
   *
   * @return 사용자 유형 ({@link UserType})
   */
  UserType userType() default UserType.CUSTOMER;
}