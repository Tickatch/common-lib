package io.github.tickatch.common.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * 도메인 이벤트 기본 클래스.
 *
 * <p>모든 도메인 이벤트가 상속받는 추상 클래스.
 * RabbitMQ 등 메시지 브로커에서 사용 가능.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @Getter
 * public class TicketCreatedEvent extends DomainEvent {
 *     private final Long ticketId;
 *     private final String eventName;
 *     private final int quantity;
 *
 *     public TicketCreatedEvent(Long ticketId, String eventName, int quantity) {
 *         super();
 *         this.ticketId = ticketId;
 *         this.eventName = eventName;
 *         this.quantity = quantity;
 *     }
 *
 *     @Override
 *     public String getAggregateId() {
 *         return String.valueOf(ticketId);
 *     }
 *
 *     @Override
 *     public String getAggregateType() {
 *         return "Ticket";
 *     }
 * }
 * }</pre>
 *
 * @author Tickatch
 * @since 0.0.1
 */
@Getter
public abstract class DomainEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 이벤트 고유 ID */
    private final String eventId;

    /** 이벤트 타입 (클래스 이름) */
    private final String eventType;

    /** 이벤트 발생 시간 */
    private final Instant occurredAt;

    /** 이벤트 버전 (스키마 버전 관리용) */
    private final int version;

    /**
     * 기본 생성자 - 자동으로 메타데이터 생성
     */
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.occurredAt = Instant.now();
        this.version = 1;
    }

    /**
     * 버전 지정 생성자
     */
    protected DomainEvent(int version) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.occurredAt = Instant.now();
        this.version = version;
    }

    /**
     * 복원용 생성자 (이벤트 소싱에서 재구성 시 사용)
     */
    protected DomainEvent(String eventId, Instant occurredAt) {
        this.eventId = eventId;
        this.eventType = this.getClass().getSimpleName();
        this.occurredAt = occurredAt;
        this.version = 1;
    }

    /**
     * 복원용 생성자 (버전 포함)
     */
    protected DomainEvent(String eventId, Instant occurredAt, int version) {
        this.eventId = eventId;
        this.eventType = this.getClass().getSimpleName();
        this.occurredAt = occurredAt;
        this.version = version;
    }

    /**
     * Aggregate ID 반환 (하위 클래스에서 구현)
     */
    @JsonIgnore
    public String getAggregateId() {
        return null;
    }

    /**
     * Aggregate 타입 반환 (하위 클래스에서 구현)
     */
    @JsonIgnore
    public String getAggregateType() {
        return null;
    }

    /**
     * RabbitMQ 라우팅 키 반환
     * 기본값: {aggregateType}.{eventType}
     */
    @JsonIgnore
    public String getRoutingKey() {
        String aggregate = getAggregateType();
        if (aggregate != null && !aggregate.isEmpty()) {
            return aggregate.toLowerCase() + "." + eventType;
        }
        return eventType.toLowerCase();
    }
}