package io.github.tickatch.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "log-service")
public interface LogClient {
    @PostMapping("/api/v1/event-logs")
    void sendLog(LogRequest request);
}
