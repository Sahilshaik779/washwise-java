package com.washwise.backend.domain.user;

import com.washwise.backend.domain.user.dto.SubscriptionCreateRequest;
import com.washwise.backend.domain.user.dto.UserResponse;

public interface UserService {
    UserResponse handleSubscription(String userId, SubscriptionCreateRequest request);
    UserResponse handleSelfSubscription(User currentUser, SubscriptionCreateRequest request);
    String getOrCreateQr(User user);
    void changePassword(User currentUser, String currentPassword, String newPassword);
    void deleteUser(String userId, User currentUser);
}