# Tickatch Common Library

Tickatch MSA 프로젝트를 위한 공통 라이브러리입니다.

Spring Boot 3.x 기반의 API 응답 표준화, 예외 처리, 인증/인가, 로깅, 분산 추적, 이벤트 등을 제공합니다.

---

## 설치

### Gradle

```groovy
dependencies {
    implementation 'io.github.tickatch:common-lib:0.0.5'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.tickatch</groupId>
    <artifactId>common-lib</artifactId>
    <version>0.0.5</version>
</dependency>
```

### 요구사항

- Java 21+
- Spring Boot 3.5+

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **API 응답 표준화** | 일관된 응답 포맷 (`ApiResponse`, `PageResponse`) |
| **전역 예외 처리** | `@RestControllerAdvice` 기반 통합 예외 핸들링 |
| **인증/인가** | X-User-Id 헤더 기반 인증, Spring Security 통합 |
| **분산 추적** | traceId 자동 생성/전파 (HTTP, Feign, RabbitMQ, Scheduler) |
| **요청 추적 로깅** | MDC 기반 requestId/userId 추적, AOP 자동 로깅 |
| **JPA Auditing** | createdBy, updatedBy 자동 설정 |
| **이벤트** | RabbitMQ용 도메인/통합 이벤트 |
| **Swagger** | OpenAPI 3.0 + JWT 인증 스키마 |
| **유틸리티** | JSON 변환, 도메인 ID 생성 |

---

## 빠른 시작

의존성만 추가하면 **AutoConfiguration**에 의해 자동으로 활성화됩니다.

```yaml
# application.yml - 최소 설정
spring:
  messages:
    basename: messages
    encoding: UTF-8

openapi:
  service:
    url: http://localhost:8080
    title: My Service API
    description: API Documentation
```

```properties
# messages.properties - 에러 메시지 정의
BAD_REQUEST=잘못된 요청입니다.
VALIDATION_ERROR=입력값이 유효하지 않습니다.
NOT_FOUND=리소스를 찾을 수 없습니다.
UNAUTHORIZED=인증이 필요합니다.
FORBIDDEN=접근 권한이 없습니다.
INTERNAL_SERVER_ERROR=서버 오류가 발생했습니다.
```

---

## 패키지 구조

```
io.github.tickatch.common/
├── api/           # API 응답 DTO
│   ├── ApiResponse.java
│   ├── PageResponse.java
│   └── PageInfo.java
├── autoconfig/    # Spring Boot AutoConfiguration
│   ├── SecurityAutoConfiguration.java
│   ├── MdcFilterAutoConfiguration.java
│   ├── FeignTraceAutoConfiguration.java
│   ├── ScheduledTraceAutoConfiguration.java
│   ├── LoggingAutoConfiguration.java
│   ├── ExceptionHandlerAutoConfiguration.java
│   ├── JpaAuditingAutoConfiguration.java
│   └── SwaggerAutoConfiguration.java
├── error/         # 예외 처리
│   ├── ErrorCode.java
│   ├── GlobalErrorCode.java
│   ├── BusinessException.java
│   ├── FieldError.java
│   ├── GlobalExceptionHandler.java
│   └── ValidationErrorParser.java
├── event/         # 이벤트
│   ├── DomainEvent.java
│   ├── IntegrationEvent.java
│   └── EventContext.java
├── jpa/           # JPA 지원
│   └── AuditorAwareImpl.java
├── logging/       # 로깅
│   ├── MdcUtils.java
│   ├── MdcFilter.java
│   ├── LogExecution.java
│   ├── LogManager.java
│   └── LoggingAspect.java
├── message/       # 메시지 처리
│   ├── MessageResolver.java
│   └── DefaultMessageResolver.java
├── security/      # 인증/인가
│   ├── UserType.java
│   ├── AuthenticatedUser.java
│   ├── LoginFilter.java
│   └── BaseSecurityConfig.java
├── security/test/ # 테스트 지원
│   ├── MockUser.java
│   └── WithMockUserSecurityContextFactory.java
├── swagger/       # API 문서
│   └── SwaggerConfig.java
└── util/          # 유틸리티
    ├── JsonUtils.java
    └── UuidUtils.java
```

---

## 상세 사용법

### API 응답

#### 성공 응답

