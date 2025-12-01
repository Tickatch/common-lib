package io.github.tickatch.common.autoconfig;

import io.github.tickatch.common.jpa.AuditorAwareImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 기능을 자동으로 구성하는 AutoConfiguration.
 *
 * <p>이 설정은 다음 조건을 모두 충족할 때 활성화된다:
 * <ul>
 *   <li>{@link EnableJpaAuditing} 클래스가 클래스패스에 존재할 것 (spring-data-jpa 의존성)</li>
 *   <li>{@code tickatch.jpa.auditing.enabled=true}이거나 설정이 없을 것 (기본 활성화)</li>
 *   <li>사용자가 {@link AuditorAware} 빈을 직접 정의하지 않았을 것</li>
 * </ul>
 *
 * <h2>제공 기능</h2>
 * <ul>
 *   <li>JPA Auditing 자동 활성화 ({@code @EnableJpaAuditing})</li>
 *   <li>{@link AuditorAwareImpl} 빈 자동 등록</li>
 *   <li>{@code @CreatedBy}, {@code @LastModifiedBy} 필드 자동 설정</li>
 * </ul>
 *
 * <h2>비활성화 방법</h2>
 * <pre>{@code
 * # application.yml
 * tickatch:
 *   jpa:
 *     auditing:
 *       enabled: false
 * }</pre>
 *
 * <h2>커스텀 AuditorAware 사용</h2>
 * <p>직접 {@link AuditorAware} 빈을 정의하면 이 AutoConfiguration의 기본 구현은 적용되지 않는다:
 * <pre>{@code
 * @Bean
 * public AuditorAware<String> auditorAware() {
 *     return () -> Optional.of("customAuditor");
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see AuditorAwareImpl
 */
@AutoConfiguration
@EnableJpaAuditing
@ConditionalOnClass(EnableJpaAuditing.class)
@ConditionalOnProperty(
        prefix = "tickatch.jpa.auditing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class JpaAuditingAutoConfiguration {

    /**
     * JPA Auditing을 위한 {@link AuditorAware} 빈을 등록한다.
     *
     * <p>사용자가 직접 {@link AuditorAware} 빈을 정의한 경우 이 빈은 생성되지 않는다.
     *
     * @return {@link AuditorAwareImpl} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}