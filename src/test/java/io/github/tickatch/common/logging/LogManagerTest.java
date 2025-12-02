package io.github.tickatch.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.*;

/**
 * LogManager 단위 테스트.
 */
@DisplayName("LogManager 테스트")
class LogManagerTest {

    private LogManager logManager;

    @BeforeEach
    void setUp() {
        logManager = new LogManager();
        MdcUtils.setRequestId("test-request-id");
        MdcUtils.setUserId("test-user-id");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("logControllerEntry가 예외 없이 실행된다")
    void logControllerEntry_executesWithoutException() {
        assertThatCode(() ->
                logManager.logControllerEntry("GET", "/api/test", "TestController.test", "Params: {}")
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logControllerExit가 예외 없이 실행된다")
    void logControllerExit_executesWithoutException() {
        assertThatCode(() ->
                logManager.logControllerExit("GET", "/api/test", "TestController.test", "{\"result\":\"ok\"}")
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logMethodEntry가 예외 없이 실행된다")
    void logMethodEntry_executesWithoutException() {
        assertThatCode(() ->
                logManager.logMethodEntry("TestService.doSomething", "Params: {id=1}")
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logMethodExit가 예외 없이 실행된다")
    void logMethodExit_executesWithoutException() {
        assertThatCode(() ->
                logManager.logMethodExit("TestService.doSomething", "{\"success\":true}")
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logException이 예외 없이 실행된다")
    void logException_executesWithoutException() {
        assertThatCode(() ->
                logManager.logException(new RuntimeException("테스트 예외"))
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logException with context가 예외 없이 실행된다")
    void logException_withContext_executesWithoutException() {
        assertThatCode(() ->
                logManager.logException("TestService.doSomething", new RuntimeException("테스트 예외"))
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MDC 값이 없어도 예외 없이 실행된다")
    void logMethods_withoutMdc_executesWithoutException() {
        // given
        MDC.clear();

        // when & then
        assertThatCode(() -> {
            logManager.logControllerEntry("POST", "/api/test", "Controller.method", "{}");
            logManager.logControllerExit("POST", "/api/test", "Controller.method", "{}");
            logManager.logMethodEntry("Service.method", "{}");
            logManager.logMethodExit("Service.method", "{}");
            logManager.logException(new RuntimeException("error"));
        }).doesNotThrowAnyException();
    }
}