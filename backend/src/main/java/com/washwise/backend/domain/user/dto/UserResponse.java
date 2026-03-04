package com.washwise.backend.domain.user.dto;

import com.washwise.backend.domain.user.MembershipPlanEnum;
import java.time.ZonedDateTime;
import java.util.Map;

public record UserResponse(
    String id,
    String username,
    String email,
    String role,
    MembershipPlanEnum membershipPlan,
    ZonedDateTime membershipExpiryDate,
    Map<String, Integer> monthlyServicesUsed
) {}