package io.github.tickatch.common.autoconfig;

import io.github.tickatch.common.logging.MdcUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.UUID;

/**
 * @Scheduled 메서드 실행 시 자동으로 traceId를 생성하는 AutoConfiguration.
 *
 * <p>스케줄러나 배치 작업에서 Feign 호출 시에도 분산 추적이 가능하도록 한다.
 *
 * <p>활성화 조건:
 * <ul>
 *   <li>{@link Aspect} 클래스가 클래스패스에 존재할 것 (spring-boot-starter-aop 의존성)</li>
 * </ul>
 *
 * @author Tickatch
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnClass(Aspect.class)
public class ScheduledTraceAutoConfiguration {

  @Bean
  public ScheduledTraceAspect scheduledTraceAspect() {
    return new ScheduledTraceAspect();
  }

  /**
   * @Scheduled 메서드 실행 전후로 MDC traceId를 관리하는 Aspect.
   */
  @Aspect
  static class ScheduledTraceAspect {

    /**
     * @Scheduled 메서드 실행 시 traceId를 자동 생성한다.
     *
     * <p>이미 MDC에 traceId가 있으면 (이벤트 컨텍스트 등) 그대로 유지한다.
     */
    @Around("@annotation(scheduled)")
    public Object aroundScheduled(ProceedingJoinPoint joinPoint, Scheduled scheduled) throws Throwable {
      boolean mdcWasEmpty = !MdcUtils.hasRequestId();

      try {
        // MDC에 traceId가 없으면 새로 생성
        if (mdcWasEmpty) {
          MdcUtils.setRequestId(UUID.randomUUID().toString());
        }
        return joinPoint.proceed();
      } finally {
        // 이 메서드에서 생성한 경우에만 클리어
        if (mdcWasEmpty) {
          MDC.clear();
        }
      }
    }
  }
}
