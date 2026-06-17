package com.restaurantmanager.restaurant;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private BlockPos doorBellPos;
    private BlockPos counterPos;
    private final List<RestaurantMenu> menus = new ArrayList<>();
    private final List<SeatTableLink> seatTableLinks = new ArrayList<>();

    public Restaurant(UUID id, BlockPos signPos, AABB bounds, int reputation, int customersServed, int ordersCompleted,
                      int ordersFailed, int customersLost, int moneyEarned, boolean open, String name,
                      UUID ownerUuid, String ownerName, RestaurantMenu menu) {
        this(id, signPos, bounds, reputation, customersServed, ordersCompleted, ordersFailed, customersLost,
                moneyEarned, open, true, name, ownerUuid, ownerName, menu == null ? List.of() : List.of(menu));
    }

    public Restaurant(UUID id, BlockPos signPos, AABB bounds, int reputation, int customersServed, int ordersCompleted,
                      int ordersFailed, int customersLost, int moneyEarned, boolean open, boolean active, String name,
                      UUID ownerUuid, String ownerName, RestaurantMenu menu) {
        this(id, signPos, bounds, reputation, customersServed, ordersCompleted, ordersFailed, customersLost,
                moneyEarned, open, active, name, ownerUuid, ownerName, menu == null ? List.of() : List.of(menu));
    }

    public Restaurant(UUID id, BlockPos signPos, AABB bounds, int reputation, int customersServed, int ordersCompleted,
                      int ordersFailed, int customersLost, int moneyEarned, boolean open, boolean active, String name,
                      UUID ownerUuid, String ownerName, List<RestaurantMenu> menus) {
        this(id, signPos, bounds, reputation, customersServed, ordersCompleted, ordersFailed, customersLost,
                moneyEarned, open, active, name, ownerUuid, ownerName, menus, null, null, List.of());
    }

    public Restaurant(UUID id, BlockPos signPos, AABB bounds, int reputation, int customersServed, int ordersCompleted,
                      int ordersFailed, int customersLost, int moneyEarned, boolean open, boolean active, String name,
                      UUID ownerUuid, String ownerName, List<RestaurantMenu> menus, BlockPos doorBellPos,
                      BlockPos counterPos, List<SeatTableLink> seatTableLinks) {
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
        this.doorBellPos = doorBellPos == null ? null : doorBellPos.immutable();
        this.counterPos = counterPos == null ? null : counterPos.immutable();
        replaceMenus(menus);
        replaceSeatTableLinks(seatTableLinks);
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

    public Optional<BlockPos> getDoorBellPos() {
        return Optional.ofNullable(doorBellPos);
    }

    public boolean hasDoorBell(BlockPos pos) {
        return doorBellPos != null && doorBellPos.equals(pos);
    }

    public void setDoorBellPos(BlockPos doorBellPos) {
        this.doorBellPos = doorBellPos == null ? null : doorBellPos.immutable();
    }

    public void clearDoorBellPos(BlockPos pos) {
        if (hasDoorBell(pos)) {
            this.doorBellPos = null;
        }
    }

    public Optional<BlockPos> getCounterPos() {
        return Optional.ofNullable(counterPos);
    }

    public boolean hasCounter(BlockPos pos) {
        return counterPos != null && counterPos.equals(pos);
    }

    public void setCounterPos(BlockPos counterPos) {
        this.counterPos = counterPos == null ? null : counterPos.immutable();
    }

    public void clearCounterPos(BlockPos pos) {
        if (hasCounter(pos)) {
            this.counterPos = null;
        }
    }

    public List<SeatTableLink> getSeatTableLinks() {
        return Collections.unmodifiableList(seatTableLinks);
    }

    public void addSeatTableLink(BlockPos chairPos, BlockPos tablePos) {
        seatTableLinks.removeIf(link -> link.hasChair(chairPos));
        seatTableLinks.add(new SeatTableLink(chairPos, tablePos));
    }

    public void removeSeatTableLinkAt(BlockPos pos) {
        seatTableLinks.removeIf(link -> link.contains(pos));
    }

    public void replaceSeatTableLinks(List<SeatTableLink> links) {
        seatTableLinks.clear();
        if (links != null) {
            for (SeatTableLink link : links) {
                if (link != null) {
                    seatTableLinks.add(link);
                }
            }
        }
    }

    public void updateAnchor(BlockPos signPos, AABB bounds) {
        this.signPos = signPos;
        this.bounds = bounds;
        this.active = true;
    }

    public int getReputation() {
        return reputation;
    }

    public int getStarRating() {
        if (reputation >= 1000) {
            return 5;
        }
        if (reputation >= 500) {
            return 4;
        }
        if (reputation >= 250) {
            return 3;
        }
        if (reputation >= 100) {
            return 2;
        }
        return 1;
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

    public boolean isOwner(UUID playerUuid) {
        return ownerUuid == null || ownerUuid.equals(playerUuid);
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
        return menus.isEmpty() ? addOrReplaceMenu(new RestaurantMenu()) : menus.get(0);
    }

    public List<RestaurantMenu> getMenus() {
        return Collections.unmodifiableList(menus);
    }

    public Optional<RestaurantMenu> getMenuByCategory(MenuCategory category) {
        MenuCategory resolvedCategory = category == null ? MenuCategory.MAIN_DISH : category;
        return menus.stream().filter(menu -> menu.getCategory() == resolvedCategory).findFirst();
    }

    public RestaurantMenu getOrCreateMenu(MenuCategory category) {
        return getMenuByCategory(category).orElseGet(() -> addOrReplaceMenu(new RestaurantMenu("", category, List.of())));
    }

    public RestaurantMenu addOrReplaceMenu(RestaurantMenu menu) {
        RestaurantMenu resolvedMenu = menu == null ? new RestaurantMenu() : menu;
        removeMenu(resolvedMenu.getCategory());
        menus.add(resolvedMenu);
        return resolvedMenu;
    }

    public boolean removeMenu(MenuCategory category) {
        MenuCategory resolvedCategory = category == null ? MenuCategory.MAIN_DISH : category;
        return menus.removeIf(menu -> menu.getCategory() == resolvedCategory);
    }

    public void replaceMenus(List<RestaurantMenu> newMenus) {
        menus.clear();
        if (newMenus != null) {
            for (RestaurantMenu menu : newMenus) {
                if (menu != null) {
                    addOrReplaceMenu(menu);
                }
            }
        }
        if (menus.isEmpty()) {
            menus.add(new RestaurantMenu());
        }
    }

    public int getMenuItemCount() {
        return menus.stream().mapToInt(menu -> menu.getItems().size()).sum();
    }

    public boolean hasMenuItems() {
        return menus.stream().anyMatch(menu -> !menu.isEmpty());
    }

    public int getUniqueMenuItemCount() {
        Set<net.minecraft.resources.ResourceLocation> uniqueItems = new HashSet<>();
        for (RestaurantMenu menu : menus) {
            uniqueItems.addAll(menu.getItems());
        }
        return uniqueItems.size();
    }

    public int getMenuCategoryCount() {
        return (int) menus.stream()
                .filter(menu -> !menu.isEmpty())
                .map(RestaurantMenu::getCategory)
                .distinct()
                .count();
    }

    public int getVarietyReputationBonus() {
        int bonus = 0;
        int uniqueItems = getUniqueMenuItemCount();
        int categories = getMenuCategoryCount();

        if (uniqueItems >= 5) {
            bonus++;
        }
        if (uniqueItems >= 10) {
            bonus++;
        }
        if (categories >= 3) {
            bonus++;
        }
        if (categories >= 5) {
            bonus++;
        }
        return bonus;
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
