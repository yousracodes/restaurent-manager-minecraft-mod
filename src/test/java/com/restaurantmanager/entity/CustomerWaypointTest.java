package com.restaurantmanager.entity;

/** Run directly with Java 17; no Minecraft client or test framework required. */
public final class CustomerWaypointTest {
    public static void main(String[] args) {
        check(true, 10.5, 64, 20.5, "center of approach block");
        check(true, 11.1, 64, 21.1, "navigation completed diagonally outside old sitting radius");
        check(true, 11.2, 64, 20.8, "navigation completed outside old sitting radius on one axis");
        check(true, 10.8, 64.5, 20.8, "partial block floor");
        check(false, 12, 64, 20.8, "customer still far from destination");
        check(false, 10.8, 65, 20.8, "customer on a different floor");
        check(false, 10.8, 64, 19.8, "customer on the wrong side of the block");
        System.out.println("7 customer waypoint regression checks passed.");
    }

    private static void check(boolean expected, double x, double y, double z, String scenario) {
        if (CustomerWaypoint.hasArrived(x, y, z, 10, 64, 20, 0.6F) != expected) {
            throw new AssertionError(scenario);
        }
    }
}
