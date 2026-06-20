package com.restaurantmanager.restaurant;

import com.restaurantmanager.RestaurantManagerMod;
import com.restaurantmanager.block.CashRegisterBlock;
import com.restaurantmanager.block.DoorBellBlock;
import com.restaurantmanager.setup.ModBlocks;
import com.restaurantmanager.seating.RestaurantSeatingApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RestaurantManager extends SavedData {
    private static final String KEY = "restaurant_manager_restaurants";
    private static final double LAYOUT_BIND_RANGE = 6.0;
    private final List<Restaurant> restaurants = new ArrayList<>();

    public static RestaurantManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RestaurantManager::load, RestaurantManager::new, KEY);
    }

    private static RestaurantManager load(CompoundTag compound) {
        RestaurantManager manager = new RestaurantManager();
        ListTag list = compound.getList("restaurants", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            manager.restaurants.add(restaurantFromCompound(list.getCompound(i)));
        }
        return manager;
    }

    public RestaurantManager() {
    }

    public List<Restaurant> getRestaurants() {
        return restaurants;
    }

    public void addRestaurant(Restaurant restaurant) {
        restaurants.add(restaurant);
        setDirty();
    }

    public void removeRestaurantAt(BlockPos signPos) {
        restaurants.removeIf(r -> r.getSignPos().equals(signPos));
        setDirty();
    }

    public void detachRestaurantAt(BlockPos signPos) {
        findBySignPos(signPos).ifPresent(restaurant -> {
            restaurant.detachSign();
            setDirty();
        });
    }

    public Optional<Restaurant> findById(UUID id) {
        return restaurants.stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    public Optional<Restaurant> findByPosition(BlockPos pos) {
        return restaurants.stream()
                .filter(Restaurant::isActive)
                .filter(r -> r.getBounds().contains(pos.getX(), pos.getY(), pos.getZ()))
                .findFirst();
    }

    public Optional<Restaurant> findBySignPos(BlockPos signPos) {
        return restaurants.stream()
                .filter(Restaurant::isActive)
                .filter(r -> r.getSignPos().equals(signPos))
                .findFirst();
    }

    public Optional<Restaurant> findForLayoutBlock(BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        return restaurants.stream()
                .filter(Restaurant::isActive)
                .filter(restaurant -> restaurant.getBounds().inflate(LAYOUT_BIND_RANGE).contains(
                        center.x,
                        center.y,
                        center.z
                ))
                .min((first, second) -> Double.compare(
                        first.getBounds().getCenter().distanceToSqr(center),
                        second.getBounds().getCenter().distanceToSqr(center)
                ));
    }

    public void refreshLayoutBlocks(Restaurant restaurant, ServerLevel level) {
        if (restaurant == null || !restaurant.isActive()) {
            return;
        }

        Vec3 center = restaurant.getBounds().getCenter();
        AABB searchBounds = restaurant.getBounds().inflate(LAYOUT_BIND_RANGE);
        BlockPos min = BlockPos.containing(searchBounds.minX, searchBounds.minY, searchBounds.minZ);
        BlockPos max = BlockPos.containing(searchBounds.maxX, searchBounds.maxY, searchBounds.maxZ);

        BlockPos nearestDoorBell = null;
        BlockState nearestDoorBellState = null;
        double nearestDoorBellDistance = Double.MAX_VALUE;
        BlockPos nearestCounter = null;
        BlockState nearestCounterState = null;
        double nearestCounterDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.DOOR_BELL.get()) && belongsToRestaurant(pos, restaurant)) {
                double distance = Vec3.atCenterOf(pos).distanceToSqr(center);
                if (distance < nearestDoorBellDistance) {
                    nearestDoorBell = pos.immutable();
                    nearestDoorBellState = state;
                    nearestDoorBellDistance = distance;
                }
            } else if (state.is(ModBlocks.CASH_REGISTER.get()) && belongsToRestaurant(pos, restaurant)) {
                double distance = Vec3.atCenterOf(pos).distanceToSqr(center);
                if (distance < nearestCounterDistance) {
                    nearestCounter = pos.immutable();
                    nearestCounterState = state;
                    nearestCounterDistance = distance;
                }
            }
        }

        if (nearestDoorBell != null) {
            restaurant.setDoorBellPos(nearestDoorBell);
            Direction facing = nearestDoorBellState != null && nearestDoorBellState.hasProperty(DoorBellBlock.FACING)
                    ? nearestDoorBellState.getValue(DoorBellBlock.FACING)
                    : Direction.NORTH;
            RestaurantSeatingApi.markDoorBell(nearestDoorBell, facing, level);
        }
        if (nearestCounter != null) {
            restaurant.setCounterPos(nearestCounter);
            Direction facing = nearestCounterState != null && nearestCounterState.hasProperty(CashRegisterBlock.FACING)
                    ? nearestCounterState.getValue(CashRegisterBlock.FACING)
                    : Direction.NORTH;
            RestaurantSeatingApi.markCounter(nearestCounter, facing, level);
        }
        if (nearestDoorBell != null || nearestCounter != null) {
            setDirty();
        }
    }

    private boolean belongsToRestaurant(BlockPos pos, Restaurant restaurant) {
        return findForLayoutBlock(pos)
                .map(owner -> owner.getId().equals(restaurant.getId()))
                .orElse(false);
    }

    public Optional<Restaurant> findForDoorBell(BlockPos bellPos) {
        return findForLayoutBlock(bellPos);
    }

    public Optional<Restaurant> assignDoorBell(BlockPos bellPos) {
        Optional<Restaurant> owner = findForLayoutBlock(bellPos);
        owner.ifPresent(restaurant -> {
            restaurant.setDoorBellPos(bellPos);
            setDirty();
        });
        return owner;
    }

    public void clearDoorBell(BlockPos bellPos) {
        boolean changed = false;
        for (Restaurant restaurant : restaurants) {
            if (restaurant.hasDoorBell(bellPos)) {
                restaurant.clearDoorBellPos(bellPos);
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public Optional<Restaurant> assignCounter(BlockPos counterPos) {
        Optional<Restaurant> owner = findForLayoutBlock(counterPos);
        owner.ifPresent(restaurant -> {
            restaurant.setCounterPos(counterPos);
            setDirty();
        });
        return owner;
    }

    public void clearCounter(BlockPos counterPos) {
        boolean changed = false;
        for (Restaurant restaurant : restaurants) {
            if (restaurant.hasCounter(counterPos)) {
                restaurant.clearCounterPos(counterPos);
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public Optional<Restaurant> linkSeatToTable(BlockPos chairPos, BlockPos tablePos) {
        Optional<Restaurant> chairRestaurant = findForLayoutBlock(chairPos);
        Optional<Restaurant> tableRestaurant = findForLayoutBlock(tablePos);
        if (chairRestaurant.isEmpty() || tableRestaurant.isEmpty()
                || !chairRestaurant.get().getId().equals(tableRestaurant.get().getId())) {
            return Optional.empty();
        }
        Restaurant restaurant = chairRestaurant.get();
        restaurant.addSeatTableLink(chairPos, tablePos);
        setDirty();
        return Optional.of(restaurant);
    }

    public boolean overlaps(AABB bounds) {
        return overlaps(bounds, null);
    }

    public boolean overlaps(AABB bounds, UUID ignoredRestaurantId) {
        return restaurants.stream()
                .filter(Restaurant::isActive)
                .filter(r -> ignoredRestaurantId == null || !r.getId().equals(ignoredRestaurantId))
                .anyMatch(r -> r.getBounds().intersects(bounds));
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag list = new ListTag();
        for (Restaurant restaurant : restaurants) {
            restaurantToTag(list, restaurant);
        }
        compound.put("restaurants", list);
        return compound;
    }

    private static void restaurantToTag(ListTag list, Restaurant restaurant) {
        CompoundTag tag = new CompoundTag();
        restaurantToCompound(restaurant, tag);
        list.add(tag);
    }

    private static void restaurantToCompound(Restaurant restaurant, CompoundTag compound) {
        compound.putUUID("id", restaurant.getId());
        compound.put("signPos", NbtUtils.writeBlockPos(restaurant.getSignPos()));
        compound.put("bounds", writeAabb(restaurant.getBounds()));
        compound.putInt("reputation", restaurant.getReputation());
        compound.putInt("customersServed", restaurant.getCustomersServed());
        compound.putInt("ordersCompleted", restaurant.getOrdersCompleted());
        compound.putInt("ordersFailed", restaurant.getOrdersFailed());
        compound.putInt("customersLost", restaurant.getCustomersLost());
        compound.putInt("moneyEarned", restaurant.getMoneyEarned());
        compound.putBoolean("open", restaurant.isOpen());
        compound.putBoolean("active", restaurant.isActive());
        compound.putString("name", restaurant.getName());
        if (restaurant.getOwnerUuid() != null) {
            compound.putUUID("ownerUuid", restaurant.getOwnerUuid());
        }
        compound.putString("ownerName", restaurant.getOwnerName());
        restaurant.getDoorBellPos().ifPresent(pos -> compound.put("doorBellPos", NbtUtils.writeBlockPos(pos)));
        restaurant.getCounterPos().ifPresent(pos -> compound.put("counterPos", NbtUtils.writeBlockPos(pos)));
        compound.put("seatTableLinks", writeSeatTableLinks(restaurant.getSeatTableLinks()));
        compound.put("menus", writeMenus(restaurant.getMenus()));
    }

    private static Restaurant restaurantFromCompound(CompoundTag compound) {
        return new Restaurant(
                compound.getUUID("id"),
                NbtUtils.readBlockPos(compound.getCompound("signPos")),
                readAabb(compound.getCompound("bounds")),
                compound.getInt("reputation"),
                compound.getInt("customersServed"),
                compound.getInt("ordersCompleted"),
                compound.getInt("ordersFailed"),
                compound.getInt("customersLost"),
                compound.getInt("moneyEarned"),
                !compound.contains("open") || compound.getBoolean("open"),
                !compound.contains("active") || compound.getBoolean("active"),
                compound.contains("name") ? compound.getString("name") : "Restaurant",
                compound.hasUUID("ownerUuid") ? compound.getUUID("ownerUuid") : null,
                compound.getString("ownerName"),
                readMenus(compound),
                compound.contains("doorBellPos") ? NbtUtils.readBlockPos(compound.getCompound("doorBellPos")) : null,
                compound.contains("counterPos") ? NbtUtils.readBlockPos(compound.getCompound("counterPos")) : null,
                readSeatTableLinks(compound)
        );
    }

    private static ListTag writeSeatTableLinks(List<SeatTableLink> links) {
        ListTag list = new ListTag();
        for (SeatTableLink link : links) {
            CompoundTag tag = new CompoundTag();
            tag.put("chairPos", NbtUtils.writeBlockPos(link.getChairPos()));
            tag.put("tablePos", NbtUtils.writeBlockPos(link.getTablePos()));
            list.add(tag);
        }
        return list;
    }

    private static List<SeatTableLink> readSeatTableLinks(CompoundTag compound) {
        List<SeatTableLink> links = new ArrayList<>();
        if (!compound.contains("seatTableLinks", Tag.TAG_LIST)) {
            return links;
        }
        ListTag list = compound.getList("seatTableLinks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (tag.contains("chairPos") && tag.contains("tablePos")) {
                links.add(new SeatTableLink(
                        NbtUtils.readBlockPos(tag.getCompound("chairPos")),
                        NbtUtils.readBlockPos(tag.getCompound("tablePos"))
                ));
            }
        }
        return links;
    }

    private static ListTag writeMenus(List<RestaurantMenu> menus) {
        ListTag list = new ListTag();
        for (RestaurantMenu menu : menus) {
            CompoundTag menuTag = new CompoundTag();
            menuTag.putString("title", menu.getTitle());
            menuTag.putString("category", menu.getCategory().name());
            menuTag.put("items", writeMenuItems(menu));
            list.add(menuTag);
        }
        return list;
    }

    private static ListTag writeMenuItems(RestaurantMenu menu) {
        ListTag list = new ListTag();
        for (ResourceLocation item : menu.getItems()) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("id", item.toString());
            list.add(itemTag);
        }
        return list;
    }

    private static List<RestaurantMenu> readMenus(CompoundTag compound) {
        List<RestaurantMenu> menus = new ArrayList<>();
        if (compound.contains("menus", Tag.TAG_LIST)) {
            ListTag menuList = compound.getList("menus", Tag.TAG_COMPOUND);
            for (int i = 0; i < menuList.size(); i++) {
                CompoundTag menuTag = menuList.getCompound(i);
                menus.add(new RestaurantMenu(
                        menuTag.getString("title"),
                        MenuCategory.byName(menuTag.getString("category")),
                        readMenuItems(menuTag.getList("items", Tag.TAG_COMPOUND))
                ));
            }
        } else {
            menus.add(new RestaurantMenu(
                    compound.getString("menuTitle"),
                    MenuCategory.MAIN_DISH,
                    readMenuItems(compound.getList("menu", Tag.TAG_COMPOUND))
            ));
        }
        return menus;
    }

    private static List<ResourceLocation> readMenuItems(ListTag list) {
        List<ResourceLocation> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getCompound(i).getString("id"));
            if (id != null) {
                items.add(id);
            }
        }
        return items;
    }

    private static CompoundTag writeAabb(AABB bounds) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("minX", bounds.minX);
        tag.putDouble("minY", bounds.minY);
        tag.putDouble("minZ", bounds.minZ);
        tag.putDouble("maxX", bounds.maxX);
        tag.putDouble("maxY", bounds.maxY);
        tag.putDouble("maxZ", bounds.maxZ);
        return tag;
    }

    private static AABB readAabb(CompoundTag tag) {
        return new AABB(
                tag.getDouble("minX"),
                tag.getDouble("minY"),
                tag.getDouble("minZ"),
                tag.getDouble("maxX"),
                tag.getDouble("maxY"),
                tag.getDouble("maxZ")
        );
    }
}
