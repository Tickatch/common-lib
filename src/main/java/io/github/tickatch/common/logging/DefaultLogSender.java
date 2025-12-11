package io.github.tickatch.common.logging;

import io.github.tickatch.common.feign.LogClient;
import io.github.tickatch.common.feign.LogRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultLogSender implements LogSender {

    private final LogClient logClient;

    @Override
    public void send(LogRequest request) {
        try {
            logClient.sendLog(request);
        } catch (Exception e) {
            // 로그 서버 장애 시 서비스 영향 없게 해야 함
            System.err.println("LogService unreachable: " + e.getMessage());
        }
    }
}
