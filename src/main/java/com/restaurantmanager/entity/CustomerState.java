package com.restaurantmanager.entity;

public enum CustomerState {
    SPAWN("Arriving"),
    FINDING_RESTAURANT("Finding restaurant"),
    ENTERING("Entering"),
    JOINING_QUEUE("Joining queue"),
    WAITING_IN_QUEUE("Waiting in queue"),
    MOVING_TO_SEAT("Moving to seat"),
    SEATED("Seated"),
    REQUESTING("Ordering"),
    WAITING_FOR_PLATE("Waiting for plate"),
    EATING("Eating"),
    MOVING_TO_COUNTER("Moving to counter"),
    WAITING_AT_COUNTER("At counter"),
    WAITING_FOR_PACKAGE("Waiting for package"),
    PAYING("Paying"),
    SERVED("Served"),
    LEAVING("Leaving");

    private final String displayName;

    CustomerState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
