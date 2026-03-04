package com.washwise.backend.domain.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import java.util.Arrays;
import java.util.List;

@Getter
public enum LaundryServiceType {
    WASH_AND_FOLD("Wash and Fold", 10.0, "pending", "started", "washing", "folding", "ready_for_pickup", "picked_up"),
    WASH_AND_IRON("Wash and Iron", 25.0, "pending", "started", "washing", "ironing", "ready_for_pickup", "picked_up"),
    PREMIUM_WASH("Premium Wash", 40.0, "pending", "started", "inspection", "pre_treatment", "washing", "drying", "quality_check", "ready_for_pickup", "picked_up"),
    DRY_CLEANING("Dry Cleaning", 50.0, "pending", "started", "tagging", "pre_treatment", "dry_cleaning", "pressing", "finishing", "ready_for_pickup", "picked_up"),
    STEAM_IRON("Steam Iron", 15.0, "pending", "started", "steaming", "pressing", "finishing", "ready_for_pickup", "picked_up");

    private final String displayName;
    private final double price;
    private final List<String> workflow;

    LaundryServiceType(String displayName, double price, String... workflowStages) {
        this.displayName = displayName;
        this.price = price;
        this.workflow = Arrays.asList(workflowStages);
    }

    @JsonCreator
    public static LaundryServiceType fromString(String value) {
        if (value == null) {
            return null;
        }
        return LaundryServiceType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name().toLowerCase();
    }
}