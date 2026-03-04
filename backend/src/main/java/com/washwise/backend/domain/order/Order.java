package com.washwise.backend.domain.order;

import com.washwise.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Builder.Default
    private Double totalCost = 0.0;

    @Column(length = 20)
    @Builder.Default
    private String paymentStatus = "unpaid";

    @Builder.Default
    private Boolean isCoveredByPlan = false;

    private String qrCodePath;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}