package io.github.tickatch.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MdcFilter 단위 테스트.
 */
@DisplayName("MdcFilter 테스트")
@ExtendWith(MockitoExtension.class)
class MdcFilterTest {

  private MdcFilter mdcFilter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    mdcFilter = new MdcFilter();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  // ========================================
  // 상수 테스트
  // ========================================

  @Nested
  @DisplayName("상수 테스트")
  class ConstantsTest {

    @Test
    @DisplayName("HEADER_TRACE_ID 값을 확인한다")
    void headerTraceId_constant() {
      assertThat(MdcFilter.HEADER_TRACE_ID).isEqualTo("X-Trace-Id");
    }

    @Test
    @DisplayName("HEADER_USER_ID 값을 확인한다")
    void headerUserId_constant() {
      assertThat(MdcFilter.HEADER_USER_ID).isEqualTo("X-User-Id");
    }
  }

  // ========================================
  // Trace ID 테스트
  // ========================================

  @Nested
  @DisplayName("Trace ID 처리 테스트")
  class TraceIdTest {

    @Test
    @DisplayName("X-Trace-Id 헤더가 있으면 해당 값을 사용한다")
    void doFilterInternal_withTraceIdHeader_usesHeaderValue() throws ServletException, IOException {
      // given
      String expectedTraceId = "incoming-trace-id";
      request.addHeader("X-Trace-Id", expectedTraceId);
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedTraceId.set(MdcUtils.getRequestId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedTraceId.get()).isEqualTo(expectedTraceId);
    }

