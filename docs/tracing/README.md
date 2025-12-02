# 분산 추적 (Distributed Tracing) 가이드

이 문서는 Tickatch MSA 환경에서 요청 흐름을 추적하는 방법을 설명합니다.

## 목차

- [개요](#개요)
- [핵심 개념](#핵심-개념)
- [트리거별 traceId 생성](#트리거별-traceid-생성)
- [API 통신 추적 (HTTP/Feign)](#api-통신-추적-httpfeign)
- [메시지 통신 추적 (RabbitMQ)](#메시지-통신-추적-rabbitmq)
- [스케줄러/배치 추적](#스케줄러배치-추적)
- [혼합 시나리오](#혼합-시나리오)
- [로그 설정](#로그-설정)
- [트러블슈팅](#트러블슈팅)

---

## 개요

분산 시스템에서 하나의 요청이 여러 서비스를 거치면서 처리됩니다. 문제가 발생했을 때 전체 흐름을 추적하려면 **동일한 식별자(traceId)**가 모든 서비스의 로그에 남아야 합니다.

```
사용자 요청 → API Gateway → Order Service → Payment Service → Ticket Service
                  │              │               │               │
               [abc-123]     [abc-123]       [abc-123]       [abc-123]
                  │              │               │               │
                  └──────────────┴───────────────┴───────────────┘
                              동일한 traceId로 전체 흐름 추적
```

---

## 핵심 개념

### TraceId란?

- 하나의 비즈니스 요청을 식별하는 고유 ID (UUID)
- 요청의 시작점에서 생성되어 모든 하위 서비스로 전파
- 로그 검색 시 이 ID로 전체 흐름 조회 가능

### 관련 컴포넌트

| 컴포넌트 | 역할 |
|---------|------|
| `MdcFilter` | HTTP 요청 시 traceId 생성/수신 및 MDC 저장 |
| `MdcUtils` | MDC 읽기/쓰기 유틸리티 |
| `FeignTraceAutoConfiguration` | Feign 호출 시 traceId 헤더 전파 |
| `ScheduledTraceAutoConfiguration` | @Scheduled 메서드 실행 시 traceId 자동 생성 |
| `IntegrationEvent` | 이벤트 발행 시 traceId 자동 포함 |
| `EventContext` | 이벤트 수신 시 traceId MDC 복원 |

---

## 트리거별 traceId 생성

모든 시작점에서 traceId가 자동으로 관리됩니다.

| 트리거 | 담당 컴포넌트 | 동작 |
|--------|-------------|------|
| HTTP 요청 (최초) | `MdcFilter` | 새 traceId 생성 |
| HTTP 요청 (전파) | `MdcFilter` | X-Trace-Id 헤더에서 수신 |
| Feign 호출 | `FeignTraceAutoConfiguration` | X-Trace-Id 헤더로 전파 |
| 이벤트 수신 | `EventContext.run()` | 이벤트에서 traceId 복원 |
| 이벤트 발행 | `IntegrationEvent.from()` | MDC에서 traceId 자동 추출 |
| @Scheduled | `ScheduledTraceAutoConfiguration` | 새 traceId 자동 생성 |
| 수동 (배치 등) | `EventContext.runWithNewTrace()` | 개발자가 명시적 호출 |

### 전체 흐름도

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              시작점 (Trigger)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  [HTTP 요청]              [@Scheduled]              [이벤트 수신]            │
│       │                        │                         │                  │
│       ▼                        ▼                         ▼                  │
│   MdcFilter            ScheduledTraceAspect       EventContext.run()        │
│   (자동)                    (자동)                    (수동 호출)            │
│       │                        │                         │                  │
│       └────────────────────────┴─────────────────────────┘                  │
│                                │                                            │
│                                ▼                                            │
│                    MDC: requestId = "abc-123"                               │
│                                │                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                              전파 (Propagation)                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                │                                            │
│              ┌─────────────────┼─────────────────┐                          │
│              ▼                 ▼                 ▼                          │
│         Feign 호출        이벤트 발행         로그 출력                      │
│              │                 │                 │                          │
│              ▼                 ▼                 ▼                          │
│      X-Trace-Id 헤더    traceId 필드      [abc-123] ...                     │
│          자동 추가         자동 포함                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## API 통신 추적 (HTTP/Feign)

### 흐름도

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. 클라이언트 요청                                                          │
│     POST /orders                                                            │
│     (X-Trace-Id 헤더 없음)                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  2. Order Service - MdcFilter                                               │
│                                                                             │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ X-Trace-Id 헤더 확인                                             │    │
│     │   → 없음 → UUID 생성: "abc-123-def-456"                          │    │
│     │   → MDC.put("requestId", "abc-123-def-456")                      │    │
│     │   → Response Header에 X-Trace-Id 추가                            │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│     로그: [abc-123-def-456] 주문 생성 요청 수신                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  3. Order Service - Feign Client 호출                                       │
│                                                                             │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ FeignTraceAutoConfiguration.MdcPropagationInterceptor           │    │
│     │   → MDC에서 requestId 조회: "abc-123-def-456"                    │    │
│     │   → Request Header에 X-Trace-Id: abc-123-def-456 추가            │    │
│     │   → Request Header에 X-User-Id: user-789 추가 (있는 경우)         │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│     로그: [abc-123-def-456] Payment Service 호출                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │  HTTP Request
                                      │  Headers:
                                      │    X-Trace-Id: abc-123-def-456
                                      │    X-User-Id: user-789
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  4. Payment Service - MdcFilter                                             │
│                                                                             │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ X-Trace-Id 헤더 확인                                             │    │
│     │   → 있음 → 헤더 값 사용: "abc-123-def-456"                        │    │
│     │   → MDC.put("requestId", "abc-123-def-456")                      │    │
│     │   → 새로 생성하지 않음!                                           │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│     로그: [abc-123-def-456] 결제 처리 시작                                   │
│     로그: [abc-123-def-456] 결제 완료                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  5. 응답 반환                                                                │
│                                                                             │
│     Response Headers:                                                       │
│       X-Trace-Id: abc-123-def-456                                          │
│                                                                             │
│     클라이언트도 traceId 확인 가능 (디버깅용)                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 코드 예시

```java
// Order Service - Controller
@PostMapping("/orders")
public OrderResponse createOrder(@RequestBody OrderRequest request) {
    // MdcFilter가 이미 traceId를 MDC에 설정한 상태
    log.info("주문 생성 요청: {}", request);  // [abc-123-def-456] 주문 생성 요청: ...
    
    // Feign 호출 - 자동으로 X-Trace-Id 헤더 전파됨
    PaymentResponse payment = paymentClient.process(request.getPaymentInfo());
    
    log.info("결제 완료: {}", payment);  // [abc-123-def-456] 결제 완료: ...
    return OrderResponse.success(order);
}

// Payment Service - Controller  
@PostMapping("/payments")
public PaymentResponse process(@RequestBody PaymentInfo info) {
    // 동일한 traceId가 MDC에 설정됨
    log.info("결제 처리 시작");  // [abc-123-def-456] 결제 처리 시작
    // ...
    log.info("결제 완료");  // [abc-123-def-456] 결제 완료
    return PaymentResponse.success();
}
```

### 로그 출력 예시

```
# Order Service 로그
2025-01-15 10:30:00.123 [http-nio-8080-exec-1] [abc-123-def-456] [user-789] INFO  c.t.OrderController - 주문 생성 요청 수신
2025-01-15 10:30:00.234 [http-nio-8080-exec-1] [abc-123-def-456] [user-789] INFO  c.t.OrderService - Payment Service 호출
2025-01-15 10:30:00.567 [http-nio-8080-exec-1] [abc-123-def-456] [user-789] INFO  c.t.OrderService - 주문 생성 완료

# Payment Service 로그 (동일한 traceId)
2025-01-15 10:30:00.345 [http-nio-8081-exec-3] [abc-123-def-456] [user-789] INFO  c.t.PaymentController - 결제 처리 시작
2025-01-15 10:30:00.456 [http-nio-8081-exec-3] [abc-123-def-456] [user-789] INFO  c.t.PaymentService - 결제 완료
```

---

## 메시지 통신 추적 (RabbitMQ)

### 흐름도

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. Order Service - 이벤트 발행                                              │
│                                                                             │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ IntegrationEvent.from(domainEvent, "order-service")             │    │
│     │   → resolveTraceId(null) 호출                                    │    │
│     │   → MDC에서 requestId 조회: "abc-123-def-456"                    │    │
│     │   → IntegrationEvent.traceId = "abc-123-def-456"                │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│     로그: [abc-123-def-456] OrderCreatedEvent 발행                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │  RabbitMQ Message
                                      │  Payload (JSON):
                                      │  {
                                      │    "eventId": "evt-001",
                                      │    "eventType": "OrderCreatedEvent",
                                      │    "traceId": "abc-123-def-456",  ← 여기에 포함
                                      │    "sourceService": "order-service",
                                      │    "payload": "..."
                                      │  }
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  2. Payment Service - 이벤트 수신                                            │
│                                                                             │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ @RabbitListener                                                  │    │
│     │ EventContext.run(event, e -> { ... })                           │    │
│     │   → setupMdc(event) 호출                                         │    │
│     │   → event.getTraceId(): "abc-123-def-456"                       │    │
│     │   → MDC.put("requestId", "abc-123-def-456")                     │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│     로그: [abc-123-def-456] OrderCreatedEvent 수신                          │
│     로그: [abc-123-def-456] 결제 처리 시작                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  3. Payment Service - 새 이벤트 발행 (이벤트 체이닝)                          │
│                                                                             │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ IntegrationEvent.from(paymentCompletedEvent, "payment-service") │    │
│     │   → resolveTraceId(null) 호출                                    │    │
│     │   → MDC에서 requestId 조회: "abc-123-def-456" (동일!)            │    │
│     │   → IntegrationEvent.traceId = "abc-123-def-456"                │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│     로그: [abc-123-def-456] PaymentCompletedEvent 발행                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │  RabbitMQ Message
                                      │  {
                                      │    "traceId": "abc-123-def-456",  ← 동일한 traceId
                                      │    ...
                                      │  }
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  4. Ticket Service - 이벤트 수신                                             │
│                                                                             │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ EventContext.run(event, e -> { ... })                           │    │
│     │   → MDC.put("requestId", "abc-123-def-456")                     │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│     로그: [abc-123-def-456] PaymentCompletedEvent 수신                      │
│     로그: [abc-123-def-456] 티켓 발권 완료                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 코드 예시

```java
// Order Service - 이벤트 발행
@Transactional
public Order createOrder(OrderRequest request) {
    Order order = orderRepository.save(new Order(request));
    
    // traceId가 자동으로 MDC에서 추출되어 포함됨
    OrderCreatedEvent domainEvent = new OrderCreatedEvent(order.getId(), order.getAmount());
    IntegrationEvent event = IntegrationEvent.from(domainEvent, "order-service");
    
    log.info("OrderCreatedEvent 발행: orderId={}", order.getId());
    rabbitTemplate.convertAndSend(exchange, event.getRoutingKey(), event);
    
    return order;
}

// Payment Service - 이벤트 수신 및 체이닝
@RabbitListener(queues = "order.created.queue")
public void handleOrderCreated(IntegrationEvent event) {
    // ✅ EventContext.run() 사용 (반환값 없음)
    EventContext.run(event, e -> {
        // MDC에 원본 traceId 복원됨
        OrderCreatedEvent payload = e.getPayloadAs(OrderCreatedEvent.class);
        log.info("OrderCreatedEvent 수신: orderId={}", payload.getOrderId());
        
        // 결제 처리
        Payment payment = paymentService.process(payload);
        log.info("결제 처리 완료: paymentId={}", payment.getId());
        
        // 새 이벤트 발행 - 같은 traceId 자동 유지
        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(payment.getId());
        IntegrationEvent newEvent = IntegrationEvent.from(completedEvent, "payment-service");
        // newEvent.getTraceId() == "abc-123-def-456" (동일!)
        
        log.info("PaymentCompletedEvent 발행");
        rabbitTemplate.convertAndSend(exchange, newEvent.getRoutingKey(), newEvent);
    });
    // EventContext가 자동으로 MDC.clear() 호출
}

// Ticket Service - 최종 이벤트 수신
@RabbitListener(queues = "payment.completed.queue")
public void handlePaymentCompleted(IntegrationEvent event) {
    EventContext.run(event, e -> {
        PaymentCompletedEvent payload = e.getPayloadAs(PaymentCompletedEvent.class);
        log.info("PaymentCompletedEvent 수신: paymentId={}", payload.getPaymentId());
        
        Ticket ticket = ticketService.issue(payload.getPaymentId());
        log.info("티켓 발권 완료: ticketId={}", ticket.getId());
    });
}
```

### EventContext API

| 메서드 | 용도 | 반환값 |
|--------|------|--------|
| `run(event, Consumer)` | 이벤트 처리 (반환값 없음) | void |
| `execute(event, Function)` | 이벤트 처리 (반환값 있음) | R |
| `runWithNewTrace(Runnable)` | 새 traceId로 실행 (반환값 없음) | void |
| `executeWithNewTrace(Supplier)` | 새 traceId로 실행 (반환값 있음) | R |

```java
// 반환값 없음 → run()
EventContext.run(event, e -> {
    processEvent(e);
});

// 반환값 있음 → execute()
String result = EventContext.execute(event, e -> {
    return e.getPayloadAs(MyEvent.class).getId();
});
```

### 로그 출력 예시

```
# Order Service 로그
2025-01-15 10:30:00.100 [http-nio-8080-exec-1] [abc-123-def-456] INFO  c.t.OrderService - 주문 생성: orderId=1001
2025-01-15 10:30:00.150 [http-nio-8080-exec-1] [abc-123-def-456] INFO  c.t.OrderService - OrderCreatedEvent 발행

# Payment Service 로그 (동일한 traceId)
2025-01-15 10:30:00.200 [rabbit-listener-1] [abc-123-def-456] INFO  c.t.PaymentListener - OrderCreatedEvent 수신: orderId=1001
2025-01-15 10:30:00.300 [rabbit-listener-1] [abc-123-def-456] INFO  c.t.PaymentService - 결제 처리 완료: paymentId=2001
2025-01-15 10:30:00.350 [rabbit-listener-1] [abc-123-def-456] INFO  c.t.PaymentListener - PaymentCompletedEvent 발행

# Ticket Service 로그 (동일한 traceId)
2025-01-15 10:30:00.400 [rabbit-listener-2] [abc-123-def-456] INFO  c.t.TicketListener - PaymentCompletedEvent 수신: paymentId=2001
2025-01-15 10:30:00.500 [rabbit-listener-2] [abc-123-def-456] INFO  c.t.TicketService - 티켓 발권 완료: ticketId=3001
```

---

## 스케줄러/배치 추적

### @Scheduled 메서드 (자동)

`ScheduledTraceAutoConfiguration`이 `@Scheduled` 메서드에 자동으로 traceId를 생성합니다.

```java
@Component
public class ReportScheduler {

    // ✅ traceId가 자동으로 생성됨 (ScheduledTraceAspect)
    @Scheduled(cron = "0 0 2 * * *")
    public void generateDailyReport() {
        log.info("일일 리포트 생성 시작");  // [sched-xxx-xxx] 일일 리포트 생성 시작
        
        // Feign 호출도 추적 가능
        reportClient.fetchData();  // X-Trace-Id 헤더 자동 전파
        
        // 이벤트 발행도 추적 가능
        IntegrationEvent event = IntegrationEvent.from(reportEvent, "scheduler");
        rabbitTemplate.convertAndSend(exchange, event.getRoutingKey(), event);
        
        log.info("일일 리포트 생성 완료");  // [sched-xxx-xxx] 일일 리포트 생성 완료
    }
}
```

### 배치/수동 작업

HTTP 요청이나 @Scheduled 외의 시작점에서는 명시적으로 traceId를 생성합니다.

```java
@Component
public class BatchProcessor {

    // 반환값 없음 → runWithNewTrace()
    public void processBatch() {
        EventContext.runWithNewTrace(() -> {
            log.info("배치 처리 시작");  // [batch-xxx-xxx] 배치 처리 시작
            
            // 이 안에서의 모든 호출이 추적됨
            externalClient.call();
            
            log.info("배치 처리 완료");  // [batch-xxx-xxx] 배치 처리 완료
        });
    }

    // 반환값 있음 → executeWithNewTrace()
    public int processBatchWithResult() {
        return EventContext.executeWithNewTrace(() -> {
            log.info("배치 처리 시작");
            int count = processItems();
            log.info("배치 처리 완료: count={}", count);
            return count;
        });
    }
}
```

### 로그 출력 예시

```
# 스케줄러 로그
2025-01-15 02:00:00.001 [scheduling-1] [sched-abc-123] INFO  c.t.ReportScheduler - 일일 리포트 생성 시작
2025-01-15 02:00:01.234 [scheduling-1] [sched-abc-123] INFO  c.t.ReportScheduler - 데이터 조회 완료
2025-01-15 02:00:02.567 [scheduling-1] [sched-abc-123] INFO  c.t.ReportScheduler - 일일 리포트 생성 완료

# 배치 로그 (별도의 traceId)
2025-01-15 03:00:00.001 [batch-worker-1] [batch-def-456] INFO  c.t.BatchProcessor - 배치 처리 시작
2025-01-15 03:00:05.234 [batch-worker-1] [batch-def-456] INFO  c.t.BatchProcessor - 배치 처리 완료
```

---

## 혼합 시나리오

### HTTP 요청 → 이벤트 발행 → Feign 호출

실제 환경에서는 HTTP와 메시지 통신이 혼합되어 사용됩니다.

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                           전체 요청 흐름                                          │
│                         traceId: abc-123-def-456                                 │
└──────────────────────────────────────────────────────────────────────────────────┘

[Client]
    │
    │ POST /orders
    ▼
┌─────────────┐     Feign (HTTP)      ┌─────────────┐
│   Order     │ ──────────────────▶   │  Inventory  │
│   Service   │                       │   Service   │
│             │ ◀──────────────────   │             │
└─────────────┘     재고 확인 응답     └─────────────┘
    │
    │ RabbitMQ (이벤트)
    ▼
┌─────────────┐     RabbitMQ          ┌─────────────┐
│   Payment   │ ──────────────────▶   │   Ticket    │
│   Service   │                       │   Service   │
└─────────────┘                       └─────────────┘
    │
    │ Feign (HTTP)
    ▼
┌─────────────┐
│ Notification│
│   Service   │
└─────────────┘

모든 서비스 로그: [abc-123-def-456] ...
```

### 코드 예시

```java
@PostMapping("/orders")
public OrderResponse createOrder(@RequestBody OrderRequest request) {
    // 1. Feign 호출 - traceId 자동 전파
    InventoryResponse inventory = inventoryClient.check(request.getProductId());
    log.info("재고 확인: {}", inventory);
    
    // 2. 주문 생성
    Order order = orderService.create(request);
    log.info("주문 생성: {}", order.getId());
    
    // 3. 이벤트 발행 - traceId 자동 포함
    IntegrationEvent event = IntegrationEvent.from(
        new OrderCreatedEvent(order.getId()),
        "order-service"
    );
    rabbitTemplate.convertAndSend(exchange, event.getRoutingKey(), event);
    log.info("OrderCreatedEvent 발행");
    
    return OrderResponse.success(order);
}
```

---

## 로그 설정

### logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- 콘솔 출력 패턴 -->
    <property name="CONSOLE_LOG_PATTERN" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId}] [%X{userId}] %-5level %logger{36} - %msg%n"/>
    
    <!-- JSON 포맷 (ELK 연동용) -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>sourceService</includeMdcKeyName>
            <includeMdcKeyName>eventType</includeMdcKeyName>
        </encoder>
    </appender>
    
    <!-- 콘솔 출력 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### ELK에서 검색

```
# Kibana에서 traceId로 전체 흐름 검색
requestId: "abc-123-def-456"

# 특정 사용자의 모든 요청 검색
userId: "user-789"

# 특정 이벤트 타입 검색
eventType: "OrderCreatedEvent"
```

---

## 트러블슈팅

### 1. traceId가 전파되지 않는 경우

**증상**: 서비스 간 호출 시 각 서비스에서 다른 traceId가 생성됨

**원인 및 해결**:

| 통신 방식 | 확인 사항 |
|----------|----------|
| Feign | `FeignTraceAutoConfiguration` 빈 로드 확인 |
| RabbitMQ | `EventContext.run()` 사용 여부 확인 |
| @Scheduled | `ScheduledTraceAutoConfiguration` 빈 로드 확인 |

```java
// ❌ 잘못된 사용 - MDC 복원 안 됨
@RabbitListener(queues = "order.queue")
public void handle(IntegrationEvent event) {
    OrderCreatedEvent payload = event.getPayloadAs(OrderCreatedEvent.class);
    // traceId가 MDC에 없음!
}

// ✅ 올바른 사용
@RabbitListener(queues = "order.queue")
public void handle(IntegrationEvent event) {
    EventContext.run(event, e -> {
        OrderCreatedEvent payload = e.getPayloadAs(OrderCreatedEvent.class);
        // traceId가 MDC에 복원됨
    });
}
```

### 2. 새로운 흐름에서 이전 traceId가 남아있는 경우

**증상**: 스레드 재사용 시 이전 요청의 traceId가 로그에 출력됨

**해결**: 새로운 traceId 명시적 생성 (단, @Scheduled는 자동 처리됨)

```java
// @Scheduled 외의 배치 작업
public void batchTask() {
    EventContext.runWithNewTrace(() -> {
        // 새로운 traceId로 실행
        log.info("배치 작업 시작");
        // ...
    });
}
```

### 3. MDC가 클리어되지 않는 경우

**증상**: 스레드 풀 재사용 시 이전 요청의 정보가 남아있음

**해결**: `MdcFilter`, `EventContext`, `ScheduledTraceAspect`는 자동으로 MDC를 정리함. 수동으로 MDC를 설정한 경우 반드시 `finally`에서 정리:

```java
try {
    MdcUtils.setRequestId(customTraceId);
    // 작업 수행
} finally {
    MdcUtils.clear();  // 반드시 정리!
}
```

### 4. AutoConfiguration이 동작하지 않는 경우

**확인 사항**:

1. `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일 존재 확인
2. 필요한 의존성 확인:
    - Feign 추적: `spring-cloud-starter-openfeign`
    - @Scheduled 추적: `spring-boot-starter-aop`
3. `@ConditionalOnMissingBean` 조건 충족 여부 확인

---

## 요약

| 통신 방식 | traceId 전파 방법 | 자동화 컴포넌트 |
|----------|------------------|----------------|
| HTTP 요청 수신 | `X-Trace-Id` 헤더 | `MdcFilter` (자동) |
| Feign 호출 | `X-Trace-Id` 헤더 자동 추가 | `FeignTraceAutoConfiguration` (자동) |
| 이벤트 발행 | `IntegrationEvent.traceId` 필드 | `IntegrationEvent.from()` (자동) |
| 이벤트 수신 | `IntegrationEvent`에서 MDC 복원 | `EventContext.run()` **(수동 호출 필요)** |
| @Scheduled | MDC에 traceId 설정 | `ScheduledTraceAutoConfiguration` (자동) |
| 배치/수동 | MDC에 traceId 설정 | `EventContext.runWithNewTrace()` **(수동 호출 필요)** |

**핵심 원칙**:
- HTTP 요청, Feign 호출, @Scheduled는 **자동**으로 traceId가 관리됩니다.
- 이벤트 수신 시에는 반드시 `EventContext.run()`을 **수동으로 호출**하세요.
- 배치/수동 작업에서는 `EventContext.runWithNewTrace()`를 **수동으로 호출**하세요.