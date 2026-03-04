package com.washwise.backend.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;

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
}