```java
@GetMapping("/{id}")
public ApiResponse<TicketDto> getTicket(@PathVariable Long id) {
    TicketDto ticket = ticketService.findById(id);
    return ApiResponse.success(ticket);
}

@PostMapping
public ApiResponse<TicketDto> createTicket(@RequestBody @Valid TicketRequest request) {
    TicketDto ticket = ticketService.create(request);
    return ApiResponse.success(ticket, "티켓이 생성되었습니다.");
}

@DeleteMapping("/{id}")
public ApiResponse<Void> deleteTicket(@PathVariable Long id) {
    ticketService.delete(id);
    return ApiResponse.successWithMessage("티켓이 삭제되었습니다.");
}
```

**응답 예시:**
```json
{
  "success": true,
  "data": { "id": 123, "name": "콘서트 티켓" },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

#### 페이징 응답

```java
@GetMapping
public PageResponse<TicketDto> getTickets(Pageable pageable) {
    Page<Ticket> page = ticketRepository.findAll(pageable);
    return PageResponse.from(page, TicketDto::from);
}
```

**응답 예시:**
```json
{
  "content": [
    { "id": 1, "name": "티켓1" },
    { "id": 2, "name": "티켓2" }
  ],
  "pageInfo": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false,
    "hasNext": true
  }
}
```

---

### 예외 처리

#### ErrorCode 정의

```java
@Getter
@RequiredArgsConstructor
public enum TicketErrorCode implements ErrorCode {
    TICKET_NOT_FOUND(404, "TICKET_NOT_FOUND"),
    TICKET_SOLD_OUT(409, "TICKET_SOLD_OUT"),
    SEAT_ALREADY_RESERVED(409, "SEAT_ALREADY_RESERVED");

    private final int status;
    private final String code;
}
```

#### 메시지 정의 (messages.properties)

```properties
TICKET_NOT_FOUND=티켓 {0}을(를) 찾을 수 없습니다.
TICKET_SOLD_OUT={0} 공연의 티켓이 매진되었습니다.
SEAT_ALREADY_RESERVED={0}구역 {1}번 좌석은 이미 예약되었습니다.
```

#### 예외 던지기

```java
// 기본 사용
throw new BusinessException(TicketErrorCode.TICKET_NOT_FOUND, ticketId);
// → "티켓 123을(를) 찾을 수 없습니다."

// 다중 인자
throw new BusinessException(TicketErrorCode.SEAT_ALREADY_RESERVED, "A", "15");
// → "A구역 15번 좌석은 이미 예약되었습니다."

