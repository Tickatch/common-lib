package io.github.tickatch.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import static org.assertj.core.api.Assertions.*;

/**
 * BaseSecurityConfig 단위 테스트.
 */
@DisplayName("BaseSecurityConfig 테스트")
class BaseSecurityConfigTest {

    private TestSecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new TestSecurityConfig();
    }

    @Test
    @DisplayName("defaultPermitAllPaths에 Swagger 경로가 포함된다")
    void defaultPermitAllPaths_containsSwaggerPaths() {
        // when
        String[] paths = securityConfig.defaultPermitAllPaths();

        // then
        assertThat(paths).contains(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
        );
    }

    @Test
    @DisplayName("defaultPermitAllPaths에 Actuator 경로가 포함된다")
    void defaultPermitAllPaths_containsActuatorPaths() {
        // when
        String[] paths = securityConfig.defaultPermitAllPaths();

        // then
        assertThat(paths).contains(
                "/actuator/health",
                "/actuator/info"
        );
    }

    @Test
    @DisplayName("authenticationEntryPoint가 null이 아니다")
    void authenticationEntryPoint_notNull() {
        // when
        AuthenticationEntryPoint entryPoint = securityConfig.authenticationEntryPoint();

        // then
        assertThat(entryPoint).isNotNull();
    }

    @Test
    @DisplayName("accessDeniedHandler가 null이 아니다")
    void accessDeniedHandler_notNull() {
        // when
        AccessDeniedHandler handler = securityConfig.accessDeniedHandler();

        // then
        assertThat(handler).isNotNull();
    }

    /**
     * 테스트용 구현체
     */
    static class TestSecurityConfig extends BaseSecurityConfig {
        @Override
        protected LoginFilter loginFilterBean() {
            return new LoginFilter();
        }
    }
}