    @Test
    @DisplayName("X-Trace-Id 헤더가 없으면 새 UUID를 생성한다")
    void doFilterInternal_withoutTraceIdHeader_generatesNewUuid() throws ServletException, IOException {
      // given
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedTraceId.set(MdcUtils.getRequestId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedTraceId.get()).isNotNull();
      assertThatCode(() -> UUID.fromString(capturedTraceId.get())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빈 X-Trace-Id 헤더는 새 UUID를 생성한다")
    void doFilterInternal_withEmptyTraceIdHeader_generatesNewUuid() throws ServletException, IOException {
      // given
      request.addHeader("X-Trace-Id", "");
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedTraceId.set(MdcUtils.getRequestId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedTraceId.get()).isNotEmpty();
      assertThatCode(() -> UUID.fromString(capturedTraceId.get())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공백만 있는 X-Trace-Id 헤더는 새 UUID를 생성한다")
    void doFilterInternal_withWhitespaceTraceIdHeader_generatesNewUuid() throws ServletException, IOException {
      // given
      request.addHeader("X-Trace-Id", "   ");
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedTraceId.set(MdcUtils.getRequestId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedTraceId.get()).isNotBlank();
      assertThatCode(() -> UUID.fromString(capturedTraceId.get())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("응답 헤더에 X-Trace-Id를 포함한다")
    void doFilterInternal_setsTraceIdInResponseHeader() throws ServletException, IOException {
      // given
      String traceId = "response-trace-id";
      request.addHeader("X-Trace-Id", traceId);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(response.getHeader("X-Trace-Id")).isEqualTo(traceId);
    }

    @Test
    @DisplayName("새로 생성된 traceId도 응답 헤더에 포함된다")
    void doFilterInternal_setsGeneratedTraceIdInResponseHeader() throws ServletException, IOException {
      // given - 헤더 없음

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(response.getHeader("X-Trace-Id")).isNotNull();
      assertThatCode(() -> UUID.fromString(response.getHeader("X-Trace-Id"))).doesNotThrowAnyException();
    }
  }

  // ========================================
  // User ID 테스트
  // ========================================

  @Nested
  @DisplayName("User ID 처리 테스트")
  class UserIdTest {

    @Test
    @DisplayName("X-User-Id 헤더가 있으면 MDC에 저장한다")
    void doFilterInternal_withUserIdHeader_setsToMdc() throws ServletException, IOException {
      // given
      String userId = "user-123-456";
      request.addHeader("X-User-Id", userId);
      AtomicReference<String> capturedUserId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedUserId.set(MdcUtils.getUserId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedUserId.get()).isEqualTo(userId);
    }

    @Test
    @DisplayName("X-User-Id 헤더가 없으면 MDC에 저장하지 않는다")
    void doFilterInternal_withoutUserIdHeader_doesNotSetToMdc() throws ServletException, IOException {
      // given
      AtomicReference<String> capturedUserId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedUserId.set(MdcUtils.getUserId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedUserId.get()).isNull();
    }

    @Test
    @DisplayName("빈 X-User-Id 헤더는 MDC에 저장하지 않는다")
    void doFilterInternal_withEmptyUserIdHeader_doesNotSetToMdc() throws ServletException, IOException {
      // given
      request.addHeader("X-User-Id", "");
      AtomicReference<String> capturedUserId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedUserId.set(MdcUtils.getUserId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedUserId.get()).isNull();
    }

    @Test
    @DisplayName("공백만 있는 X-User-Id 헤더는 MDC에 저장하지 않는다")
    void doFilterInternal_withWhitespaceUserIdHeader_doesNotSetToMdc() throws ServletException, IOException {
      // given
      request.addHeader("X-User-Id", "   ");
      AtomicReference<String> capturedUserId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedUserId.set(MdcUtils.getUserId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedUserId.get()).isNull();
    }
  }

  // ========================================
  // MDC 클리어 테스트
  // ========================================

  @Nested
  @DisplayName("MDC 클리어 테스트")
  class MdcClearTest {

    @Test
    @DisplayName("필터 실행 후 MDC가 클리어된다")
    void doFilterInternal_clearsMdcAfterExecution() throws ServletException, IOException {
      // given
      request.addHeader("X-Trace-Id", "trace-123");
      request.addHeader("X-User-Id", "user-456");

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(MdcUtils.getRequestId()).isNull();
      assertThat(MdcUtils.getUserId()).isNull();
    }

    @Test
    @DisplayName("예외 발생 시에도 MDC가 클리어된다")
    void doFilterInternal_clearsMdcOnException() throws ServletException, IOException {
      // given
      request.addHeader("X-Trace-Id", "trace-123");
      doThrow(new RuntimeException("테스트 예외")).when(filterChain).doFilter(request, response);

      // when & then
      assertThatThrownBy(() -> mdcFilter.doFilterInternal(request, response, filterChain))
          .isInstanceOf(RuntimeException.class);

      assertThat(MdcUtils.getRequestId()).isNull();
    }

    @Test
    @DisplayName("ServletException 발생 시에도 MDC가 클리어된다")
    void doFilterInternal_clearsMdcOnServletException() throws ServletException, IOException {
      // given
      request.addHeader("X-Trace-Id", "trace-123");
      doThrow(new ServletException("서블릿 예외")).when(filterChain).doFilter(request, response);

      // when & then
      assertThatThrownBy(() -> mdcFilter.doFilterInternal(request, response, filterChain))
          .isInstanceOf(ServletException.class);

      assertThat(MdcUtils.getRequestId()).isNull();
    }
  }

  // ========================================
  // 통합 시나리오 테스트
  // ========================================

  @Nested
  @DisplayName("통합 시나리오 테스트")
  class IntegrationScenarioTest {

    @Test
    @DisplayName("Feign 호출에서 전달받은 헤더를 MDC에 저장한다")
    void feignScenario_propagatesHeaders() throws ServletException, IOException {
      // given - Feign에서 전달받은 헤더
      String traceId = "feign-trace-id";
      String userId = "feign-user-id";
      request.addHeader("X-Trace-Id", traceId);
      request.addHeader("X-User-Id", userId);
      AtomicReference<String> capturedTraceId = new AtomicReference<>();
      AtomicReference<String> capturedUserId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedTraceId.set(MdcUtils.getRequestId());
        capturedUserId.set(MdcUtils.getUserId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedTraceId.get()).isEqualTo(traceId);
      assertThat(capturedUserId.get()).isEqualTo(userId);
    }

    @Test
    @DisplayName("첫 요청은 새 traceId를 생성한다")
    void firstRequest_generatesNewTraceId() throws ServletException, IOException {
      // given - 헤더 없음 (첫 요청)
      AtomicReference<String> capturedTraceId = new AtomicReference<>();

      doAnswer(invocation -> {
        capturedTraceId.set(MdcUtils.getRequestId());
        return null;
      }).when(filterChain).doFilter(request, response);

      // when
      mdcFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(capturedTraceId.get()).isNotNull();
      assertThatCode(() -> UUID.fromString(capturedTraceId.get())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여러 요청이 각각 다른 traceId를 가진다")
    void multipleRequests_haveDifferentTraceIds() throws ServletException, IOException {
      // given
      MockHttpServletRequest request1 = new MockHttpServletRequest();
      MockHttpServletRequest request2 = new MockHttpServletRequest();
      MockHttpServletResponse response1 = new MockHttpServletResponse();
      MockHttpServletResponse response2 = new MockHttpServletResponse();

      // when
      mdcFilter.doFilterInternal(request1, response1, filterChain);
      String traceId1 = response1.getHeader("X-Trace-Id");

      mdcFilter.doFilterInternal(request2, response2, filterChain);
      String traceId2 = response2.getHeader("X-Trace-Id");

      // then
      assertThat(traceId1).isNotNull();
      assertThat(traceId2).isNotNull();
      assertThat(traceId1).isNotEqualTo(traceId2);
    }
  }
}