package io.github.tickatch.common.security.test;

import io.github.tickatch.common.security.AuthenticatedUser;
import io.github.tickatch.common.security.UserType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * {@link MockUser} 애노테이션 기반으로 테스트 실행 시 SecurityContext를 생성하는 팩토리 클래스.
 *
 * <p>Spring Security 테스트 프레임워크의 {@link WithSecurityContextFactory} 인터페이스를 구현하여,
 * 테스트 메서드 실행 전에 인증 정보를 SecurityContext에 설정한다.
 *
 * <p>주어진 {@link MockUser} 애노테이션의 속성(userId, userType)을 기반으로
 * {@link AuthenticatedUser} 객체(UserDetails 구현체)를 생성하고,
 * 이를 principal로 가지는 {@link UsernamePasswordAuthenticationToken} 인증 객체를 구성한다.
 *
 * <p>사용자 유형({@link UserType})에 따라 자동으로 권한(Role)이 부여된다:
 * <ul>
 *   <li>{@link UserType#CUSTOMER} → {@code ROLE_CUSTOMER}</li>
 *   <li>{@link UserType#SELLER} → {@code ROLE_SELLER}</li>
 *   <li>{@link UserType#ADMIN} → {@code ROLE_ADMIN}</li>
 * </ul>
 *
 * <p>이 팩토리는 {@link MockUser} 애노테이션과 함께 사용되며,
 * 인증이 필요한 테스트를 간편하게 구성하는 데 도움을 준다.
 *
 * <p>사용 예:
 * <pre>{@code
 * // 판매자 권한으로 테스트
 * @Test
 * @MockUser(userId = "seller123", userType = UserType.SELLER)
 * void testSellerEndpoint() {
 *     // SecurityContext에 ROLE_SELLER 권한을 가진 사용자가 설정됨
 *     // @PreAuthorize("hasRole('SELLER')") 엔드포인트 테스트 가능
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see MockUser
 * @see UserType
 * @see AuthenticatedUser
 * @see WithSecurityContextFactory
 * @see SecurityContext
 */
public class WithMockUserSecurityContextFactory implements WithSecurityContextFactory<MockUser> {

  /**
   * 주어진 {@link MockUser} 정보를 바탕으로 SecurityContext를 생성한다.
   *
   * <p>애노테이션에서 userId와 userType을 추출하여 {@link AuthenticatedUser} 객체를 생성하고,
   * 이를 포함하는 인증 객체(Authentication)를 SecurityContext에 설정한다.
   *
   * <p>생성된 {@link AuthenticatedUser}는 userType에 따라 적절한 권한(GrantedAuthority)을 가지며,
   * 이를 통해 {@code @PreAuthorize} 등의 권한 기반 접근 제어 테스트가 가능하다.
   *
   * @param user 테스트용 사용자 정보가 포함된 애노테이션 인스턴스
   * @return 설정된 인증 정보(Authentication)가 포함된 SecurityContext
   */
  @Override
  public SecurityContext createSecurityContext(MockUser user) {
    UserDetails userDetails = AuthenticatedUser.of(user.userId(), user.userType());

    Authentication authentication = new UsernamePasswordAuthenticationToken(
        userDetails,
        "",
        userDetails.getAuthorities()
    );

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);

    return context;
  }
}