package com.washwise.backend.domain.user;

import com.washwise.backend.domain.order.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 255)
    private String password; // Store hashed

    @Column(nullable = false, length = 20)
    private String role; // "customer", "serviceman", "admin"

    @Column(unique = true)
    private String googleId;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders;

    // --- Subscription Fields ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MembershipPlanEnum membershipPlan = MembershipPlanEnum.NONE;

    private ZonedDateTime membershipExpiryDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Integer> monthlyServicesUsed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> staticQrCodes;

    // --- Password Reset ---
    @Column(unique = true)
    private String resetToken;
    private ZonedDateTime resetTokenExpiry;
}