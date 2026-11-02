package com.restaurantmanager.entity;

/** Matches Forge ground navigation's final waypoint arrival bounds. */
final class CustomerWaypoint {
    private CustomerWaypoint() {
    }

    static boolean hasArrived(double x, double y, double z, int nodeX, int nodeY, int nodeZ, float width) {
        double tolerance = width > 0.75F ? width / 2.0F : 0.75F - width / 2.0F;
        double offset = (width + 1) / 2D;
        return Math.abs(x - (nodeX + offset)) <= tolerance
                && Math.abs(z - (nodeZ + offset)) <= tolerance
                && Math.abs(y - nodeY) < 1.0D;
    }
}
