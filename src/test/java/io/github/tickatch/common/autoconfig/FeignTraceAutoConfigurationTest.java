package io.github.tickatch.common.autoconfig;

import feign.RequestTemplate;
import io.github.tickatch.common.logging.MdcFilter;
import io.github.tickatch.common.logging.MdcUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * FeignTraceAutoConfiguration 단위 테스트.
 */
@DisplayName("FeignTraceAutoConfiguration 테스트")
class FeignTraceAutoConfigurationTest {

  private FeignTraceAutoConfiguration configuration;
  private FeignTraceAutoConfiguration.MdcPropagationInterceptor interceptor;

  @BeforeEach
  void setUp() {
    configuration = new FeignTraceAutoConfiguration();
    interceptor = (FeignTraceAutoConfiguration.MdcPropagationInterceptor)
        configuration.mdcPropagationInterceptor();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  // ========================================
  // 빈 생성 테스트
  // ========================================

  @Nested
  @DisplayName("빈 생성 테스트")
  class BeanCreationTest {

    @Test
    @DisplayName("mdcPropagationInterceptor 빈을 생성한다")
    void createsMdcPropagationInterceptor() {
      // when
      var interceptor = configuration.mdcPropagationInterceptor();

      // then
      assertThat(interceptor).isNotNull();
      assertThat(interceptor).isInstanceOf(FeignTraceAutoConfiguration.MdcPropagationInterceptor.class);
    }
  }

  // ========================================
  // Trace ID 전파 테스트
  // ========================================

  @Nested
  @DisplayName("Trace ID 전파 테스트")
  class TraceIdPropagationTest {

    @Test
    @DisplayName("MDC에 requestId가 있으면 X-Trace-Id 헤더에 추가한다")
    void apply_withRequestId_addsTraceIdHeader() {
      // given
      String traceId = UUID.randomUUID().toString();
      MdcUtils.setRequestId(traceId);
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      Collection<String> traceIdHeaders = template.headers().get(MdcFilter.HEADER_TRACE_ID);
      assertThat(traceIdHeaders).isNotNull();
      assertThat(traceIdHeaders).containsExactly(traceId);
    }

    @Test
    @DisplayName("MDC에 requestId가 없으면 X-Trace-Id 헤더를 추가하지 않는다")
    void apply_withoutRequestId_doesNotAddTraceIdHeader() {
      // given
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      assertThat(template.headers().get(MdcFilter.HEADER_TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("MDC에 빈 문자열 requestId가 있으면 헤더를 추가하지 않는다")
    void apply_withEmptyRequestId_doesNotAddTraceIdHeader() {
      // given
      MDC.put(MdcUtils.REQUEST_ID, "");
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      assertThat(template.headers().get(MdcFilter.HEADER_TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("MDC에 공백만 있는 requestId가 있으면 헤더를 추가하지 않는다")
    void apply_withWhitespaceRequestId_doesNotAddTraceIdHeader() {
      // given
      MDC.put(MdcUtils.REQUEST_ID, "   ");
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      assertThat(template.headers().get(MdcFilter.HEADER_TRACE_ID)).isNull();
    }
  }

  // ========================================
  // User ID 전파 테스트
  // ========================================

  @Nested
  @DisplayName("User ID 전파 테스트")
  class UserIdPropagationTest {

    @Test
    @DisplayName("MDC에 userId가 있으면 X-User-Id 헤더에 추가한다")
    void apply_withUserId_addsUserIdHeader() {
      // given
      String userId = "user-123-456";
      MdcUtils.setUserId(userId);
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      Collection<String> userIdHeaders = template.headers().get(MdcFilter.HEADER_USER_ID);
      assertThat(userIdHeaders).isNotNull();
      assertThat(userIdHeaders).containsExactly(userId);
    }

    @Test
    @DisplayName("MDC에 userId가 없으면 X-User-Id 헤더를 추가하지 않는다")
    void apply_withoutUserId_doesNotAddUserIdHeader() {
      // given
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      assertThat(template.headers().get(MdcFilter.HEADER_USER_ID)).isNull();
    }

    @Test
    @DisplayName("MDC에 빈 문자열 userId가 있으면 헤더를 추가하지 않는다")
    void apply_withEmptyUserId_doesNotAddUserIdHeader() {
      // given
      MDC.put(MdcUtils.USER_ID, "");
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      assertThat(template.headers().get(MdcFilter.HEADER_USER_ID)).isNull();
    }
  }

  // ========================================
  // 통합 시나리오 테스트
  // ========================================

  @Nested
  @DisplayName("통합 시나리오 테스트")
  class IntegrationScenarioTest {

    @Test
    @DisplayName("requestId와 userId가 모두 있으면 두 헤더 모두 추가된다")
    void apply_withBothIds_addsBothHeaders() {
      // given
      String traceId = UUID.randomUUID().toString();
      String userId = "user-abc-123";
      MdcUtils.setRequestId(traceId);
      MdcUtils.setUserId(userId);
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      assertThat(template.headers().get(MdcFilter.HEADER_TRACE_ID)).containsExactly(traceId);
      assertThat(template.headers().get(MdcFilter.HEADER_USER_ID)).containsExactly(userId);
    }

    @Test
    @DisplayName("requestId만 있으면 X-Trace-Id 헤더만 추가된다")
    void apply_withOnlyRequestId_addsOnlyTraceIdHeader() {
      // given
      String traceId = UUID.randomUUID().toString();
      MdcUtils.setRequestId(traceId);
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      assertThat(template.headers().get(MdcFilter.HEADER_TRACE_ID)).containsExactly(traceId);
      assertThat(template.headers().get(MdcFilter.HEADER_USER_ID)).isNull();
    }

    @Test
    @DisplayName("userId만 있으면 X-User-Id 헤더만 추가된다")
    void apply_withOnlyUserId_addsOnlyUserIdHeader() {
      // given
      String userId = "user-only";
      MdcUtils.setUserId(userId);
      RequestTemplate template = new RequestTemplate();

      // when
      interceptor.apply(template);

      // then
      assertThat(template.headers().get(MdcFilter.HEADER_TRACE_ID)).isNull();
      assertThat(template.headers().get(MdcFilter.HEADER_USER_ID)).containsExactly(userId);
    }

    @Test
    @DisplayName("분산 추적 시나리오: 전체 흐름")
    void distributedTracingScenario() {
      // given - API Gateway에서 시작된 요청
      String originalTraceId = UUID.randomUUID().toString();
      String userId = "user-123";
      MdcUtils.setRequestId(originalTraceId);
      MdcUtils.setUserId(userId);

      // when - Service A에서 Service B로 Feign 호출
      RequestTemplate templateToServiceB = new RequestTemplate();
      interceptor.apply(templateToServiceB);

      // then - 헤더에 정보가 포함됨
      assertThat(templateToServiceB.headers().get("X-Trace-Id")).containsExactly(originalTraceId);
      assertThat(templateToServiceB.headers().get("X-User-Id")).containsExactly(userId);

      // when - Service B에서 Service C로 Feign 호출 (같은 MDC 상태)
      RequestTemplate templateToServiceC = new RequestTemplate();
      interceptor.apply(templateToServiceC);

      // then - 동일한 traceId 전파
      assertThat(templateToServiceC.headers().get("X-Trace-Id")).containsExactly(originalTraceId);
    }

    @Test
    @DisplayName("기존 헤더가 있어도 덮어쓴다")
    void apply_overwritesExistingHeaders() {
      // given
      String newTraceId = "new-trace-id";
      MdcUtils.setRequestId(newTraceId);
      RequestTemplate template = new RequestTemplate();
      template.header("X-Trace-Id", "old-trace-id");

      // when
      interceptor.apply(template);

      // then - 새 값이 추가됨 (Feign은 멀티밸류 헤더)
      Collection<String> traceIdHeaders = template.headers().get("X-Trace-Id");
      assertThat(traceIdHeaders).contains(newTraceId);
    }
  }
}