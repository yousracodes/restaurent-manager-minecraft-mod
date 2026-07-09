package com.restaurantmanager.entity;

import com.restaurantmanager.block.PlateBlockEntity;
import com.restaurantmanager.food.FoodRequest;
import com.restaurantmanager.item.PlateItem;
import com.restaurantmanager.restaurant.SeatTableLink;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DineInCustomerEntity extends CustomerEntity {
    public DineInCustomerEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }

    @Override
    protected CustomerServiceType getConfiguredServiceType() {
        return CustomerServiceType.DINE_IN;
    }

    @Override
    public boolean receivePlate(Player player, ItemStack plateStack) {
        if (serviceType != CustomerServiceType.DINE_IN || state != CustomerState.WAITING_FOR_PLATE) {
            player.displayClientMessage(Component.literal("This customer is not waiting for dine-in food."), false);
            return false;
        }
        List<ItemStack> foods = PlateItem.getFoods(plateStack);
        if (foods.isEmpty()) {
            player.displayClientMessage(Component.literal("The plate is empty."), false);
            return false;
        }

        int matchingIndex = findMatchingRemainingRequestIndex(foods);
        if (matchingIndex < 0) {
            Optional<FoodRequest> missingRequest = findMissingTakeoutRequest(foods);
            String message;
            if (missingRequest.isPresent()) {
                message = "Plate is missing: " + missingRequest.get().getDescription();
            } else {
                message = "Plate does not contain a requested item.";
            }
            player.displayClientMessage(Component.literal(message), false);
            reactToWrongOrder(message);
            wrongOrderCooldown = 20 * 4;
            return false;
        }

        completePlateRequest(player, plateStack, foods, matchingIndex);
        return true;
    }

    @Override
    public boolean serveDineInFromHeldPlate(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof PlateItem) {
            return receivePlate(player, mainHand);
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof PlateItem) {
            return receivePlate(player, offHand);
        }
        player.displayClientMessage(Component.literal("Hold a plate to serve this order."), false);
        return false;
    }

    private void completePlateRequest(Player player, ItemStack plateStack, List<ItemStack> foods, int requestIndex) {
        FoodRequest request = requests.get(requestIndex);
        int reward = getScaledReward(request);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getInventory().placeItemBackInInventory(new ItemStack(net.minecraft.world.item.Items.EMERALD, reward));
        }
        player.displayClientMessage(Component.literal("Plate accepted. Here's your change."), false);

        removeServedFoodFromPlate(plateStack, foods, request);
        requests.set(requestIndex, null);
        while (currentRequestIndex < requests.size() && requests.get(currentRequestIndex) == null) {
            currentRequestIndex++;
        }

        wrongOrderCooldown = 0;
        waitTimer = 0;
        if (isOrderComplete()) {
            state = CustomerState.SERVED;
            this.setCustomName(Component.literal("Order complete"));
            this.setCustomNameVisible(true);
        } else {
            state = CustomerState.WAITING_FOR_PLATE;
            updateRequestName();
        }
    }

    @Override
    protected void scanForPlateMeal() {
        if (++plateScanTimer < PLATE_SCAN_INTERVAL || assignedTablePos == null) {
            return;
        }
        plateScanTimer = 0;
        findLinkedTablePlateWithCompleteOrder()
                .ifPresent(plate -> {
                    Optional<FoodRequest> missingRequest = findMissingTakeoutRequest(plate.getMeals());
                    if (missingRequest.isPresent()) {
                        reactToWrongOrder("Plate is missing: " + missingRequest.get().getDescription());
                        return;
                    }
                    servedPlatePos = plate.getBlockPos();
                    state = CustomerState.EATING;
                    eatingTimer = 0;
                    this.setCustomName(Component.literal("Eating"));
                    this.setCustomNameVisible(true);
                });
    }

    private Optional<PlateBlockEntity> findLinkedTablePlateWithCompleteOrder() {
        return getPlateScanPositions().stream()
                .map(pos -> this.level().getBlockEntity(pos))
                .filter(PlateBlockEntity.class::isInstance)
                .map(PlateBlockEntity.class::cast)
                .filter(PlateBlockEntity::hasMeals)
                .filter(plate -> findMissingTakeoutRequest(plate.getMeals()).isEmpty())
                .findFirst();
    }

    private Optional<PlateBlockEntity> findLinkedTablePlateWithMeals() {
        return getPlateScanPositions().stream()
                .map(pos -> this.level().getBlockEntity(pos))
                .filter(PlateBlockEntity.class::isInstance)
                .map(PlateBlockEntity.class::cast)
                .filter(PlateBlockEntity::hasMeals)
                .findFirst();
    }

    private List<BlockPos> getPlateScanPositions() {
        List<BlockPos> positions = new ArrayList<>();
        addScanPosition(positions, assignedTablePos);
        addScanPosition(positions, assignedTablePos.above());
        addScanPosition(positions, assignedTablePos.above(2));
        return positions;
    }

    private void addScanPosition(List<BlockPos> positions, BlockPos pos) {
        if (!positions.contains(pos)) {
            positions.add(pos);
        }
    }

    @Override
    protected void finishEating() {
        if (servedPlatePos != null && this.level().getBlockEntity(servedPlatePos) instanceof PlateBlockEntity plate) {
            plate.clearMeals();
        }
        servedPlatePos = null;
        prepareToLeave();
        completeOrderFromContainer();
        releaseReservedSeat();
    }

    @Override
    protected boolean findAndReserveSeat() {
        if (!(this.level() instanceof ServerLevel serverLevel) || restaurant == null || reservedSeatPos != null) {
            return reservedSeatPos != null;
        }
        if (seatSearchCooldown > 0) {
            return false;
        }

        Optional<SeatTableLink> linkedSeat = restaurant.getSeatTableLinks().stream()
                .filter(link -> RestaurantSeatingApi.occupySeat(link.getChairPos(), this.getUUID(), serverLevel))
                .findFirst();
        if (linkedSeat.isPresent()) {
            SeatTableLink link = linkedSeat.get();
            reservedSeatPos = link.getChairPos();
            seatTarget = getSeatTarget(reservedSeatPos);
            assignedTablePos = link.getTablePos();
            noSeatWaitTimer = 0;
            seatSearchCooldown = 0;
            return true;
        }

        AABB bounds = restaurant.getBounds();
        BlockPos center = BlockPos.containing(bounds.getCenter());
        double radius = Math.max(8.0, Math.max(bounds.getXsize(), bounds.getZsize()) + 4.0);
        Optional<MarkedPoint> seat = RestaurantSeatingApi.findNearestAvailableSeat(center, radius, serverLevel);
        if (seat.isEmpty()) {
            seatSearchCooldown = staggeredCooldown(RESERVATION_SEARCH_INTERVAL);
            return false;
        }

        MarkedPoint point = seat.get();
        if (!RestaurantSeatingApi.occupySeat(point.getPos(), this.getUUID(), serverLevel)) {
            seatSearchCooldown = staggeredCooldown(RESERVATION_SEARCH_INTERVAL);
            return false;
        }
        reservedSeatPos = point.getPos();
        seatTarget = getSeatTarget(reservedSeatPos);
        assignedTablePos = RestaurantSeatingApi.getLinkedTable(reservedSeatPos, serverLevel)
                .or(() -> RestaurantSeatingApi.findNearestTable(reservedSeatPos, 4.0, serverLevel)
                        .map(MarkedPoint::getPos))
                .orElse(null);
        noSeatWaitTimer = 0;
        seatSearchCooldown = 0;
        return true;
    }

    @Override
    protected Vec3 getSeatTarget(BlockPos seatPos) {
        return new Vec3(seatPos.getX() + 0.5, seatPos.getY() - 0.1, seatPos.getZ() + 0.5);
    }

    @Override
    protected Vec3 getSeatApproachTarget() {
        if (reservedSeatPos == null) {
            return seatTarget;
        }
        return new Vec3(reservedSeatPos.getX() + 0.5, reservedSeatPos.getY() + 0.5, reservedSeatPos.getZ() + 0.5);
    }

    @Override
    protected boolean canLeaveQueueForSeat() {
        if (restaurant == null || reservedQueuePos == null) {
            return false;
        }
        return findAndReserveSeat();
    }

    @Override
    protected void releaseReservedSeat() {
        if (reservedSeatPos != null && this.level() instanceof ServerLevel serverLevel) {
            RestaurantSeatingApi.releaseSeat(reservedSeatPos, this.getUUID(), serverLevel);
            reservedSeatPos = null;
            seatTarget = null;
            assignedTablePos = null;
        }
    }

    @Override
    protected void prepareToLeave() {
        this.noPhysics = false;
        if (reservedSeatPos == null && seatTarget == null) {
            return;
        }

        Vec3 standingTarget = reservedSeatPos != null
                ? getSeatApproachTarget()
                : new Vec3(seatTarget.x, seatTarget.y + 0.6, seatTarget.z);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(standingTarget.x, standingTarget.y, standingTarget.z);
        this.xo = standingTarget.x;
        this.yo = standingTarget.y;
        this.zo = standingTarget.z;
    }

    @Override
    protected void holdSeatPosition() {
        if (seatTarget == null) {
            return;
        }
        this.noPhysics = true;
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(seatTarget.x, seatTarget.y, seatTarget.z, 0.0);
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(seatTarget.x, seatTarget.y, seatTarget.z);
        this.xo = seatTarget.x;
        this.yo = seatTarget.y;
        this.zo = seatTarget.z;
        faceAssignedTable();
    }

    @Override
    protected boolean shouldStaySeated() {
        return serviceType == CustomerServiceType.DINE_IN
                && (state == CustomerState.SEATED
                || state == CustomerState.REQUESTING
                || state == CustomerState.WAITING_FOR_PLATE
                || state == CustomerState.EATING);
    }

    @Override
    protected void faceAssignedTable() {
        if (assignedTablePos == null) {
            return;
        }

        Vec3 tableCenter = Vec3.atCenterOf(assignedTablePos);
        double dx = tableCenter.x - this.getX();
        double dz = tableCenter.z - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;
    }
}
