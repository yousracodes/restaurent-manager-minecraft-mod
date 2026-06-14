package com.restaurantmanager.restaurant;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class Restaurant {
    private final UUID id;
    private BlockPos signPos;
    private AABB bounds;
    private int reputation;
    private int customersServed;
    private int ordersCompleted;
    private int ordersFailed;
    private int customersLost;
    private int moneyEarned;
    private boolean open;
    private boolean active;
    private String name;
    private UUID ownerUuid;
    private String ownerName;
    private final RestaurantMenu menu;

    public Restaurant(UUID id, BlockPos signPos, AABB bounds, int reputation, int customersServed, int ordersCompleted,
                      int ordersFailed, int customersLost, int moneyEarned, boolean open, String name,
                      UUID ownerUuid, String ownerName, RestaurantMenu menu) {
        this(id, signPos, bounds, reputation, customersServed, ordersCompleted, ordersFailed, customersLost,
                moneyEarned, open, true, name, ownerUuid, ownerName, menu);
    }

    public Restaurant(UUID id, BlockPos signPos, AABB bounds, int reputation, int customersServed, int ordersCompleted,
                      int ordersFailed, int customersLost, int moneyEarned, boolean open, boolean active, String name,
                      UUID ownerUuid, String ownerName, RestaurantMenu menu) {
        this.id = id;
        this.signPos = signPos;
        this.bounds = bounds;
        this.reputation = reputation;
        this.customersServed = customersServed;
        this.ordersCompleted = ordersCompleted;
        this.ordersFailed = ordersFailed;
        this.customersLost = customersLost;
        this.moneyEarned = moneyEarned;
        this.open = open;
        this.active = active;
        this.name = name == null || name.isBlank() ? "Restaurant" : name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName == null ? "" : ownerName;
        this.menu = menu;
    }

    public Restaurant(UUID id, BlockPos signPos, AABB bounds, int reputation, int customersServed, boolean open, RestaurantMenu menu) {
        this(id, signPos, bounds, reputation, customersServed, 0, 0, 0, 0, open, "Restaurant", null, "", menu);
    }

    public Restaurant(UUID id, BlockPos signPos, AABB bounds, int reputation, int customersServed, boolean open) {
        this(id, signPos, bounds, reputation, customersServed, open, new RestaurantMenu());
    }

    public Restaurant(UUID id, BlockPos signPos, AABB bounds, int reputation, int customersServed) {
        this(id, signPos, bounds, reputation, customersServed, true);
    }

    public Restaurant(BlockPos signPos, AABB bounds) {
        this(UUID.randomUUID(), signPos, bounds, 0, 0, true);
    }

    public UUID getId() {
        return id;
    }

    public BlockPos getSignPos() {
        return signPos;
    }

    public AABB getBounds() {
        return bounds;
    }

    public void updateAnchor(BlockPos signPos, AABB bounds) {
        this.signPos = signPos;
        this.bounds = bounds;
        this.active = true;
    }

    public int getReputation() {
        return reputation;
    }

    public int getCustomersServed() {
        return customersServed;
    }

    public int getOrdersCompleted() {
        return ordersCompleted;
    }

    public int getOrdersFailed() {
        return ordersFailed;
    }

    public int getCustomersLost() {
        return customersLost;
    }

    public int getMoneyEarned() {
        return moneyEarned;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean hasOwner() {
        return ownerUuid != null;
    }

    public void setIdentity(String name, UUID ownerUuid, String ownerName) {
        this.name = name == null || name.isBlank() ? "Restaurant" : name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName == null ? "" : ownerName;
    }

    public void setName(String name) {
        this.name = name == null || name.isBlank() ? "Restaurant" : name;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isActive() {
        return active;
    }

    public void detachSign() {
        this.active = false;
    }

    public RestaurantMenu getMenu() {
        return menu;
    }

    public void addCustomerServed() {
        this.customersServed++;
    }

    public void addReputation(int amount) {
        this.reputation += amount;
    }

    public void addOrderCompleted() {
        this.ordersCompleted++;
    }

    public void addOrderFailed() {
        this.ordersFailed++;
    }

    public void addCustomerLost() {
        this.customersLost++;
    }

    public void addMoneyEarned(int amount) {
        this.moneyEarned += amount;
    }
}
