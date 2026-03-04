package com.washwise.backend.domain.order;

import com.washwise.backend.domain.notification.NotificationService;
import com.washwise.backend.domain.order.dto.OrderDtos.*;
import com.washwise.backend.domain.user.MembershipPlanEnum;
import com.washwise.backend.domain.user.User;
import com.washwise.backend.domain.user.UserRepository;
import com.washwise.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Order createOrder(OrderCreateRequest request) {
        User owner = userRepository.findByUsername(request.customerUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        boolean isActiveSubscription = owner.getMembershipPlan() != MembershipPlanEnum.NONE &&
                owner.getMembershipExpiryDate() != null &&
                owner.getMembershipExpiryDate().isAfter(ZonedDateTime.now());

        Map<String, Integer> usage = owner.getMonthlyServicesUsed();
        if (usage == null) usage = new HashMap<>();

        Order order = Order.builder()
                .owner(owner)
                .paymentStatus("unpaid")
                .build();
        
        order = orderRepository.save(order); // Save to generate ID

        double totalCost = 0.0;
        boolean isFullyCovered = true;

        for (ServiceOrderItemCreate itemData : request.services()) {
            LaundryServiceType serviceType = itemData.serviceType();
            double itemCost = serviceType.getPrice() * itemData.quantity();
            boolean itemIsCovered = false;

            if (isActiveSubscription) {
                int currentUsage = usage.getOrDefault(serviceType.name(), 0);
                if (currentUsage < 4) {
                    boolean isEligible = owner.getMembershipPlan() == MembershipPlanEnum.PREMIUM ||
                            (owner.getMembershipPlan() == MembershipPlanEnum.STANDARD && 
                            (serviceType == LaundryServiceType.WASH_AND_FOLD || serviceType == LaundryServiceType.WASH_AND_IRON));

                    if (isEligible) {
                        itemCost = 0.0;
                        itemIsCovered = true;
                        usage.put(serviceType.name(), currentUsage + 1);
                    }
                }
            }

            if (!itemIsCovered) isFullyCovered = false;
            totalCost += itemCost;

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .serviceType(serviceType)
                    .quantity(itemData.quantity())
                    .cost(itemCost)
                    .status(serviceType.getWorkflow().get(0))
                    .build();
            
            order.getItems().add(orderItem);
        }

        order.setTotalCost(totalCost);
        order.setIsCoveredByPlan(isFullyCovered);
        if (totalCost == 0.0) order.setPaymentStatus("paid");

        owner.setMonthlyServicesUsed(usage);
        userRepository.save(owner);

        return orderRepository.save(order);
    }

    @Transactional
    public Order processPayment(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (!order.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Not your order");
        }

        order.setPaymentStatus("paid");
        return orderRepository.save(order);
    }

    @Transactional
    public OrderItem updateItemStatus(String itemId, String newStatus) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if (!item.getServiceType().getWorkflow().contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status " + newStatus);
        }

        item.setStatus(newStatus);
        orderItemRepository.save(item);

        User owner = item.getOrder().getOwner();
        String subject = "Order Update: " + item.getServiceType().getDisplayName();
        String body = "Your service status is now: <b>" + newStatus + "</b>";
        
        // In Spring, @Async on a method in NotificationService replaces BackgroundTasks
        notificationService.sendEmail(owner.getEmail(), subject, body);

        return item;
    }
}