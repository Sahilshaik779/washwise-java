package com.washwise.backend.domain.user;

import com.washwise.backend.domain.user.dto.UserResponse; 
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getMembershipPlan(),
                user.getMembershipExpiryDate(),
                user.getMonthlyServicesUsed()
        );
    }
}