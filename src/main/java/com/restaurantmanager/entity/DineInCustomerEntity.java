package com.restaurantmanager.entity;

import com.restaurantmanager.block.PlateBlockEntity;
import com.restaurantmanager.food.FoodRequest;
import com.restaurantmanager.item.PlateItem;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.restaurant.SeatTableLink;
import com.restaurantmanager.seating.MarkedPoint;
import com.restaurantmanager.seating.RestaurantSeatingApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.restaurantmanager.seating.SeatingMarkSavedData;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
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

import java.util.List;
import java.util.Optional;

public class DineInCustomerEntity extends CustomerEntity {
    private Vec3 seatApproachTarget;
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

        Optional<ItemStack> unexpectedFood = findUnexpectedFood(foods);
        if (unexpectedFood.isPresent()) {
            String message = "Plate has an item I didn't order: " + unexpectedFood.get().getHoverName().getString();
            player.displayClientMessage(Component.literal(message), false);
            reactToWrongOrder(message);
            wrongOrderCooldown = 20 * 4;
            return false;
        }

        Optional<FoodRequest> missingRequest = findMissingTakeoutRequest(foods);
        if (missingRequest.isPresent()) {
            String message = "Plate is missing: " + missingRequest.get().getDescription();
            player.displayClientMessage(Component.literal(message), false);
            reactToWrongOrder(message);
            wrongOrderCooldown = 20 * 4;
            return false;
        }

        completeHandedPlateOrder(player, plateStack);
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

    private void completeHandedPlateOrder(Player player, ItemStack plateStack) {
        int totalReward = 0;
        int completedRequests = 0;
        for (int i = currentRequestIndex; i < requests.size(); i++) {
            FoodRequest request = requests.get(i);
            if (request == null) {
                continue;
            }
            totalReward += getScaledReward(request);
            completedRequests++;
        }

        if (player instanceof ServerPlayer serverPlayer && totalReward > 0) {
            serverPlayer.getInventory().placeItemBackInInventory(new ItemStack(net.minecraft.world.item.Items.EMERALD, totalReward));
        }
        if (restaurant != null) {
            for (int i = 0; i < completedRequests; i++) {
                restaurant.addOrderCompleted();
            }
            restaurant.addMoneyEarned(totalReward);
            if (this.level() instanceof ServerLevel serverLevel) {
                RestaurantManager.get(serverLevel).setDirty();
            }
        }

        currentRequestIndex = requests.size();
        PlateItem.clearFood(plateStack);
        prepareToLeave();
        state = CustomerState.SERVED;
        this.setCustomName(Component.literal("Order complete"));
        this.setCustomNameVisible(true);
        releaseReservedSeat();
        wrongOrderCooldown = 0;
        waitTimer = 0;
        player.displayClientMessage(Component.literal("Plate accepted. Here's your change."), false);
    }

    @Override
    protected void scanForPlateMeal() {
        if (++plateScanTimer < PLATE_SCAN_INTERVAL || assignedTablePos == null) {
            return;
        }
        plateScanTimer = 0;
        findLinkedTablePlateWithMeals()
                .ifPresent(plate -> {
                    List<ItemStack> meals = plate.getMeals();
                    Optional<ItemStack> unexpectedFood = findUnexpectedFood(meals);
                    if (unexpectedFood.isPresent()) {
                        if (wrongOrderCooldown <= 0) {
                            reactToWrongOrder("Plate has an item I didn't order: "
                                    + unexpectedFood.get().getHoverName().getString());
                            wrongOrderCooldown = 20 * 4;
                        }
                        return;
                    }

                    Optional<FoodRequest> missingRequest = findMissingTakeoutRequest(meals);
                    if (missingRequest.isPresent()) {
                        return;
                    }
                    servedPlatePos = plate.getBlockPos();
                    state = CustomerState.EATING;
                    eatingTimer = 0;
                    this.setCustomName(Component.literal("Eating"));
                    this.setCustomNameVisible(true);
                });
    }

    private Optional<PlateBlockEntity> findLinkedTablePlateWithMeals() {
        Optional<PlateBlockEntity> plate = getPlateWithMealsAt(assignedTablePos);
        if (plate.isPresent()) {
            return plate;
        }
        plate = getPlateWithMealsAt(assignedTablePos.above());
        if (plate.isPresent()) {
            return plate;
        }
        return getPlateWithMealsAt(assignedTablePos.above(2));
    }

    private Optional<PlateBlockEntity> getPlateWithMealsAt(BlockPos pos) {
        if (this.level().getBlockEntity(pos) instanceof PlateBlockEntity plate && plate.hasMeals()) {
            return Optional.of(plate);
        }
        return Optional.empty();
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
        // Accept any accessible side of the chair, including a side reached
        // before the originally selected waypoint. Do not require a nudge
        // toward a fixed block center to trigger sitting.
        if (!shouldStaySeated() && SeatAccess.canSitFrom(this.level(), this, reservedSeatPos)) {
            return this.position();
        }
        if (seatApproachTarget != null && canStandAt(seatApproachTarget)
                && !this.getNavigation().isStuck()) {
            return seatApproachTarget;
        }
        seatApproachTarget = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = reservedSeatPos.relative(direction);
            if (this.level() instanceof ServerLevel serverLevel) {
                var marks = SeatingMarkSavedData.get(serverLevel);
                if (marks.isSeat(pos) || marks.isTable(pos)) {
                    continue;
                }
            }
            double floor = WalkNodeEvaluator.getFloorLevel(this.level(), pos);
            Vec3 candidate = new Vec3(pos.getX() + 0.5, floor, pos.getZ() + 0.5);
            if (!canStandAt(candidate) || !SeatAccess.canSitFrom(this.level(), this, reservedSeatPos, candidate)) {
                continue;
            }
            if (shouldStaySeated()) {
                seatApproachTarget = candidate;
                return candidate;
            }
            var path = this.getNavigation().createPath(BlockPos.containing(candidate), 0);
            if (path != null && path.canReach()) {
                double distance = 0.0;
                Vec3 previous = this.position();
                for (int i = 0; i < path.getNodeCount(); i++) {
                    Vec3 next = Vec3.atBottomCenterOf(path.getNode(i).asBlockPos());
                    distance += previous.distanceTo(next);
                    previous = next;
                }
                if (distance < bestDistance) {
                    bestDistance = distance;
                    seatApproachTarget = candidate;
                }
            }
        }
        return seatApproachTarget;
    }

    @Override
    protected boolean canSitFromHere(Vec3 approachTarget) {
        return reservedSeatPos != null && seatTarget != null
                && SeatAccess.canSitFrom(this.level(), this, reservedSeatPos);
    }

    private boolean canStandAt(Vec3 target) {
        AABB bounds = this.getBoundingBox().move(target.subtract(this.position()));
        return this.level().noCollision(this, bounds.deflate(0.001))
                && this.level().getBlockCollisions(this, bounds.move(0.0, -0.05, 0.0)).iterator().hasNext();
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
            seatApproachTarget = null;
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
        if (standingTarget == null) {
            return;
        }
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
