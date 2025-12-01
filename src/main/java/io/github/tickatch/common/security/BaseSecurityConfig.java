package io.github.tickatch.common.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 공통적으로 적용할 Spring Security 보안 설정의 기반 클래스.
 *
 * <p>이 클래스는 템플릿 메서드 패턴(Template Method Pattern)을 기반으로 설계되어 있으며,
 * 하위 보안 설정 클래스가 공통적인 필터 체인 구성 로직은 그대로 사용하면서,
 * 특정 지점(예: 로그인 필터 정의, 엔드포인트 접근 정책, 예외 처리)을 필요에 따라 확장할 수 있도록 한다.
 *
 * <h2>핵심 목적</h2>
 * <ul>
 *   <li>Stateless 기반 JWT 인증 구조를 간단히 구성하기 위한 공통 보안 설정 제공</li>
 *   <li>사용자 정의 {@link LoginFilter}를 필터 체인에 등록하기 위한 확장 포인트 제공</li>
 *   <li>401(UNAUTHORIZED), 403(FORBIDDEN) 등 인증/인가 실패 상황에 대한 기본 예외 처리 제공</li>
 *   <li>세션을 사용하지 않는 REST API 환경에 적합한 기본 정책 제공</li>
 * </ul>
 *
 * <h2>기본 제공 기능</h2>
 * <ul>
 *   <li>CSRF 비활성화</li>
 *   <li>세션 생성 정책을 {@link SessionCreationPolicy#STATELESS}로 설정</li>
 *   <li>하위 클래스에서 제공한 {@link LoginFilter}를 {@link UsernamePasswordAuthenticationFilter} 이전에 필터 체인에 삽입</li>
 *   <li>기본적으로 Swagger, API 문서, 헬스 체크 등 인증이 필요 없는 공용 엔드포인트 허용</li>
 *   <li>401/403 상황에 대한 기본적인 HTTP 상태 코드 응답 처리</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
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
 *
 *     // 선택적: 엔드포인트별 접근 제어 커스터마이징
 *     @Override
 *     protected Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>
 *             .AuthorizationManagerRequestMatcherRegistry> authorizeHttpRequests() {
 *         return registry -> registry
 *             .requestMatchers("/public/**").permitAll()
 *             .requestMatchers("/admin/**").hasRole("ADMIN")
 *             .anyRequest().authenticated();
 *     }
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see LoginFilter
 * @see AuthenticatedUser
 */
@Slf4j
public abstract class BaseSecurityConfig {

    /**
     * 로그인(인증) 처리를 담당하는 커스텀 {@link LoginFilter} 빈을 반환한다.
     *
     * <p>하위 클래스는 이 메서드를 구현하여 실제 인증 로직을 수행하는
     * {@link LoginFilter} 인스턴스를 반환해야 한다.
     *
     * @return 인증 처리를 담당하는 {@link LoginFilter} 인스턴스
     */
    protected abstract LoginFilter loginFilterBean();

    /**
     * {@link HttpSecurity}를 사용하여 {@link SecurityFilterChain}을 구성하고 빌드한다.
     *
     * <p>기본 구성:
     * <ul>
     *   <li>CSRF 비활성화</li>
     *   <li>하위 클래스가 제공한 {@link LoginFilter}를 {@link UsernamePasswordAuthenticationFilter} 전에 등록</li>
     *   <li>세션 생성 정책을 {@link SessionCreationPolicy#STATELESS}로 설정</li>
     *   <li>인증/인가 관련 커스터마이저 적용</li>
     * </ul>
     *
     * @param http {@link HttpSecurity} 구성 객체
     * @return 구성된 {@link SecurityFilterChain}
     * @throws Exception Spring Security 구성 중 발생할 수 있는 예외
     */
    public SecurityFilterChain build(HttpSecurity http) throws Exception {
        http.csrf(CsrfConfigurer::disable)
                .addFilterBefore(loginFilterBean(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeHttpRequests())
                .exceptionHandling(exceptionHandlingCustomizer());

        return http.build();
    }

    /**
     * HTTP 요청별 인가(Authorization) 규칙을 정의한다.
     *
     * <p>기본적으로 Swagger, API 문서 등 공용 엔드포인트는 인증 없이 접근을 허용하고,
     * 그 외 모든 요청은 인증을 요구한다.
     *
     * <p>하위 클래스에서 오버라이드하여 커스텀 접근 정책을 정의할 수 있다.
     *
     * @return 인가 규칙을 설정하는 {@link Customizer}
     */
    protected Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>
    authorizeHttpRequests() {

        return registry -> registry
                // 기본적으로 허용할 엔드포인트
                .requestMatchers(defaultPermitAllPaths()).permitAll()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated();
    }

    /**
     * 인증 없이 허용할 기본 경로 목록을 반환한다.
     *
     * <p>Swagger, API 문서, 헬스 체크 등 인증이 필요 없는 공용 엔드포인트를 포함한다.
     * 하위 클래스에서 오버라이드하여 허용 경로를 추가하거나 변경할 수 있다.
     *
     * @return permitAll 대상 경로 배열
     */
    protected String[] defaultPermitAllPaths() {
        return new String[]{
                // Swagger UI
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/swagger-resources/**",
                // Actuator 헬스 체크
                "/actuator/health",
                "/actuator/info"
        };
    }

    /**
     * 인증/인가 예외 처리(예: 401, 403) 동작을 커스터마이즈할 {@link Customizer}를 반환한다.
     *
     * <p>기본 구현은 {@link #authenticationEntryPoint()}와 {@link #accessDeniedHandler()}를
     * 사용하여 인증 실패 및 접근 거부 상황에 대한 기본 응답을 지정한다.
     *
     * @return {@link ExceptionHandlingConfigurer} 구성을 위한 {@link Customizer}
     */
    protected Customizer<ExceptionHandlingConfigurer<HttpSecurity>> exceptionHandlingCustomizer() {
        return handler -> {
            handler.authenticationEntryPoint(authenticationEntryPoint());
            handler.accessDeniedHandler(accessDeniedHandler());
        };
    }

    /**
     * 인증이 필요한 요청에서 인증이 되어있지 않을 때 호출되는 {@link AuthenticationEntryPoint}를 반환한다.
     *
     * <p>기본 구현은 HTTP 401 (Unauthorized) 상태 코드를 전송한다.
     * 하위 클래스에서 오버라이드하여 JSON 응답 등을 반환하도록 변경할 수 있다.
     *
     * @return 인증 실패 시 동작할 {@link AuthenticationEntryPoint}
     */
    protected AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.debug("인증 실패: {} - {}", authException.getClass().getSimpleName(), authException.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        };
    }

    /**
     * 인증은 되었지만 권한이 부족한 요청(접근 거부)에 대해 처리할 {@link AccessDeniedHandler}를 반환한다.
     *
     * <p>기본 구현은 HTTP 403 (Forbidden) 상태 코드를 전송한다.
     * 하위 클래스에서 오버라이드하여 JSON 응답 등을 반환하도록 변경할 수 있다.
     *
     * @return 접근 거부 시 동작할 {@link AccessDeniedHandler}
     */
    protected AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.debug("접근 거부: {} - {}", accessDeniedException.getClass().getSimpleName(), accessDeniedException.getMessage());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        };
    }
}