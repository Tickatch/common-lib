package io.github.tickatch.common.logging;

import io.github.tickatch.common.feign.LogRequest;
import io.github.tickatch.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 실패 이벤트 전용 로그 헬퍼.
 *
 * <p> LogService에는 실패 이벤트만 전송한다.
 *
 * <p>공통 MDC 정보(userId, ipAddress, traceId)는 MdcFilter를 통해 자동 수집된다.
 */
@Component
@RequiredArgsConstructor
public class LogHelper {

  private static final String ACTION_FAIL = "FAIL";

  private final LogSender logSender;

  @Value("${spring.application.name}")
  private String serviceName;

  /**
   * 실패 로그 기록.
   *
   * @param category 이벤트 대분류 (AUTH, PAYMENT, RESERVATION 등)
   * @param type 이벤트 타입 (LOGIN, CREATE, CANCEL 등)
   * @param detail 실패 원인 및 컨텍스트 정보
   */
  public void fail(String category, String type, Object detail) {
    sendFail(category, type, detail, null, null);
  }

  /**
   * 실패 로그 기록 (resourceId 포함).
   */
  public void fail(String category, String type, Object detail, String resourceId) {
    sendFail(category, type, detail, resourceId, null);
  }

  /**
   * 실패 로그 기록 (resourceId + deviceInfo 포함).
   */
  public void fail(
      String category,
      String type,
      Object detail,
      String resourceId,
      String deviceInfo
  ) {
    sendFail(category, type, detail, resourceId, deviceInfo);
  }

  /**
   * 내부 공통 전송 메서드.
   */
  private void sendFail(
      String category,
      String type,
      Object detail,
      String resourceId,
      String deviceInfo
  ) {
    LogRequest request = new LogRequest(
        category,
        type,
        ACTION_FAIL,
        JsonUtils.toJson(detail),
        deviceInfo,
        MdcUtils.getUserId(),
        resourceId,
        MdcUtils.get("ipAddress"),
        MdcUtils.getRequestId(),
        serviceName
    );

    logSender.send(request);
  }
}
