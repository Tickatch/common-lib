package io.github.tickatch.common.error;

import io.github.tickatch.common.api.ApiResponse;
import io.github.tickatch.common.message.MessageResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GlobalExceptionHandler 단위 테스트.
 */
@DisplayName("GlobalExceptionHandler 테스트")
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageResolver messageResolver;

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageResolver);
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // 기본 stubbing - varargs 처리
        lenient().when(messageResolver.resolve(anyString(), any(Object[].class)))
                .thenReturn("에러 메시지");
    }

    @Test
    @DisplayName("BusinessException을 처리한다")
    void handleBusinessException() {
        // given
        BusinessException e = new BusinessException(GlobalErrorCode.BAD_REQUEST);

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(request, e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("AccessDeniedException을 처리한다")
    void handleAccessDenied() {
        // given
        AccessDeniedException e = new AccessDeniedException("접근 거부");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(request, e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError().getCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("IllegalArgumentException을 처리한다")
    void handleIllegalArgument() {
        // given
        IllegalArgumentException e = new IllegalArgumentException("잘못된 인자");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(request, e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("잘못된 인자");
    }

    @Test
    @DisplayName("IllegalStateException을 처리한다")
    void handleIllegalState() {
        // given
        IllegalStateException e = new IllegalStateException("잘못된 상태");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalState(request, e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("일반 Exception을 500 에러로 처리한다")
    void handleAllExceptions() {
        // given
        Exception e = new RuntimeException("알 수 없는 에러");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleAllExceptions(request, e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("응답에 요청 경로가 포함된다")
    void responseIncludesRequestPath() {
        // given
        request.setRequestURI("/api/users/123");
        Exception e = new RuntimeException("에러");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleAllExceptions(request, e);

        // then
        assertThat(response.getBody().getError().getPath()).isEqualTo("/api/users/123");
    }
}