package com.washwise.backend.domain.order;

import com.washwise.backend.domain.order.dto.OrderDtos.OrderItemResponse;
import com.washwise.backend.domain.order.dto.OrderDtos.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        // Calculate next statuses based on the workflow
        List<String> workflow = item.getServiceType().getWorkflow();
        int currentIndex = workflow.indexOf(item.getStatus());
        List<String> possibleNextStatuses = currentIndex >= 0 && currentIndex < workflow.size() - 1 
                ? workflow.subList(currentIndex + 1, workflow.size()) 
                : List.of();

        return new OrderItemResponse(
                item.getId(),
                item.getServiceType().getDisplayName(),
                item.getQuantity(),
                item.getCost(),
                item.getStatus(),
                possibleNextStatuses
        );
    }

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        String qrUrl = order.getQrCodePath() != null ? "/qr_codes/" + order.getQrCodePath() : null;

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getOwner().getId(),
                order.getCreatedAt(),
                order.getTotalCost(),
                order.getPaymentStatus(),
                order.getIsCoveredByPlan(),
                qrUrl,
                itemResponses
        );
    }
}