package com.washwise.backend.domain.user;

import com.washwise.backend.domain.order.Order;
import com.washwise.backend.domain.order.OrderMapper;
import com.washwise.backend.domain.order.OrderRepository;
import com.washwise.backend.domain.order.dto.OrderDtos.OrderResponse;
import com.washwise.backend.domain.user.dto.PasswordChangeRequest;
import com.washwise.backend.domain.user.dto.SubscriptionCreateRequest;
import com.washwise.backend.domain.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    // Injected for the active-orders endpoint
    private final OrderRepository orderRepository; 
    private final OrderMapper orderMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SERVICEMAN')")
    public ResponseEntity<List<UserResponse>> listUsers() {
        List<UserResponse> users = userRepository.findAll()
                .stream().map(userMapper::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> readUsersMe(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userMapper.toResponse(currentUser));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('ROLE_SERVICEMAN')")
    public ResponseEntity<Map<String, String>> removeUser(
            @PathVariable String userId,
            @AuthenticationPrincipal User currentUser) {
        userService.deleteUser(userId, currentUser);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    @GetMapping("/me/qrcodes")
    public ResponseEntity<Map<String, String>> getMyQr(@AuthenticationPrincipal User currentUser) {
        String filename = userService.getOrCreateQr(currentUser);
        return ResponseEntity.ok(Map.of("user_qr", "/qr_codes/" + filename));
    }

    @PostMapping("/{userId}/subscribe")
    @PreAuthorize("hasAuthority('ROLE_SERVICEMAN')")
    public ResponseEntity<UserResponse> subscribeUser(
            @PathVariable String userId,
            @Valid @RequestBody SubscriptionCreateRequest request) {
        return ResponseEntity.ok(userService.handleSubscription(userId, request));
    }

    @PutMapping("/me/subscribe")
    public ResponseEntity<UserResponse> selfSubscribe(
            @Valid @RequestBody SubscriptionCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.handleSelfSubscription(currentUser, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal User currentUser) {
        userService.changePassword(currentUser, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    @GetMapping("/{userId}/active-orders")
    @PreAuthorize("hasAuthority('ROLE_SERVICEMAN')")
    public ResponseEntity<List<OrderResponse>> getUserActiveOrders(
            @PathVariable String userId) {
        
        // Fetch all orders for user
        List<Order> allOrders = orderRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId);
        
        // Filter out orders where all items are picked_up
        List<OrderResponse> activeOrders = allOrders.stream()
                .filter(order -> order.getItems().stream()
                        .anyMatch(item -> !"picked_up".equals(item.getStatus())))
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(activeOrders);
    }
}