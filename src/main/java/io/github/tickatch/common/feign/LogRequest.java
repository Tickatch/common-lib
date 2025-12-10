package io.github.tickatch.common.feign;

public record LogRequest(
    String eventCategory,
    String eventType,
    String actionType,
    String eventDetail,
    String deviceInfo,
    String userId,
    String resourceId,
    String ipAddress,
    String traceId,
    String serviceName
) {}
