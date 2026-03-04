package com.washwise.backend.domain.user;

import com.washwise.backend.domain.user.dto.SubscriptionCreateRequest;
import com.washwise.backend.domain.user.dto.UserResponse;
import com.washwise.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper; // Assume MapStruct mapper is created to map Entity to Record

    @Override
    @Transactional
    public UserResponse handleSubscription(String userId, SubscriptionCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return updateSubscription(user, request.plan());
    }

    @Override
    @Transactional
    public UserResponse handleSelfSubscription(User currentUser, SubscriptionCreateRequest request) {
        return updateSubscription(currentUser, request.plan());
    }

    private UserResponse updateSubscription(User user, MembershipPlanEnum plan) {
        user.setMembershipPlan(plan);
        if (plan != MembershipPlanEnum.NONE) {
            user.setMembershipExpiryDate(ZonedDateTime.now().plusDays(365));
            user.setMonthlyServicesUsed(new HashMap<>()); // Reset usage
        } else {
            user.setMembershipExpiryDate(null);
        }
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public String getOrCreateQr(User user) {
        if (user.getStaticQrCodes() != null && user.getStaticQrCodes().containsKey("user_qr_filename")) {
            return user.getStaticQrCodes().get("user_qr_filename");
        }

        String filename = "user_" + user.getId() + ".png";
        // Logic to generate QR goes here (similar to utils.generate_qr)
        
        Map<String, String> qrData = user.getStaticQrCodes() == null ? new HashMap<>() : user.getStaticQrCodes();
        qrData.put("user_qr_filename", filename);
        user.setStaticQrCodes(qrData);
        userRepository.save(user);
        
        return filename;
    }

    @Override
    @Transactional
    public void changePassword(User currentUser, String currentPassword, String newPassword) {
        if (currentUser.getPassword() == null || !passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password");
        }
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
    }

    @Override
    @Transactional
    public void deleteUser(String userId, User currentUser) {
        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot delete yourself");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user); // Cascade rule on Entity handles Order deletion
    }
}