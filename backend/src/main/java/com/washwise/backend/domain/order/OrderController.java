package com.washwise.backend.domain.order;

import com.washwise.backend.domain.order.dto.OrderDtos.*;
import com.washwise.backend.domain.user.User;
import com.washwise.backend.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderServiceImpl orderService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper; // MapStruct mapper instance

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SERVICEMAN')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {
        Order newOrder = orderService.createOrder(request);
        return ResponseEntity.ok(orderMapper.toResponse(newOrder));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(@AuthenticationPrincipal User currentUser) {
        List<Order> orders;
        if ("customer".equals(currentUser.getRole())) {
            orders = orderRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentUser.getId());
        } else {
            orders = orderRepository.findAll();
        }
        return ResponseEntity.ok(orders.stream().map(orderMapper::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/qr/{orderId}")
    public ResponseEntity<OrderResponse> getOrderByQr(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @PutMapping("/{orderId}/pay")
    public ResponseEntity<OrderResponse> payOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal User currentUser) {
        Order order = orderService.processPayment(orderId, currentUser.getId());
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @PutMapping("/items/{itemId}/status")
    @PreAuthorize("hasAuthority('ROLE_SERVICEMAN')")
    public ResponseEntity<OrderItemResponse> updateStatus(
            @PathVariable String itemId,
            @Valid @RequestBody StatusUpdateRequest statusUpdate) {
        OrderItem item = orderService.updateItemStatus(itemId, statusUpdate.status());
        return ResponseEntity.ok(orderMapper.toItemResponse(item));
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal User currentUser) {
        
        List<Order> myOrders = orderRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentUser.getId());
        
        List<OrderResponse> response = myOrders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }
}