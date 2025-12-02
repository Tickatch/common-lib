package io.github.tickatch.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LoginFilter 단위 테스트.
 */
@DisplayName("LoginFilter 테스트")
class LoginFilterTest {

    private LoginFilter loginFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        loginFilter = new LoginFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========================================
    // 정상 인증 테스트
    // ========================================

    @Nested
    @DisplayName("정상 인증 테스트")
    class SuccessfulAuthenticationTest {

        @Test
        @DisplayName("X-User-Id 헤더가 있으면 인증을 설정한다")
        void doFilter_withValidUserId_setsAuthentication() throws ServletException, IOException {
            // given
            String userId = UUID.randomUUID().toString();
            request.addHeader("X-User-Id", userId);

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();
        }

        @Test
        @DisplayName("인증된 사용자의 principal은 AuthenticatedUser이다")
        void doFilter_withValidUserId_principalIsAuthenticatedUser() throws ServletException, IOException {
            // given
            request.addHeader("X-User-Id", "test-user-id");

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        }

        @Test
        @DisplayName("AuthenticatedUser에서 userId를 올바르게 추출한다")
        void doFilter_withValidUserId_extractsUserId() throws ServletException, IOException {
            // given
            String userId = "usr-98765-xyz";
            request.addHeader("X-User-Id", userId);

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            assertThat(user.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("UUID 형식의 userId를 처리한다")
        void doFilter_withUuidUserId_extractsUserId() throws ServletException, IOException {
            // given
            String userId = "550e8400-e29b-41d4-a716-446655440000";
            request.addHeader("X-User-Id", userId);

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            assertThat(user.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("credentials는 null이다")
        void doFilter_withValidUserId_credentialsIsNull() throws ServletException, IOException {
            // given
            request.addHeader("X-User-Id", "user-id");

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.getCredentials()).isNull();
        }

        @Test
        @DisplayName("authorities는 빈 컬렉션이다")
        void doFilter_withValidUserId_authoritiesIsEmpty() throws ServletException, IOException {
            // given
            request.addHeader("X-User-Id", "user-id");

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.getAuthorities()).isEmpty();
        }
    }

    // ========================================
    // 필터 체인 테스트
    // ========================================

    @Nested
    @DisplayName("필터 체인 테스트")
    class FilterChainTest {

        @Test
        @DisplayName("인증 후 필터 체인을 계속 진행한다")
        void doFilter_withValidUserId_continuesFilterChain() throws ServletException, IOException {
            // given
            request.addHeader("X-User-Id", "user-id");

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("인증 실패해도 필터 체인을 계속 진행한다")
        void doFilter_withoutUserId_continuesFilterChain() throws ServletException, IOException {
            // given - 헤더 없음

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }
    }

    // ========================================
    // 인증 건너뛰기 케이스 테스트
    // ========================================

    @Nested
    @DisplayName("인증 건너뛰기 케이스 테스트")
    class SkipAuthenticationTest {

        @Test
        @DisplayName("X-User-Id 헤더가 없으면 인증을 설정하지 않는다")
        void doFilter_withoutUserId_noAuthentication() throws ServletException, IOException {
            // given - 헤더 없음

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();
        }

        @Test
        @DisplayName("X-User-Id가 빈 문자열이면 인증을 설정하지 않는다")
        void doFilter_withEmptyUserId_noAuthentication() throws ServletException, IOException {
            // given
            request.addHeader("X-User-Id", "");

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();
        }

        @Test
        @DisplayName("X-User-Id가 공백만 있으면 인증을 설정하지 않는다")
        void doFilter_withWhitespaceUserId_noAuthentication() throws ServletException, IOException {
            // given
            request.addHeader("X-User-Id", "   ");

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();
        }
    }

    // ========================================
    // 다양한 userId 값 테스트
    // ========================================

    @Nested
    @DisplayName("다양한 userId 값 테스트")
    class VariousUserIdTest {

        @Test
        @DisplayName("숫자로만 구성된 userId를 처리한다")
        void doFilter_withNumericUserId() throws ServletException, IOException {
            // given
            request.addHeader("X-User-Id", "12345");

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            assertThat(user.getUserId()).isEqualTo("12345");
        }

        @Test
        @DisplayName("특수문자가 포함된 userId를 처리한다")
        void doFilter_withSpecialCharactersUserId() throws ServletException, IOException {
            // given
            request.addHeader("X-User-Id", "user-123_test@domain");

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            assertThat(user.getUserId()).isEqualTo("user-123_test@domain");
        }

        @Test
        @DisplayName("매우 긴 userId를 처리한다")
        void doFilter_withLongUserId() throws ServletException, IOException {
            // given
            String longUserId = "a".repeat(500);
            request.addHeader("X-User-Id", longUserId);

            // when
            loginFilter.doFilter(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            assertThat(user.getUserId()).hasSize(500);
        }
    }

    // ========================================
    // GenericFilterBean 상속 테스트
    // ========================================

    @Nested
    @DisplayName("GenericFilterBean 상속 테스트")
    class GenericFilterBeanTest {

        @Test
        @DisplayName("LoginFilter는 GenericFilterBean을 상속한다")
        void extendsGenericFilterBean() {
            // then
            assertThat(loginFilter).isInstanceOf(org.springframework.web.filter.GenericFilterBean.class);
        }
    }
}