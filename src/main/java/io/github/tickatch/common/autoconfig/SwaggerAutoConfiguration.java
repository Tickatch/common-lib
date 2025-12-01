package io.github.tickatch.common.autoconfig;

import io.github.tickatch.common.swagger.SwaggerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Swagger/OpenAPI 설정을 자동으로 구성하는 AutoConfiguration.
 *
 * <p>이 설정은 다음 조건을 모두 충족할 때 활성화된다:
 * <ul>
 *   <li>springdoc-openapi 라이브러리가 클래스패스에 존재할 것</li>
 *   <li>Servlet 기반 Web Application일 것</li>
 *   <li>{@code tickatch.swagger.enabled=true}이거나 설정이 없을 것 (기본 활성화)</li>
 *   <li>사용자가 {@link SwaggerConfig} 빈을 직접 정의하지 않았을 것</li>
 * </ul>
 *
 * <h2>제공 기능</h2>
 * <ul>
 *   <li>OpenAPI 기본 정보 설정</li>
 *   <li>JWT Bearer 인증 스키마</li>
 *   <li>Gateway prefix 경로 변환 (설정 시)</li>
 * </ul>
 *
 * <h2>설정 예시</h2>
 * <pre>{@code
 * # application.yml
 * openapi:
 *   service:
 *     url: https://api.tickatch.io
 *     title: Ticket Service API
 *     description: 티켓 서비스 API 문서
 *     path-prefix: /v1/tickets
 * }</pre>
 *
 * <h2>비활성화 방법</h2>
 * <pre>{@code
 * tickatch:
 *   swagger:
 *     enabled: false
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see SwaggerConfig
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        prefix = "tickatch.swagger",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SwaggerAutoConfiguration {

    /**
     * 기본 {@link SwaggerConfig} 빈을 등록한다.
     *
     * <p>사용자가 직접 {@link SwaggerConfig} 빈을 정의한 경우 이 빈은 생성되지 않는다.
     *
     * @return {@link SwaggerConfig} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public SwaggerConfig swaggerConfig() {
        return new SwaggerConfig();
    }
}