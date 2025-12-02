package io.github.tickatch.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청 단위로 MDC(Mapped Diagnostic Context)를 초기화하고 관리하는 필터.
 *
 * <p>각 HTTP 요청마다 고유한 requestId를 생성하고, 헤더에서 사용자 ID를 추출하여 MDC에 저장한다.
 * 이를 통해 로그 추적성(Traceability)을 향상시킬 수 있다.
 *
 * <p>{@link OncePerRequestFilter}를 상속하여 요청당 한 번만 실행되며,
 * 요청 처리가 완료된 후에는 MDC를 반드시 초기화하여 메모리 누수 및 정보 오염을 방지한다.
 *
 * <p>처리하는 헤더:
 * <ul>
 *   <li>{@code X-User-Id} — 사용자 ID (API Gateway에서 전달, UUID 문자열)</li>
 * </ul>
 *
 * <p>로그 출력 예시 (logback.xml 패턴 설정 시):
 * <pre>
 * 2025-01-15 10:30:00.123 [http-nio-8080-exec-1] [abc-123-def] [550e8400-e29b-41d4-a716-446655440000] INFO  c.e.TicketController - 티켓 조회
 * </pre>
 *
 * <p>사용 방법 - 각 서비스에서 빈으로 등록:
 * <pre>{@code
 * @Configuration
 * public class FilterConfig {
 *
 *     @Bean
 *     public FilterRegistrationBean<MdcFilter> mdcFilter() {
 *         FilterRegistrationBean<MdcFilter> registrationBean = new FilterRegistrationBean<>();
 *         registrationBean.setFilter(new MdcFilter());
 *         registrationBean.addUrlPatterns("/*");
 *         registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
 *         return registrationBean;
 *     }
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 * @see MdcUtils
 */
public class MdcFilter extends OncePerRequestFilter {

    /** 사용자 ID를 전달하는 HTTP 헤더 이름. */
    private static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 요청마다 requestId와 userId를 MDC에 저장하고, 요청 처리가 끝나면 MDC를 초기화한다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 필터 처리 중 서블릿 예외 발생 시
     * @throws IOException 필터 처리 중 I/O 예외 발생 시
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 고유 요청 ID 생성 및 저장
            String requestId = UUID.randomUUID().toString();
            MdcUtils.setRequestId(requestId);

            // 헤더에서 사용자 ID 추출 및 저장
            String userId = extractUserId(request);
            if (userId != null) {
                MdcUtils.setUserId(userId);
            }

            filterChain.doFilter(request, response);
        } finally {
            // 메모리 누수 방지를 위해 MDC 클리어
            MDC.clear();
        }
    }

    /**
     * HTTP 헤더에서 사용자 ID를 추출한다.
     *
     * @param request HTTP 요청
     * @return 사용자 ID (UUID 문자열), 헤더가 없거나 빈 문자열이면 null
     */
    private String extractUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader(HEADER_USER_ID);

        if (!StringUtils.hasText(userIdHeader)) {
            return null;
        }

        return userIdHeader;
    }
}