package io.github.tickatch.common.security.test;

import io.github.tickatch.common.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * {@link MockUser} 애노테이션 기반으로 테스트 실행 시 SecurityContext를 생성하는 팩토리 클래스.
 *
 * <p>Spring Security 테스트 프레임워크의 {@link
 * org.springframework.security.test.context.support.WithSecurityContextFactory} 인터페이스를 구현하여, 테스트
 * 메서드 실행 전에 인증 정보를 SecurityContext에 설정한다.
 *
 * <p>주어진 {@link MockUser} 애노테이션의 속성(userId)을 기반으로 {@link
 * AuthenticatedUser} 객체(UserDetails 구현체)를 생성하고, 이를 principal로 가지는 {@link
 * UsernamePasswordAuthenticationToken} 인증 객체를 구성한다.
 *
 * <p>이 팩토리는 @MockUser 애노테이션과 함께 사용되며, 인증이 필요한 테스트를 간편하게 구성하는 데 도움을 준다.
 *
 * @see MockUser
 * @see org.springframework.security.test.context.support.WithSecurityContextFactory
 * @see org.springframework.security.core.context.SecurityContext
 */
public class WithMockUserSecurityContextFactory implements WithSecurityContextFactory<MockUser> {

  /**
   * 주어진 {@link MockUser} 정보를 바탕으로 SecurityContext를 생성한다.
   *
   * @param user 테스트용 사용자 정보가 포함된 애노테이션 인스턴스
   * @return 설정된 인증 정보(Authentication)가 포함된 SecurityContext
   */
  @Override
  public SecurityContext createSecurityContext(MockUser user) {
    UserDetails userDetails =
        AuthenticatedUser.of(user.userId());

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);

    return context;
  }
}
