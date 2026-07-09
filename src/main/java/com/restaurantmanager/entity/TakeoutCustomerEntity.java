package com.restaurantmanager.entity;

import com.restaurantmanager.block.CashRegisterBlock;
import com.restaurantmanager.food.FoodRequest;
import com.restaurantmanager.item.TakeoutPackageItem;
import com.restaurantmanager.seating.MarkedPoint;
import com.restaurantmanager.seating.RestaurantSeatingApi;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class TakeoutCustomerEntity extends CustomerEntity {
    public TakeoutCustomerEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }

    @Override
    protected CustomerServiceType getConfiguredServiceType() {
        return CustomerServiceType.TAKEOUT;
    }

    @Override
    public boolean receiveTakeoutPackage(Player player, ItemStack packageStack) {
        if (serviceType != CustomerServiceType.TAKEOUT || state != CustomerState.WAITING_FOR_PACKAGE) {
            player.displayClientMessage(Component.literal("This customer is not waiting for takeout."), false);
            return false;
        }
        List<ItemStack> foods = TakeoutPackageItem.getFoods(packageStack);
        if (foods.isEmpty()) {
            player.displayClientMessage(Component.literal("The package is empty."), false);
            return false;
        }
        Optional<FoodRequest> missingRequest = findMissingTakeoutRequest(foods);
        if (missingRequest.isPresent()) {
            String message = "Package is missing: " + missingRequest.get().getDescription();
            player.displayClientMessage(Component.literal(message), false);
            reactToWrongOrder(message);
            wrongOrderCooldown = 20 * 4;
            return false;
        }

        int reward = requests.stream()
                .filter(java.util.Objects::nonNull)
                .mapToInt(this::getScaledReward)
                .sum();
        if (player instanceof ServerPlayer serverPlayer && reward > 0) {
            serverPlayer.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, reward));
        }
        player.displayClientMessage(Component.literal("Takeout accepted. Here's your change."), false);
        TakeoutPackageItem.clearFood(packageStack);
        completeOrderFromContainer();
        releaseReservedCounter();
        return true;
    }

    @Override
    public boolean serveTakeoutFromHeldPackage(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof TakeoutPackageItem) {
            return receiveTakeoutPackage(player, mainHand);
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof TakeoutPackageItem) {
            return receiveTakeoutPackage(player, offHand);
        }
        player.displayClientMessage(Component.literal("Hold a takeout package to serve this order."), false);
        return false;
    }

    @Override
    protected boolean findAndReserveCounter() {
        if (!(this.level() instanceof ServerLevel serverLevel) || restaurant == null) {
            return false;
        }
        if (reservedCounterPos != null) {
            refreshCounterTarget();
            return reservedCounterPos != null;
        }
        if (counterSearchCooldown > 0) {
            return false;
        }

        Optional<BlockPos> restaurantCounter = restaurant.getCounterPos();
        restaurantCounter.ifPresent(counterPos -> RestaurantSeatingApi.markCounter(counterPos, serverLevel));
        if (restaurantCounter.isPresent()
                && RestaurantSeatingApi.occupyCounter(restaurantCounter.get(), this.getUUID(), serverLevel)) {
            reservedCounterPos = restaurantCounter.get();
            counterTarget = getCashRegisterInteractionPosition(reservedCounterPos, serverLevel)
                    .or(() -> RestaurantSeatingApi.getPoint(reservedCounterPos, serverLevel)
                            .map(MarkedPoint::getInteractionPosition))
                    .orElse(Vec3.atBottomCenterOf(reservedCounterPos.above()));
            counterSearchCooldown = 0;
            return true;
        }

        AABB bounds = restaurant.getBounds();
        BlockPos center = BlockPos.containing(bounds.getCenter());
        double radius = Math.max(8.0, Math.max(bounds.getXsize(), bounds.getZsize()) + 4.0);
        Optional<MarkedPoint> counter = RestaurantSeatingApi.findNearestAvailableCounter(center, radius, serverLevel);
        if (counter.isEmpty()) {
            counterSearchCooldown = staggeredCooldown(RESERVATION_SEARCH_INTERVAL);
            return false;
        }

        MarkedPoint point = counter.get();
        if (!RestaurantSeatingApi.occupyCounter(point.getPos(), this.getUUID(), serverLevel)) {
            counterSearchCooldown = staggeredCooldown(RESERVATION_SEARCH_INTERVAL);
            return false;
        }
        reservedCounterPos = point.getPos();
        counterTarget = getCounterTarget(point.getPos(), serverLevel)
                .orElse(point.getInteractionPosition());
        counterSearchCooldown = 0;
        return true;
    }

    @Override
    protected Optional<Vec3> getCounterTarget(BlockPos pos, ServerLevel serverLevel) {
        return getCashRegisterInteractionPosition(pos, serverLevel)
                .or(() -> RestaurantSeatingApi.getPoint(pos, serverLevel)
                        .map(MarkedPoint::getInteractionPosition));
    }

    @Override
    protected Optional<Vec3> getCashRegisterInteractionPosition(BlockPos pos, ServerLevel serverLevel) {
        var state = serverLevel.getBlockState(pos);
        if (!state.is(com.restaurantmanager.setup.ModBlocks.CASH_REGISTER.get()) || !state.hasProperty(CashRegisterBlock.FACING)) {
            return Optional.empty();
        }
        return Optional.of(RestaurantSeatingApi.getCounterInteractionPosition(pos, state.getValue(CashRegisterBlock.FACING), serverLevel));
    }

    @Override
    protected void refreshCounterTarget() {
        if (!(this.level() instanceof ServerLevel serverLevel) || reservedCounterPos == null) {
            return;
        }
        RestaurantSeatingApi.markCounter(reservedCounterPos, serverLevel);
        counterTarget = getCounterTarget(reservedCounterPos, serverLevel)
                .orElse(Vec3.atBottomCenterOf(reservedCounterPos.above()));
    }

    @Override
    protected boolean canLeaveQueueForCounter() {
        if (restaurant == null) {
            return false;
        }
        return findAndReserveCounter();
    }

    @Override
    protected void releaseReservedCounter() {
        if (reservedCounterPos != null && this.level() instanceof ServerLevel serverLevel) {
            RestaurantSeatingApi.releaseCounter(reservedCounterPos, this.getUUID(), serverLevel);
            reservedCounterPos = null;
            counterTarget = null;
        }
    }

    @Override
    protected void holdCounterPosition() {
        if (counterTarget == null) {
            return;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.distanceToSqr(counterTarget.x, counterTarget.y, counterTarget.z) > 0.5) {
            this.setPos(counterTarget.x, counterTarget.y, counterTarget.z);
        }
        faceReservedCounter();
    }

    @Override
    protected void faceReservedCounter() {
        if (reservedCounterPos == null) {
            return;
        }

        Vec3 counterCenter = Vec3.atCenterOf(reservedCounterPos);
        double dx = counterCenter.x - this.getX();
        double dz = counterCenter.z - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }
}
