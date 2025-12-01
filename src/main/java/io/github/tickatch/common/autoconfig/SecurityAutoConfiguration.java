package io.github.tickatch.common.autoconfig;

import io.github.tickatch.common.security.BaseSecurityConfig;
import io.github.tickatch.common.security.LoginFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 기본 설정을 자동으로 구성하는 AutoConfiguration.
 *
 * <p>이 설정은 다음 조건을 모두 충족할 때 활성화된다:
 * <ul>
 *   <li>{@link SecurityFilterChain} 클래스가 클래스패스에 존재할 것 (spring-security 의존성)</li>
 *   <li>사용자가 {@link SecurityFilterChain} 빈을 직접 정의하지 않았을 것</li>
 *   <li>Servlet 기반 Web Application일 것</li>
 * </ul>
 *
 * <h2>제공 기능</h2>
 * <ul>
 *   <li>CSRF 비활성화</li>
 *   <li>Stateless 세션 정책</li>
 *   <li>{@link LoginFilter} - X-User-Id 헤더 기반 인증</li>
 *   <li>Swagger, Actuator 등 공용 엔드포인트 허용</li>
 *   <li>{@code @PreAuthorize} 등 메서드 보안 활성화</li>
 * </ul>
 *
 * <h2>커스텀 Security 설정</h2>
 * <p>직접 {@link SecurityFilterChain} 빈을 정의하면 이 AutoConfiguration은 비활성화된다:
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
 *     @Override
 *     protected Customizer<...> authorizeHttpRequests() {
 *         return registry -> registry
 *             .requestMatchers("/admin/**").hasRole("ADMIN")
 *             .anyRequest().authenticated();
 *     }
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see BaseSecurityConfig
 * @see LoginFilter
 */
@AutoConfiguration
@EnableMethodSecurity
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnMissingBean(SecurityFilterChain.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityAutoConfiguration extends BaseSecurityConfig {

    /**
     * 기본 {@link LoginFilter} 빈을 등록한다.
     *
     * <p>X-User-Id 헤더에서 사용자 ID를 추출하여 SecurityContext에 설정한다.
     * 사용자가 직접 {@link LoginFilter} 빈을 정의한 경우 이 빈은 생성되지 않는다.
     *
     * @return {@link LoginFilter} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public LoginFilter loginFilter() {
        return new LoginFilter();
    }

    /**
     * {@link BaseSecurityConfig}에서 요구하는 로그인 필터를 반환한다.
     *
     * @return 자동 구성된 {@link LoginFilter} 빈
     */
    @Override
    protected LoginFilter loginFilterBean() {
        return loginFilter();
    }

    /**
     * 기본 Security 필터 체인을 구성한다.
     *
     * <p>이 빈은 사용자가 별도로 {@link SecurityFilterChain} 빈을 제공하지 않았을 때만 등록된다.
     *
     * @param http {@link HttpSecurity} 보안 구성 객체
     * @return 기본 {@link SecurityFilterChain}
     * @throws Exception 보안 구성 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return build(http);
    }
}