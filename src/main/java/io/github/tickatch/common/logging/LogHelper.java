package io.github.tickatch.common.logging;

import io.github.tickatch.common.feign.LogRequest;
import io.github.tickatch.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 서비스 내에서 이벤트 로그를 기록하기 위한 헬퍼 클래스.
 *
 * <p>로그 기록은 LogService로 Feign 전송되며, 공통 MDC 정보(ipAddress, userId, traceId)는
 * MdcFilter를 통해 자동으로 수집된다.
 *
 * <p>사용자는 비즈니스 로직에서 성공/실패 이벤트 발생 시 본 헬퍼의 메서드를 호출하여
 * 표준화된 로그를 전송할 수 있다.
 *
 * <h2>사용 방법</h2>
 *
 * <h3>1. 단순 성공 로그 기록</h3>
 * <pre>{@code
 * logHelper.success("TICKET", "CREATE", detailObject);
 * }</pre>
 *
 * <p>resourceId, deviceInfo가 필요 없는 경우 사용한다.</p>
 *
 * <h3>2. deviceInfo 포함 성공 로그 기록 (Auth-Service 등에서 사용)</h3>
 * <pre>{@code
 * logHelper.success("AUTH", "LOGIN", detailObject, userId.toString(), deviceInfo);
 * }</pre>
 *
 * <p>resourceId와 deviceInfo를 함께 전달해야 할 경우 사용한다.</p>
 *
 * <h3>3. 실패 로그 기록</h3>
 * <pre>{@code
 * logHelper.fail("PAYMENT", "REQUEST", detailObject);
 * }</pre>
 *
 * <p>실패 이벤트를 기록하며, resourceId/deviceInfo는 null로 기록된다.</p>
 *
 * <h2>MDC에서 자동 수집되는 정보</h2>
 * <ul>
 *   <li>userId — MdcFilter에서 X-User-Id 헤더 기반 수집</li>
 *   <li>ipAddress — 요청의 실제 클라이언트 IP</li>
 *   <li>requestId(traceId) — 분산 추적용 ID</li>
 * </ul>
 *
 * <h2>직접 전달해야 하는 정보</h2>
 * <ul>
 *   <li>detail — 로그 상세 내용 (자동 JSON 직렬화)</li>
 *   <li>resourceId — 작업 대상 엔티티 식별자</li>
 *   <li>deviceInfo — Auth에서만 제공되는 디바이스 정보</li>
 * </ul>
 *
 * @author Tickatch
 * @since 0.0.1
 */
@Component
@RequiredArgsConstructor
public class LogHelper {

    private final LogSender logSender;

    @Value("${spring.application.name}")
    private String serviceName;


    /**
    * 단순 성공 로그 기록 (resourceId, deviceInfo 없음)
    */
    public void success(String category, String type, Object detail) {
        send(category, type, "SUCCESS", detail, null, null);
    }


    /**
    * 성공 로그 기록 (resourceId + deviceInfo 전달)
    *
    * @param category 이벤트 대분류 (예: AUTH, ORDER, PAYMENT)
    * @param type 이벤트 상세 타입 (예: LOGIN, CREATE, CANCEL)
    * @param detail 비즈니스 상세 정보 객체 (JSON 직렬화됨)
    * @param resourceId 이벤트가 발생한 대상 엔티티 ID
    * @param deviceInfo 디바이스 정보
    */
    public void success(String category, String type, Object detail, String resourceId, String deviceInfo) {
        send(category, type, "SUCCESS", detail, resourceId, deviceInfo);
    }

  /**
   * 실패 로그 기록 (resourceId, deviceInfo 없음)
   */
    public void fail(String category, String type, Object detail) {
        send(category, type, "FAIL", detail, null, null);
    }


  /**
   * 내부 공통 send 메서드.
   * MDC 값(userId, ipAddress, requestId)은 자동 포함된다.
   */
    private void send(
            String category,
            String type,
            String actionType,
            Object detail,
            String resourceId,
            String deviceInfo
    ) {

        LogRequest request = new LogRequest(
            category,
            type,
            actionType,
            JsonUtils.toJson(detail),           // 자동 JSON 직렬화
            deviceInfo,
            MdcUtils.getUserId(),               // MDC 자동 수집
            resourceId,
            MdcUtils.get("ipAddress"),          // MDC 자동 수집
            MdcUtils.getRequestId(),            // traceId 자동
            serviceName                         // 현재 서비스명
        );

        logSender.send(request);
    }
}
