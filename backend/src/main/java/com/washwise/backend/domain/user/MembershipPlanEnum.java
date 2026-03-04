package com.washwise.backend.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MembershipPlanEnum {
    NONE,
    STANDARD,
    PREMIUM;

    @JsonCreator
    public static MembershipPlanEnum fromString(String value) {
        if (value == null) {
            return null;
        }
        return MembershipPlanEnum.valueOf(value.toUpperCase());
    }

    // ADD THIS: Forces Spring Boot to send "standard" instead of "STANDARD" to React
    @JsonValue
    public String toJson() {
        return this.name().toLowerCase();
    }
}