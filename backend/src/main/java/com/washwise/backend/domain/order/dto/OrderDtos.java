package com.washwise.backend.domain.order.dto;

import com.washwise.backend.domain.order.LaundryServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.ZonedDateTime;
import java.util.List;

public class OrderDtos {

    public record ServiceOrderItemCreate(
        @NotNull LaundryServiceType serviceType,
        @Min(1) @Max(20) int quantity
    ) {}

    public record OrderCreateRequest(
        @NotBlank String customerUsername,
        @NotEmpty @Valid List<ServiceOrderItemCreate> services
    ) {}

    public record StatusUpdateRequest(
        @NotBlank String status
    ) {}

    public record OrderItemResponse(
        String id,
        String serviceName,
        int quantity,
        double cost,
        String status,
        List<String> possibleNextStatuses
    ) {}

    public record OrderResponse(
        String id,
        String ownerId,
        ZonedDateTime createdAt,
        double totalCost,
        String paymentStatus,
        boolean isCoveredByPlan,
        String qrCodeUrl,
        List<OrderItemResponse> items
    ) {}
}