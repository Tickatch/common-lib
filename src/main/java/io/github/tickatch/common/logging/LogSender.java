package io.github.tickatch.common.logging;

import io.github.tickatch.common.feign.LogRequest;

public interface LogSender {
    void send(LogRequest request);
}
