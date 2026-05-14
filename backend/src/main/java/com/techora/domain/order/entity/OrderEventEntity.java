package com.techora.domain.order.entity;

import com.techora.domain.order.constant.OrderEventActorType;
import com.techora.domain.order.constant.OrderEventType;
import com.techora.domain.order.constant.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_events", indexes = {
        @Index(name = "idx_order_events_order_created_at", columnList = "order_id, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private OrderEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private OrderStatus newStatus;

    @Column(nullable = false, length = 80)
    private String reason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private OrderEventActorType actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name", length = 80)
    private String actorName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
