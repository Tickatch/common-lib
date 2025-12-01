package io.github.tickatch.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * 사용자 인증 정보를 HTTP Header에서 읽어 Spring Security의 인증 컨텍스트에 설정하는 필터.
 *
 * <p>이 필터는 API Gateway에서 전달한 사용자 ID를 기반으로 애플리케이션 내부에서
 * 인증된 사용자처럼 동작할 수 있도록 {@link SecurityContextHolder}에 인증 정보를 설정한다.
 *
 * <p>처리하는 헤더:
 * <ul>
 *   <li>{@code X-User-Id} — 사용자 ID (필수)</li>
 * </ul>
 *
 * <p>{@code X-User-Id} 헤더가 누락되었거나 유효하지 않은 경우 인증 처리는 수행되지 않으며,
 * 필터는 다음 체인으로 요청을 그대로 전달한다. 이 경우 Spring Security의 기본 동작에 따라
 * 인증이 필요한 엔드포인트에서는 401 응답이 반환된다.
 *
 * <p>사용 방법 - {@link BaseSecurityConfig}를 상속하면 자동으로 적용됨:
 * <pre>{@code
 * @Configuration
 * @EnableWebSecurity
 * public class SecurityConfig extends BaseSecurityConfig {
 *
 *     @Bean
 *     @Override
 *     protected LoginFilter loginFilterBean() {
 *         return new LoginFilter();
 *     }
 *
 *     @Bean
 *     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 *         return build(http);
 *     }
 * }
 * }</pre>
 *
 * <p>인증된 사용자 정보 접근:
 * <pre>{@code
 * // 컨트롤러에서
 * @GetMapping("/me")
 * public UserInfo me(@AuthenticationPrincipal AuthenticatedUser user) {
 *     return userService.findById(user.getUserId());
 * }
 *
 * // 서비스에서
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see AuthenticatedUser
 * @see BaseSecurityConfig
 */
public class LoginFilter extends GenericFilterBean {

    /** 사용자 ID를 전달하는 HTTP 헤더 이름. */
    private static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 요청에서 사용자 인증 정보를 추출하여 SecurityContext에 저장하고, 다음 필터로 요청을 전달한다.
     *
     * @param request 필터 요청 객체
     * @param response 필터 응답 객체
     * @param filterChain 필터 체인
     * @throws IOException 입출력 예외 발생 시
     * @throws ServletException 요청 처리 중 서블릿 예외 발생 시
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        doLogin((HttpServletRequest) request);
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP Header에서 사용자 ID를 추출하여 Authentication 객체로 변환하고
     * Spring Security의 SecurityContext에 설정한다.
     *
     * <p>{@code X-User-Id} 값이 없거나 유효하지 않은 경우 인증 처리를 건너뛴다.
     *
     * @param request 현재 HTTP 요청
     */
    private void doLogin(HttpServletRequest request) {
        String userIdHeader = request.getHeader(HEADER_USER_ID);

        if (!StringUtils.hasText(userIdHeader)) {
            return;
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            // 유효하지 않은 형식의 사용자 ID
            return;
        }

        AuthenticatedUser userDetails = AuthenticatedUser.of(userId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}