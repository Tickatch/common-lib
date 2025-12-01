package io.github.tickatch.common.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Swagger/OpenAPI 공통 설정 클래스.
 *
 * <p>springdoc-openapi를 기반으로 다음 기능을 제공한다:
 * <ul>
 *   <li>기본 OpenAPI 메타 정보(title, description 등) 설정</li>
 *   <li>JWT Bearer 인증 스키마 정의 및 전역 보안 요구사항 추가</li>
 *   <li>Gateway prefix 경로 변환 (선택적)</li>
 * </ul>
 *
 * <h2>설정 예시 (application.yml)</h2>
 * <pre>{@code
 * openapi:
 *   service:
 *     url: https://api.tickatch.io
 *     title: Ticket Service API
 *     description: 티켓 서비스 API 문서
 *     path-prefix: /v1/tickets  # Gateway prefix (선택)
 * }</pre>
 *
 * <h2>커스텀 설정</h2>
 * <p>이 클래스를 상속하거나, 직접 OpenAPI 빈을 정의하여 커스터마이징할 수 있다:
 * <pre>{@code
 * @Bean
 * public OpenAPI customOpenAPI() {
 *     return new OpenAPI()
 *         .info(new Info().title("Custom API"));
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 */
public class SwaggerConfig {

    @Value("${openapi.service.url:http://localhost:8080}")
    private String serviceUrl;

    @Value("${openapi.service.title:Tickatch API}")
    private String title;

    @Value("${openapi.service.description:API Documentation}")
    private String description;

    @Value("${openapi.service.path-prefix:}")
    private String pathPrefix;

    /**
     * 기본 OpenAPI 정보와 JWT 보안 설정을 구성한다.
     *
     * <p>다음 설정을 수행한다:
     * <ul>
     *   <li>Server URL 설정</li>
     *   <li>HTTP Bearer(JWT) 타입의 SecurityScheme 등록</li>
     *   <li>Bearer 스키마를 전역 SecurityRequirement로 추가</li>
     *   <li>문서 제목 및 설명 설정</li>
     * </ul>
     *
     * @return 구성된 {@link OpenAPI} 스펙 객체
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url(serviceUrl)))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "Bearer",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("JWT 토큰을 입력하세요 (Bearer 접두어 불필요)")
                                )
                )
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version("v1")
                );
    }

    /**
     * Gateway prefix를 API 경로에 추가하는 커스터마이저.
     *
     * <p>{@code openapi.service.path-prefix} 설정이 있는 경우에만 동작한다.
     * 설정이 없거나 빈 문자열인 경우 경로 변환을 수행하지 않는다.
     *
     * <p>동작 방식:
     * <ul>
     *   <li>현재 등록된 모든 path에 prefix를 추가</li>
     *   <li>원본 경로(서비스 내부 경로)는 제거</li>
     *   <li>이미 prefix가 붙은 경로는 중복 추가하지 않음</li>
     * </ul>
     *
     * <p>예시 ({@code path-prefix: /v1/tickets}):
     * <ul>
     *   <li>컨트롤러 경로: {@code /api/events}</li>
     *   <li>문서 노출 경로: {@code /v1/tickets/api/events}</li>
     * </ul>
     *
     * @return OpenAPI paths를 변환하는 {@link OpenApiCustomizer}
     */
    @Bean
    public OpenApiCustomizer addPrefixToPaths() {
        return openApi -> {
            // prefix가 설정되지 않은 경우 변환하지 않음
            if (pathPrefix == null || pathPrefix.isBlank()) {
                return;
            }

            Paths paths = openApi.getPaths();
            if (paths == null) {
                return;
            }

            // 기존 paths를 복사하여 prefix를 붙인 새로운 경로 추가
            Map<String, PathItem> original = new LinkedHashMap<>(paths);
            for (String path : original.keySet()) {
                String prefixed = pathPrefix + path;
                if (!paths.containsKey(prefixed)) {
                    PathItem item = original.get(path);
                    paths.addPathItem(prefixed, item);
                }
            }

            // prefix가 없는 내부 경로 제거, Gateway 경로만 남김
            for (String path : original.keySet()) {
                if (!path.startsWith(pathPrefix)) {
                    paths.remove(path);
                }
            }
        };
    }
}