// 원인 예외 포함
throw new BusinessException(GlobalErrorCode.DATABASE_ERROR, e, "users");
```

**에러 응답 예시:**
```json
{
  "success": false,
  "error": {
    "code": "TICKET_NOT_FOUND",
    "message": "티켓 123을(를) 찾을 수 없습니다.",
    "status": 404,
    "path": "/api/tickets/123"
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

#### GlobalErrorCode 목록

| 카테고리 | 코드 | HTTP 상태 | 설명 |
|---------|------|----------|------|
| **요청 오류** | `BAD_REQUEST` | 400 | 잘못된 요청 |
| | `VALIDATION_ERROR` | 400 | 입력값 검증 실패 |
| | `INVALID_JSON` | 400 | JSON 파싱 실패 |
| | `TYPE_MISMATCH` | 400 | 타입 변환 실패 |
| | `MISSING_PARAMETER` | 400 | 필수 파라미터 누락 |
| **인증/인가** | `UNAUTHORIZED` | 401 | 인증 필요 |
| | `INVALID_TOKEN` | 401 | 유효하지 않은 토큰 |
| | `EXPIRED_TOKEN` | 401 | 만료된 토큰 |
| | `FORBIDDEN` | 403 | 접근 권한 없음 |
| **리소스** | `NOT_FOUND` | 404 | 리소스 없음 |
| | `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드 |
| | `CONFLICT` | 409 | 리소스 충돌 |
| | `DUPLICATE_RESOURCE` | 409 | 중복 리소스 |
| **비즈니스** | `BUSINESS_ERROR` | 422 | 비즈니스 로직 오류 |
| | `INVALID_STATE` | 422 | 잘못된 상태 |
| **서버** | `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |
| | `DATABASE_ERROR` | 500 | 데이터베이스 오류 |
| | `SERVICE_UNAVAILABLE` | 503 | 서비스 이용 불가 |

---

### 인증/인가

#### 사용자 유형 (UserType)

```java
public enum UserType {
    CUSTOMER("구매자"),   // → ROLE_CUSTOMER
    SELLER("판매자"),     // → ROLE_SELLER
    ADMIN("관리자");      // → ROLE_ADMIN
}
```

#### 컨트롤러에서 현재 사용자 조회

```java
@GetMapping("/me")
public UserInfo getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
    Long userId = user.getUserId();
    UserType userType = user.getUserType();
    
    if (userType.isAdmin()) {
        // 관리자 전용 로직
    }
    
    return userService.findById(userId);
}
```

#### 권한 기반 접근 제어

```java
@PreAuthorize("hasRole('SELLER')")
@PostMapping("/products")
public Product createProduct(@RequestBody ProductRequest request) { ... }

@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
@GetMapping("/seller/stats")
public SellerStats sellerStats() { ... }
```

#### 커스텀 Security 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends BaseSecurityConfig {

    @Bean
    @Override
    protected LoginFilter loginFilterBean() {
        return new LoginFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return build(http);
    }

    @Override
    protected Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>
            .AuthorizationManagerRequestMatcherRegistry> authorizeHttpRequests() {
        return registry -> registry
            .requestMatchers("/public/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated();
    }
}
```

#### 기본 허용 경로

AutoConfiguration이 기본으로 허용하는 경로:

- `/v3/api-docs/**` - OpenAPI 문서
- `/swagger-ui/**` - Swagger UI
- `/actuator/health` - 헬스 체크
- `/actuator/info` - 서비스 정보

---

### 분산 추적

MSA 환경에서 여러 서비스를 거치는 요청의 전체 흐름을 추적할 수 있습니다.

#### 트리거별 동작

| 트리거 | 담당 컴포넌트 | 동작 |
|--------|-------------|------|
| HTTP 요청 (최초) | `MdcFilter` | 새 traceId 생성 |
| HTTP 요청 (전파) | `MdcFilter` | X-Trace-Id 헤더에서 수신 |
| Feign 호출 | `FeignTraceAutoConfiguration` | X-Trace-Id 헤더로 자동 전파 |
| 이벤트 발행 | `IntegrationEvent.from()` | MDC에서 traceId 자동 추출 |
| 이벤트 수신 | `EventContext.run()` | 이벤트에서 traceId 복원 **(수동 호출)** |
| @Scheduled | `ScheduledTraceAutoConfiguration` | 새 traceId 자동 생성 |

#### 전체 흐름

```
[Client]
    │ POST /orders
    ▼
┌─────────────┐     Feign (자동)      ┌─────────────┐
│   Order     │ ──────────────────▶   │  Payment    │
│   Service   │  X-Trace-Id: abc-123  │   Service   │
└─────────────┘                       └─────────────┘
    │
    │ RabbitMQ (자동)
    │ traceId: abc-123
    ▼
┌─────────────┐
│   Ticket    │
│   Service   │
└─────────────┘

모든 서비스 로그: [abc-123] ...
```

#### 이벤트 발행 (자동)

```java
@PostMapping("/orders")
public OrderResponse createOrder(@RequestBody OrderRequest request) {
    // MdcFilter가 이미 traceId 설정 완료
    log.info("주문 생성");  // [abc-123] 주문 생성
    
    // 이벤트 발행 - traceId 자동 포함
    IntegrationEvent event = IntegrationEvent.from(domainEvent, "order-service");
    rabbitTemplate.convertAndSend(exchange, event.getRoutingKey(), event);
    
    return OrderResponse.success(order);
}
```

#### 이벤트 수신 (수동 호출 필요)

```java
@RabbitListener(queues = "order.created.queue")
public void handleOrderCreated(IntegrationEvent event) {
    // EventContext.run()으로 MDC 복원
    EventContext.run(event, e -> {
        OrderCreatedEvent payload = e.getPayloadAs(OrderCreatedEvent.class);
        log.info("주문 이벤트 수신");  // [abc-123] 주문 이벤트 수신
        
        // 처리 로직...
    });
}
```

#### EventContext API

| 메서드 | 용도 | 반환값 |
|--------|------|--------|
| `run(event, Consumer)` | 이벤트 처리 | void |
| `execute(event, Function)` | 이벤트 처리 (반환값) | R |
| `runWithNewTrace(Runnable)` | 새 traceId로 실행 | void |
| `executeWithNewTrace(Supplier)` | 새 traceId로 실행 (반환값) | R |

---

### 이벤트

#### 도메인 이벤트 정의

```java
@Getter
public class TicketCreatedEvent extends DomainEvent {
    
    private final Long ticketId;
    private final String eventName;

    public TicketCreatedEvent(Long ticketId, String eventName) {
        super();
        this.ticketId = ticketId;
        this.eventName = eventName;
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(ticketId);
    }

    @Override
    public String getAggregateType() {
        return "Ticket";
    }
}
```

#### 이벤트 발행 (RabbitMQ)

```java
@Service
@RequiredArgsConstructor
public class TicketService {

    private final RabbitTemplate rabbitTemplate;

    public Ticket createTicket(TicketRequest request) {
        Ticket ticket = ticketRepository.save(new Ticket(request));
        
        // 도메인 이벤트 → 통합 이벤트
        TicketCreatedEvent domainEvent = new TicketCreatedEvent(ticket.getId(), request.getEventName());
        IntegrationEvent event = IntegrationEvent.from(domainEvent, "ticket-service");
        
        rabbitTemplate.convertAndSend("ticket.exchange", event.getRoutingKey(), event);
        
        return ticket;
    }
}
```

#### IntegrationEvent 기능

```java
// TTL 설정
IntegrationEvent event = IntegrationEvent.createWithTtl(
    "TicketCreated", "ticket-service", payload, "ticket.created", 3600
);

// 만료 확인
if (event.isExpired()) { ... }

// 재시도
if (event.canRetry()) {
    IntegrationEvent retryEvent = event.retry();
}
```

---

### 로깅

#### logback.xml 설정

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId}] [%X{userId}] %-5level %logger{36} - %msg%n</pattern>
```

#### 자동 로깅 (RestController)

모든 RestController 메서드는 자동으로 로깅됩니다:

```
INFO  GET /api/tickets/123 - Request ID: abc-123, User ID: 42, Method: TicketController.getTicket, Params: {id: 123}
INFO  GET /api/tickets/123 - Request ID: abc-123, User ID: 42, Method: TicketController.getTicket, Return: {"id":123}
```

#### 선택적 로깅 (@LogExecution)

```java
@Service
public class PaymentService {

    @LogExecution
    public PaymentResult processPayment(PaymentRequest request) {
        // 진입/종료 로그가 자동 기록됨
        return result;
    }
}
```

#### MdcUtils 직접 사용

```java
// 현재 요청 ID (traceId) 조회
String requestId = MdcUtils.getRequestId();

// 요청 ID 존재 여부 확인
boolean hasId = MdcUtils.hasRequestId();

// 현재 사용자 ID 조회
String userId = MdcUtils.getUserId();

// 커스텀 값 저장
MdcUtils.put("orderId", orderId);

// MDC 정리
MdcUtils.clear();
```

---

### JPA Auditing

#### 엔티티에서 사용

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;  // 자동으로 현재 userId 설정

    @LastModifiedBy
    private String updatedBy;  // 자동으로 현재 userId 설정
}
```

> **Note:** 인증되지 않은 요청은 `"SYSTEM"`으로 설정됩니다.

---

### Swagger

#### 설정

```yaml
openapi:
  service:
    url: https://api.tickatch.io
    title: Ticket Service API
    description: 티켓 서비스 API 문서
    path-prefix: /v1/tickets  # Gateway prefix (선택)
```

#### 접속 URL

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

### 유틸리티

#### JsonUtils

```java
// Object → JSON
String json = JsonUtils.toJson(ticket);
String prettyJson = JsonUtils.toPrettyJson(ticket);

// JSON → Object
Ticket ticket = JsonUtils.fromJson(json, Ticket.class);

// 안전한 변환 (예외 대신 Optional)
Optional<Ticket> ticketOpt = JsonUtils.fromJsonSafe(json, Ticket.class);

// JSON 유효성 검사
boolean valid = JsonUtils.isValidJson(json);
```

#### UuidUtils

```java
// 기본 UUID 생성
String uuid = UuidUtils.generate();           // "550e8400-e29b-41d4-a716-446655440000"
String compact = UuidUtils.generateCompact(); // "550e8400e29b41d4a716446655440000"

// 도메인 ID 생성
String ticketId = UuidUtils.generateTicketId();      // "TKT-a1b2c3d4"
String orderId = UuidUtils.generateOrderId();        // "ORD-20250115103000-a1b2c3"
String paymentId = UuidUtils.generatePaymentId();    // "PAY-20250115103000-d4e5f6"
String userId = UuidUtils.generateUserId();          // "USR-a1b2c3d4"

// 커스텀 도메인 ID
String customId = UuidUtils.generateDomainId("ITEM");       // "ITEM-a1b2c3d4"
String timestampId = UuidUtils.generateTimestampId("LOG");  // "LOG-20250115103000-a1b2c3"

// 도메인 ID 검증
boolean valid = UuidUtils.isValidDomainId("TKT-a1b2c3d4", "TKT");
```

---

### 테스트 지원

#### @MockUser 어노테이션

```java
// 기본 사용자 (CUSTOMER)
@Test
@MockUser(userId = "testUser")
void testCustomerAccess() {
    // SecurityContext에 ROLE_CUSTOMER 권한 설정됨
}

// 판매자 권한으로 테스트
@Test
@MockUser(userId = "seller123", userType = UserType.SELLER)
void testSellerAccess() {
    // SecurityContext에 ROLE_SELLER 권한 설정됨
}

// 관리자 권한으로 테스트
@Test
@MockUser(userId = "admin", userType = UserType.ADMIN)
void testAdminAccess() {
    // SecurityContext에 ROLE_ADMIN 권한 설정됨
}
```

---

## AutoConfiguration

의존성 추가만으로 자동 활성화되는 기능들:

| AutoConfiguration | 조건 | 등록되는 빈 | 비활성화 조건 |
|-------------------|------|------------|--------------|
| `SecurityAutoConfiguration` | spring-security 존재 | `LoginFilter`, `SecurityFilterChain` | 직접 `SecurityFilterChain` 빈 정의 |
| `MdcFilterAutoConfiguration` | Servlet 웹앱 | `MdcFilter` | 직접 `MdcFilter` 빈 정의 |
| `FeignTraceAutoConfiguration` | spring-cloud-openfeign 존재 | `RequestInterceptor` | - |
| `ScheduledTraceAutoConfiguration` | spring-aop 존재 | `ScheduledTraceAspect` | - |
| `LoggingAutoConfiguration` | Servlet 웹앱 | `LoggingAspect`, `LogManager` | `tickatch.logging.enabled=false` |
| `ExceptionHandlerAutoConfiguration` | Servlet 웹앱 | `GlobalExceptionHandler` | 직접 `@RestControllerAdvice` 정의 |
| `JpaAuditingAutoConfiguration` | spring-data-jpa 존재 | `AuditorAware` | `tickatch.jpa.auditing.enabled=false` |
| `SwaggerAutoConfiguration` | springdoc-openapi 존재 | `SwaggerConfig` | `tickatch.swagger.enabled=false` |

---

## 설정 옵션

```yaml
tickatch:
  logging:
    enabled: true        # 로깅 AutoConfiguration (기본: true)
  exception:
    enabled: true        # 예외 처리 AutoConfiguration (기본: true)
  jpa:
    auditing:
      enabled: true      # JPA Auditing AutoConfiguration (기본: true)
  swagger:
    enabled: true        # Swagger AutoConfiguration (기본: true)

spring:
  messages:
    basename: messages   # 메시지 파일 위치
    encoding: UTF-8

openapi:
  service:
    url: http://localhost:8080
    title: My Service API
    description: API Documentation
    path-prefix: /v1/my-service  # Gateway prefix (선택)
```

---

## 커스터마이징

### 커스텀 예외 처리기

```java
@RestControllerAdvice
public class CustomExceptionHandler extends GlobalExceptionHandler {

    public CustomExceptionHandler(MessageResolver messageResolver) {
        super(messageResolver);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentFailed(
            PaymentFailedException e, 
            HttpServletRequest request) {
        // 커스텀 처리
    }
}
```

### 커스텀 AuditorAware

```java
@Bean
public AuditorAware<String> auditorAware() {
    return () -> Optional.of("BATCH_JOB");
}
```

### 커스텀 MessageResolver

```java
@Bean
public MessageResolver messageResolver() {
    return (code, args) -> externalMessageService.getMessage(code, args);
}
```

---