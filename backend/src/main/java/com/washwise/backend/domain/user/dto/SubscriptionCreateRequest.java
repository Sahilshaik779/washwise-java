package com.washwise.backend.domain.user.dto;

import com.washwise.backend.domain.user.MembershipPlanEnum;
import jakarta.validation.constraints.NotNull;

public record SubscriptionCreateRequest(
    @NotNull MembershipPlanEnum plan
) {}