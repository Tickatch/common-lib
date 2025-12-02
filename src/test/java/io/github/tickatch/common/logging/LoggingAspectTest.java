package io.github.tickatch.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LoggingAspect 단위 테스트.
 */
@DisplayName("LoggingAspect 테스트")
@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock
    private LogManager logManager;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private LoggingAspect loggingAspect;

    @BeforeEach
    void setUp() {
        loggingAspect = new LoggingAspect(logManager);
    }

    @Test
    @DisplayName("LoggingAspect 인스턴스를 생성할 수 있다")
    void constructor_createsInstance() {
        assertThat(loggingAspect).isNotNull();
    }

    @Test
    @DisplayName("logExecution이 메서드를 실행하고 결과를 반환한다")
    void logExecution_proceedsAndReturnsResult() throws Throwable {
        // given
        String expectedResult = "result";
        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringTypeName()).thenReturn("com.example.TestService");
        when(methodSignature.getName()).thenReturn("doSomething");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{});

        // when
        Object result = loggingAspect.logExecution(joinPoint);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("logExecution이 LogManager를 호출한다")
    void logExecution_callsLogManager() throws Throwable {
        // given
        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringTypeName()).thenReturn("TestService");
        when(methodSignature.getName()).thenReturn("method");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{});

        // when
        loggingAspect.logExecution(joinPoint);

        // then
        verify(logManager).logMethodEntry(eq("TestService.method"), anyString());
        verify(logManager).logMethodExit(eq("TestService.method"), anyString());
    }

    @Test
    @DisplayName("logExecution이 파라미터 정보를 포함한다")
    void logExecution_includesParameters() throws Throwable {
        // given
        when(joinPoint.proceed()).thenReturn(null);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 123});
        when(methodSignature.getDeclaringTypeName()).thenReturn("TestService");
        when(methodSignature.getName()).thenReturn("method");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"param1", "param2"});

        // when
        loggingAspect.logExecution(joinPoint);

        // then
        verify(logManager).logMethodEntry(eq("TestService.method"), contains("param1"));
    }
}