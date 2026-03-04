package com.washwise.backend.domain.order;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LaundryServiceType serviceType;

    @Column(nullable = false)
    private Integer quantity;

    @Builder.Default
    private Double cost = 0.0;

    @Column(nullable = false, length = 50)
    private String status;
}