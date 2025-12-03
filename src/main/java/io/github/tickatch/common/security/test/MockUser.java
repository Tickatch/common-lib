package io.github.tickatch.common.security.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * 테스트 환경에서 Spring Security의 인증 정보를 설정하기 위한 커스텀 애노테이션.
 *
 * <p>Spring Security의 {@link org.springframework.security.test.context.support.WithSecurityContext}
 * 메커니즘을 활용하여, 테스트 실행 시 지정된 사용자 정보로 SecurityContext 를 구성한다.
 *
 * <p>컨트롤러, 서비스, 리포지토리 테스트 등에서 인증이 필요한 로직을 검증할 때 편리하게 사용할 수 있으며, 속성을 자유롭게 설정할 수 있다.
 *
 * <p>사용 예:
 *
 * <pre>
 * {@code
 *   @Test
 *   @MockUser(userId = "testUser")
 *   void testMasterAccess() {
 *     // 테스트 코드
 *   }
 * }
 *
 * @see WithMockUserSecurityContextFactory
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserSecurityContextFactory.class)
public @interface MockUser {

  /** 테스트 사용자 ID. 기본값은 testUser. */
  String userId() default "testUser";
}




