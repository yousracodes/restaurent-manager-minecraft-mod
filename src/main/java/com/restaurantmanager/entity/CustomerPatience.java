package com.restaurantmanager.entity;

public enum CustomerPatience {
    HAPPY("Happy"),
    NEUTRAL("Neutral"),
    IMPATIENT("Impatient"),
    ANGRY("Angry"),
    LEAVING("Leaving");

    private final String displayName;

    CustomerPatience(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
