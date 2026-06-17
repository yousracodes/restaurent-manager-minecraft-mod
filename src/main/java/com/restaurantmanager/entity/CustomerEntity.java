package com.restaurantmanager.entity;

import com.restaurantmanager.client.ClientCustomerScreens;
import com.restaurantmanager.block.PlateBlockEntity;
import com.restaurantmanager.food.FoodRegistryScanner;
import com.restaurantmanager.food.FoodRequest;
import com.restaurantmanager.item.PlateItem;
import com.restaurantmanager.item.TakeoutPackageItem;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.OpenCustomerSuggestionPacket;
import com.restaurantmanager.network.packet.OpenTakeoutNegotiationPacket;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantMenu;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.restaurant.SeatTableLink;
import com.restaurantmanager.seating.MarkedPoint;
import com.restaurantmanager.seating.RestaurantSeatingApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class CustomerEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> ORDER_SUMMARY =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ORDER_ITEM_IDS =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SYNCED_SERVICE_TYPE =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ORDER_START_INDEX =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final Random REQUEST_RANDOM = new Random();
    private static final int TICKS_PER_MINUTE = 20 * 60;
    private static final int HAPPY_TO_NEUTRAL_TICKS = TICKS_PER_MINUTE * 15;
    private static final int NEUTRAL_TO_IMPATIENT_TICKS = TICKS_PER_MINUTE * 25;
    private static final int IMPATIENT_TO_ANGRY_TICKS = TICKS_PER_MINUTE * 35;
    private static final int ANGRY_TO_LEAVE_TICKS = TICKS_PER_MINUTE * 45;
    private static final int NO_SEAT_TO_IMPATIENT_TICKS = TICKS_PER_MINUTE * 5;
    private static final int NO_SEAT_TO_LEAVE_TICKS = TICKS_PER_MINUTE * 10;
    private static final int EATING_TICKS = 20 * 2;
    private static final int PLATE_SCAN_INTERVAL = 20;
    private static final int TAKEOUT_CHANCE_PERCENT = 30;
    private static final int TAKEOUT_NEGOTIATION_CHANCE_PERCENT = 20;
    private static final int STAR_PRICE_STEP_PERCENT = 25;
    private static final int CLOSED_RESTAURANT_DESPAWN_TICKS = TICKS_PER_MINUTE;
    private static final int CLOSED_RESTAURANT_SEARCH_INTERVAL = 20 * 5;
    private static final int RESTAURANT_SEARCH_INTERVAL = 20;
    private static final int RESERVATION_SEARCH_INTERVAL = 10;

    private final List<FoodRequest> requests = new ArrayList<>();
    private int currentRequestIndex;
    private UUID restaurantId;
    private Restaurant restaurant;
    private CustomerState state = CustomerState.SPAWN;
    private CustomerServiceType serviceType = CustomerServiceType.DINE_IN;
    private CustomerServiceType forcedServiceType;
    private CustomerPatience patience = CustomerPatience.HAPPY;
    private int waitTimer;
    private int noSeatWaitTimer;
    private int eatingTimer;
    private int plateScanTimer;
    private int wrongOrderCooldown;
    private int closedRestaurantLeaveTimer;
    private int restaurantSearchCooldown;
    private int seatSearchCooldown;
    private int counterSearchCooldown;
    private boolean leavingClosedRestaurant;
    private BlockPos reservedSeatPos;
    private BlockPos reservedQueuePos;
    private BlockPos reservedCounterPos;
    private BlockPos assignedTablePos;
    private BlockPos servedPlatePos;
    private Vec3 entranceTarget;
    private Vec3 exitTarget;
    private Vec3 seatTarget;
    private Vec3 queueTarget;
    private Vec3 counterTarget;

    public CustomerEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ORDER_SUMMARY, "");
        this.entityData.define(ORDER_ITEM_IDS, "");
        this.entityData.define(SYNCED_SERVICE_TYPE, CustomerServiceType.DINE_IN.name());
        this.entityData.define(ORDER_START_INDEX, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (restaurantId != null) {
            compound.putUUID("RestaurantId", restaurantId);
        }
        compound.putString("State", state.name());
        compound.putString("ServiceType", serviceType.name());
        if (forcedServiceType != null) {
            compound.putString("ForcedServiceType", forcedServiceType.name());
        }
        compound.putInt("WaitTimer", waitTimer);
        compound.putInt("EatingTimer", eatingTimer);
        compound.putInt("ClosedRestaurantLeaveTimer", closedRestaurantLeaveTimer);
        compound.putBoolean("LeavingClosedRestaurant", leavingClosedRestaurant);
        compound.putInt("CurrentRequestIndex", currentRequestIndex);
        compound.put("Requests", writeRequests());
        if (reservedSeatPos != null) {
            compound.put("ReservedSeat", NbtUtils.writeBlockPos(reservedSeatPos));
        }
        if (reservedQueuePos != null) {
            compound.put("ReservedQueue", NbtUtils.writeBlockPos(reservedQueuePos));
        }
        if (reservedCounterPos != null) {
            compound.put("ReservedCounter", NbtUtils.writeBlockPos(reservedCounterPos));
        }
        if (assignedTablePos != null) {
            compound.put("AssignedTable", NbtUtils.writeBlockPos(assignedTablePos));
        }
        if (servedPlatePos != null) {
            compound.put("ServedPlate", NbtUtils.writeBlockPos(servedPlatePos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        restaurantId = compound.hasUUID("RestaurantId") ? compound.getUUID("RestaurantId") : null;
        if (compound.contains("State")) {
            state = readState(compound.getString("State"));
        }
        if (compound.contains("ServiceType")) {
            serviceType = readServiceType(compound.getString("ServiceType"));
        }
        if (compound.contains("ForcedServiceType")) {
            forcedServiceType = readServiceType(compound.getString("ForcedServiceType"));
            serviceType = forcedServiceType;
        } else if (compound.contains("force_takeout") && compound.getBoolean("force_takeout")) {
            forcedServiceType = CustomerServiceType.TAKEOUT;
            serviceType = forcedServiceType;
        }
        this.entityData.set(SYNCED_SERVICE_TYPE, serviceType.name());
        waitTimer = compound.getInt("WaitTimer");
        eatingTimer = compound.getInt("EatingTimer");
        closedRestaurantLeaveTimer = compound.getInt("ClosedRestaurantLeaveTimer");
        leavingClosedRestaurant = compound.getBoolean("LeavingClosedRestaurant");
        currentRequestIndex = compound.getInt("CurrentRequestIndex");
        readRequests(compound);
        reservedSeatPos = compound.contains("ReservedSeat") ? NbtUtils.readBlockPos(compound.getCompound("ReservedSeat")) : null;
        reservedQueuePos = compound.contains("ReservedQueue") ? NbtUtils.readBlockPos(compound.getCompound("ReservedQueue")) : null;
        reservedCounterPos = compound.contains("ReservedCounter") ? NbtUtils.readBlockPos(compound.getCompound("ReservedCounter")) : null;
        assignedTablePos = compound.contains("AssignedTable") ? NbtUtils.readBlockPos(compound.getCompound("AssignedTable")) : null;
        servedPlatePos = compound.contains("ServedPlate") ? NbtUtils.readBlockPos(compound.getCompound("ServedPlate")) : null;
        seatTarget = reservedSeatPos == null ? null : Vec3.atCenterOf(reservedSeatPos);
        queueTarget = reservedQueuePos == null ? null : Vec3.atBottomCenterOf(reservedQueuePos);
        counterTarget = reservedCounterPos == null ? null : Vec3.atBottomCenterOf(reservedCounterPos.above());
        updateRequestName();
    }

    private ListTag writeRequests() {
        ListTag requestList = new ListTag();
        for (FoodRequest request : requests) {
            CompoundTag requestTag = new CompoundTag();
            if (request == null) {
                requestTag.putBoolean("Unavailable", true);
            } else {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(request.getItem());
                if (id != null) {
                    requestTag.putString("Item", id.toString());
                }
            }
            requestList.add(requestTag);
        }
        return requestList;
    }

    private void readRequests(CompoundTag compound) {
        requests.clear();
        if (!compound.contains("Requests", Tag.TAG_LIST)) {
            currentRequestIndex = 0;
            return;
        }

        ListTag requestList = compound.getList("Requests", Tag.TAG_COMPOUND);
        for (int i = 0; i < requestList.size(); i++) {
            CompoundTag requestTag = requestList.getCompound(i);
            if (requestTag.getBoolean("Unavailable")) {
                requests.add(null);
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(requestTag.getString("Item"));
            if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
                ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
                requests.add(stack.isEdible() ? FoodRegistryScanner.createRequest(stack.getItem()) : null);
            } else {
                requests.add(null);
            }
        }
        currentRequestIndex = Math.max(0, Math.min(currentRequestIndex, requests.size()));
    }

    private CustomerState readState(String savedState) {
        try {
            return CustomerState.valueOf(savedState);
        } catch (IllegalArgumentException exception) {
            return CustomerState.SPAWN;
        }
    }

    private CustomerServiceType readServiceType(String savedServiceType) {
        try {
            return CustomerServiceType.valueOf(savedServiceType);
        } catch (IllegalArgumentException exception) {
            return CustomerServiceType.DINE_IN;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EnterRestaurantGoal(this));
        this.goalSelector.addGoal(2, new CounterGoal(this));
        this.goalSelector.addGoal(3, new WaitForFoodGoal(this));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            handleState();
        }
    }

    private void handleState() {
        tickSearchCooldowns();

        if (restaurant == null) {
            findRestaurant();
        }
        if (restaurant != null && needsRequestRecovery()) {
            createRequests();
        }

        if (restaurant != null && state != CustomerState.LEAVING && (!restaurant.isActive() || !restaurant.isOpen())) {
            redirectOrLeaveClosedRestaurant();
            return;
        }

        if (state == CustomerState.SPAWN && restaurant != null) {
            chooseServiceType();
            createRequests();
            state = serviceType == CustomerServiceType.TAKEOUT ? CustomerState.MOVING_TO_COUNTER : CustomerState.ENTERING;
            resetPatience();
        }

        if (state == CustomerState.REQUESTING || state == CustomerState.WAITING_FOR_PLATE || state == CustomerState.WAITING_FOR_PACKAGE) {
            holdSeatPosition();
            holdCounterPosition();
            waitTimer++;
            if (wrongOrderCooldown > 0) {
                wrongOrderCooldown--;
            }
            updateFoodWaitPatience();
            if (state == CustomerState.WAITING_FOR_PLATE) {
                scanForPlateMeal();
            }
            if (waitTimer >= ANGRY_TO_LEAVE_TICKS) {
                leaveUnserved("Leaving: waited too long.");
            }
        }

        if (state == CustomerState.WAITING_IN_QUEUE) {
            waitTimer++;
            if (serviceType == CustomerServiceType.TAKEOUT && canLeaveQueueForCounter()) {
                releaseReservedQueueSpot();
                state = CustomerState.MOVING_TO_COUNTER;
            } else if (serviceType == CustomerServiceType.DINE_IN && canLeaveQueueForSeat()) {
                releaseReservedQueueSpot();
                state = CustomerState.ENTERING;
            } else if (moveForwardInQueue()) {
                state = CustomerState.JOINING_QUEUE;
            } else {
                holdQueuePosition();
                updateNoSeatPatience();
            }
        }

        if (state == CustomerState.WAITING_AT_COUNTER) {
            holdCounterPosition();
            state = CustomerState.WAITING_FOR_PACKAGE;
            waitTimer = 0;
            updateRequestName();
        }

        if (state == CustomerState.WAITING_FOR_PACKAGE) {
            holdCounterPosition();
        }

        if (state == CustomerState.EATING) {
            holdSeatPosition();
            eatingTimer++;
            if (eatingTimer >= EATING_TICKS) {
                finishEating();
            }
        }

        if (state == CustomerState.SERVED) {
            processService();
            state = CustomerState.PAYING;
        }

        if (state == CustomerState.PAYING) {
            leavingClosedRestaurant = false;
            closedRestaurantLeaveTimer = 0;
            state = CustomerState.LEAVING;
        }

        if (state == CustomerState.LEAVING && restaurant != null) {
            releaseAllReservations();
            if (leavingClosedRestaurant && handleClosedRestaurantLeaving()) {
                return;
            }
            Vec3 leavingTarget = getLeavingTarget();
            this.getNavigation().moveTo(leavingTarget.x, leavingTarget.y, leavingTarget.z, 1.0);
            if (this.distanceToSqr(leavingTarget.x, leavingTarget.y, leavingTarget.z) <= 1.5) {
                this.discard();
            }
        }
    }

    private boolean needsRequestRecovery() {
        return requests.isEmpty()
                && state != CustomerState.SPAWN
                && state != CustomerState.SERVED
                && state != CustomerState.PAYING
                && state != CustomerState.LEAVING;
    }

    private void tickSearchCooldowns() {
        if (restaurantSearchCooldown > 0) {
            restaurantSearchCooldown--;
        }
        if (seatSearchCooldown > 0) {
            seatSearchCooldown--;
        }
        if (counterSearchCooldown > 0) {
            counterSearchCooldown--;
        }
    }

    private Vec3 getLeavingTarget() {
        if (exitTarget != null) {
            return exitTarget;
        }
        if (restaurant != null) {
            return new Vec3(restaurant.getSignPos().getX() + 1.5, restaurant.getSignPos().getY(), restaurant.getSignPos().getZ() + 0.5);
        }
        return position();
    }

    private boolean handleClosedRestaurantLeaving() {
        closedRestaurantLeaveTimer++;
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (closedRestaurantLeaveTimer == 1 || closedRestaurantLeaveTimer % CLOSED_RESTAURANT_SEARCH_INTERVAL == 0) {
            Optional<Restaurant> nextRestaurant = findNearbyOpenRestaurant(serverLevel);
            if (nextRestaurant.isPresent()) {
                assignRestaurant(nextRestaurant.get());
                updateRestaurantTargets(serverLevel);
                releaseAllReservations();
                resetPatience();
                leavingClosedRestaurant = false;
                closedRestaurantLeaveTimer = 0;
                state = serviceType == CustomerServiceType.TAKEOUT
                        ? CustomerState.MOVING_TO_COUNTER
                        : CustomerState.ENTERING;
                updateRequestName();
                return true;
            }
        }

        if (closedRestaurantLeaveTimer >= CLOSED_RESTAURANT_DESPAWN_TICKS) {
            releaseAllReservations();
            this.discard();
            return true;
        }
        return false;
    }

    private void findRestaurant() {
        if (restaurantSearchCooldown > 0) {
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            RestaurantManager manager = RestaurantManager.get(serverLevel);
            this.restaurant = restaurantId == null
                    ? findNearbyOpenRestaurant(serverLevel).orElse(null)
                    : manager.findById(restaurantId)
                    .filter(Restaurant::isActive)
                    .filter(Restaurant::isOpen)
                    .orElseGet(() -> findNearbyOpenRestaurant(serverLevel).orElse(null));
            if (restaurant != null) {
                this.restaurantId = restaurant.getId();
                updateRestaurantTargets(serverLevel);
                if (entranceTarget != null) {
                    this.setPos(entranceTarget.x, entranceTarget.y, entranceTarget.z);
                } else {
                    this.setPos(restaurant.getSignPos().getX() + 1.5, restaurant.getSignPos().getY(), restaurant.getSignPos().getZ() + 0.5);
                }
                updateRequestName();
            } else {
                restaurantSearchCooldown = staggeredCooldown(RESTAURANT_SEARCH_INTERVAL);
            }
        }
    }

    private int staggeredCooldown(int interval) {
        return interval + Math.floorMod(this.getId(), interval);
    }

    private void updateRestaurantTargets(ServerLevel level) {
        if (restaurant == null) {
            entranceTarget = null;
            exitTarget = null;
            return;
        }
        BlockPos center = BlockPos.containing(restaurant.getBounds().getCenter());
        double radius = Math.max(16.0, Math.max(restaurant.getBounds().getXsize(), restaurant.getBounds().getZsize()) + 8.0);
        Optional<Vec3> doorBellTarget = restaurant.getDoorBellPos()
                .flatMap(pos -> RestaurantSeatingApi.getPoint(pos, level))
                .map(MarkedPoint::getInteractionPosition)
                .or(() -> restaurant.getDoorBellPos().map(Vec3::atBottomCenterOf));
        entranceTarget = doorBellTarget.or(() -> RestaurantSeatingApi.findNearestEntrance(center, radius, level)
                .map(MarkedPoint::getInteractionPosition)
        ).orElse(null);
        exitTarget = doorBellTarget.or(() -> RestaurantSeatingApi.findNearestExit(center, radius, level)
                .map(MarkedPoint::getInteractionPosition)
        ).orElse(entranceTarget);
    }

    public void assignRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
        this.restaurantId = restaurant == null ? null : restaurant.getId();
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void leaveBecauseRestaurantClosed() {
        leavingClosedRestaurant = true;
        closedRestaurantLeaveTimer = 0;
        if (state == CustomerState.LEAVING) {
            return;
        }
        releaseAllReservations();
        if (state == CustomerState.SERVED || state == CustomerState.PAYING) {
            state = CustomerState.LEAVING;
            this.setCustomName(Component.literal("Leaving: restaurant closed."));
            this.setCustomNameVisible(true);
            return;
        }
        leaveUnserved("Leaving: restaurant closed.");
    }

    public String getOrderSummary() {
        return this.entityData.get(ORDER_SUMMARY);
    }

    public List<ItemStack> getOrderItemStacks() {
        String ids = this.entityData.get(ORDER_ITEM_IDS);
        if (ids.isBlank()) {
            return List.of();
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (String rawId : ids.split("\\|")) {
            if ("-".equals(rawId)) {
                stacks.add(ItemStack.EMPTY);
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(rawId);
            if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
                stacks.add(new ItemStack(ForgeRegistries.ITEMS.getValue(id)));
            }
        }
        return stacks;
    }

    public boolean isTakeoutCustomer() {
        return CustomerServiceType.TAKEOUT.name().equals(this.entityData.get(SYNCED_SERVICE_TYPE));
    }

    public int getOrderStartIndex() {
        return this.entityData.get(ORDER_START_INDEX);
    }

    private void processService() {
        if (restaurant != null) {
            if (isOrderComplete()) {
                restaurant.addCustomerServed();
                restaurant.addReputation(getServiceReputationReward() + restaurant.getVarietyReputationBonus());
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                RestaurantManager.get(serverLevel).setDirty();
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseAllReservations();
        super.remove(reason);
    }

    private void chooseServiceType() {
        if (forcedServiceType != null) {
            serviceType = forcedServiceType;
            this.entityData.set(SYNCED_SERVICE_TYPE, serviceType.name());
            return;
        }
        serviceType = REQUEST_RANDOM.nextInt(100) < TAKEOUT_CHANCE_PERCENT
                ? CustomerServiceType.TAKEOUT
                : CustomerServiceType.DINE_IN;
        this.entityData.set(SYNCED_SERVICE_TYPE, serviceType.name());
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    public boolean serveFromInventory(Player player) {
        if (serviceType == CustomerServiceType.TAKEOUT) {
            player.displayClientMessage(Component.literal("This customer wants a takeout package."), false);
            return false;
        }
        FoodRequest request = getCurrentRequest();
        if (request == null) {
            player.displayClientMessage(Component.literal("I'm still deciding what to order."), false);
            return false;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() == request.getItem()) {
                stack.shrink(1);
                completeCurrentRequest(player, getScaledReward(request), "Thank you! Here's your change.");
                return true;
            }
        }

        player.displayClientMessage(Component.literal("You don't have: " + request.getDescription()), false);
        return false;
    }

    public boolean receiveTakeoutPackage(Player player, ItemStack packageStack) {
        if (serviceType != CustomerServiceType.TAKEOUT || state != CustomerState.WAITING_FOR_PACKAGE) {
            player.displayClientMessage(Component.literal("This customer is not waiting for takeout."), false);
            return false;
        }

        List<ItemStack> foods = TakeoutPackageItem.getFoods(packageStack);
        if (getCurrentRequest() == null || foods.isEmpty()) {
            player.displayClientMessage(Component.literal("The package is empty."), false);
            return false;
        }

        Optional<FoodRequest> missingRequest = findMissingTakeoutRequest(foods);
        if (missingRequest.isPresent()) {
            player.displayClientMessage(Component.literal("Package is missing: " + missingRequest.get().getDescription()), false);
            reactToWrongOrder("Wrong package");
            return false;
        }

        completeOrderFromContainer();
        player.displayClientMessage(Component.literal("Takeout accepted. Here's your change."), false);
        if (!player.getAbilities().instabuild) {
            packageStack.shrink(1);
        } else {
            TakeoutPackageItem.clearFood(packageStack);
        }
        releaseReservedCounter();
        return true;
    }

    public boolean receivePlate(Player player, ItemStack plateStack) {
        if (serviceType != CustomerServiceType.DINE_IN || state != CustomerState.WAITING_FOR_PLATE) {
            player.displayClientMessage(Component.literal("This customer is not waiting for dine-in food."), false);
            return false;
        }

        List<ItemStack> foods = PlateItem.getFoods(plateStack);
        if (getCurrentRequest() == null || foods.isEmpty()) {
            player.displayClientMessage(Component.literal("The plate is empty."), false);
            return false;
        }

        Optional<FoodRequest> missingRequest = findMissingTakeoutRequest(foods);
        if (missingRequest.isPresent()) {
            player.displayClientMessage(Component.literal("Plate is missing: " + missingRequest.get().getDescription()), false);
            reactToWrongOrder("Wrong plate");
            return false;
        }

        completeOrderFromContainer();
        PlateItem.clearFood(plateStack);
        player.displayClientMessage(Component.literal("Plate accepted. Here's your change."), false);
        releaseReservedSeat();
        return true;
    }

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

    public boolean serveSuggestionFromInventory(Player player, String suggestedFood) {
        FoodRequest request = getCurrentRequest();
        if (request == null) {
            player.displayClientMessage(Component.literal("I'm still deciding what to order."), false);
            return false;
        }

        ItemStack suggestedStack = FoodRegistryScanner.findFoodStack(suggestedFood);
        if (suggestedStack.isEmpty()) {
            player.displayClientMessage(Component.literal("I don't recognize that food."), false);
            return false;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() == suggestedStack.getItem()) {
                stack.shrink(1);
                int reward = getScaledReward(FoodRegistryScanner.calculateReward(suggestedStack));
                completeCurrentRequest(player, reward, "That works. Thanks for the suggestion!");
                return true;
            }
        }

        player.displayClientMessage(Component.literal("You don't have: " + suggestedStack.getHoverName().getString()), false);
        return false;
    }

    public void markCurrentRequestUnavailable(Player player) {
        FoodRequest request = getCurrentRequest();
        if (request == null) {
            return;
        }

        if (REQUEST_RANDOM.nextBoolean()) {
            player.displayClientMessage(Component.literal("Customer asks: What can you suggest instead?"), false);
            if (player instanceof ServerPlayer serverPlayer) {
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new OpenCustomerSuggestionPacket(this.getId(), this.getDisplayName().getString())
                );
            }
            return;
        }

        if (restaurant != null) {
            restaurant.addOrderFailed();
            if (this.level() instanceof ServerLevel serverLevel) {
                RestaurantManager.get(serverLevel).setDirty();
            }
        }
        advanceRequest();
        player.displayClientMessage(Component.literal("Customer skipped that request."), false);
    }

    public void startUnavailableNegotiation(Player player, int requestIndex) {
        if (requestIndex < currentRequestIndex || requestIndex >= requests.size()) {
            player.displayClientMessage(Component.literal("That order item is no longer active."), false);
            return;
        }

        FoodRequest request = requests.get(requestIndex);
        if (request == null) {
            player.displayClientMessage(Component.literal("That item is already unavailable."), false);
            updateRequestName();
            return;
        }

        if (REQUEST_RANDOM.nextInt(100) >= TAKEOUT_NEGOTIATION_CHANCE_PERCENT) {
            cancelTakeoutRequestReplacement(player, requestIndex);
            return;
        }

        player.displayClientMessage(Component.literal("Customer asks: What can you suggest instead?"), false);
        this.setCustomName(Component.literal("Negotiating: " + request.getDescription() + " unavailable"));
        this.setCustomNameVisible(true);
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new OpenTakeoutNegotiationPacket(
                            this.getId(),
                            this.getDisplayName().getString(),
                            requestIndex,
                            request.getDescription()
                    )
            );
        }
    }

    public void replaceTakeoutRequest(Player player, int requestIndex, String suggestedFood) {
        if (requestIndex < currentRequestIndex || requestIndex >= requests.size()) {
            player.displayClientMessage(Component.literal("That order item is no longer active."), false);
            return;
        }

        ItemStack suggestedStack = FoodRegistryScanner.findFoodStack(suggestedFood);
        if (suggestedStack.isEmpty()) {
            player.displayClientMessage(Component.literal("I don't recognize that replacement."), false);
            return;
        }

        requests.set(requestIndex, FoodRegistryScanner.createRequest(suggestedStack.getItem()));
        player.displayClientMessage(Component.literal("Customer accepted: " + suggestedStack.getHoverName().getString()), false);
        updateRequestName();
    }

    public void cancelTakeoutRequestReplacement(Player player, int requestIndex) {
        if (requestIndex < currentRequestIndex || requestIndex >= requests.size()) {
            player.displayClientMessage(Component.literal("That order item is no longer active."), false);
            return;
        }

        requests.set(requestIndex, null);
        if (restaurant != null) {
            restaurant.addOrderFailed();
            if (this.level() instanceof ServerLevel serverLevel) {
                RestaurantManager.get(serverLevel).setDirty();
            }
        }
        player.displayClientMessage(Component.literal("Customer skipped that unavailable item."), false);
        updateRequestName();
    }

    public void loseRequest(Player player) {
        player.displayClientMessage(Component.literal("Customer cancelled the order."), false);
        leaveUnserved("Leaving: order cancelled.");
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCustomerScreens.open(this.getId()));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }

    @Nullable
    private FoodRequest getCurrentRequest() {
        if (currentRequestIndex < 0 || currentRequestIndex >= requests.size()) {
            return null;
        }
        for (int i = currentRequestIndex; i < requests.size(); i++) {
            FoodRequest request = requests.get(i);
            if (request != null) {
                return request;
            }
        }
        return null;
    }

    private boolean isOrderComplete() {
        if (requests.isEmpty()) {
            return false;
        }
        for (int i = currentRequestIndex; i < requests.size(); i++) {
            if (requests.get(i) != null) {
                return false;
            }
        }
        return true;
    }

    private void createRequests() {
        requests.clear();
        int requestCount = 1 + REQUEST_RANDOM.nextInt(3);
        for (int i = 0; i < requestCount; i++) {
            requests.add(createRestaurantRequest());
        }
        currentRequestIndex = 0;
        updateRequestName();
    }

    private FoodRequest createRestaurantRequest() {
        if (restaurant != null && restaurant.hasMenuItems()) {
            List<net.minecraft.resources.ResourceLocation> menuItems = new ArrayList<>();
            for (RestaurantMenu menu : restaurant.getMenus()) {
                menuItems.addAll(menu.getItems());
            }
            net.minecraft.resources.ResourceLocation itemId = menuItems.get(REQUEST_RANDOM.nextInt(menuItems.size()));
            if (ForgeRegistries.ITEMS.containsKey(itemId)) {
                ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(itemId));
                if (stack.isEdible()) {
                    return FoodRegistryScanner.createRequest(stack.getItem());
                }
            }
        }
        return FoodRegistryScanner.createRequest();
    }

    private void completeCurrentRequest(Player player, int reward, String message) {
        this.spawnAtLocation(new ItemStack(Items.EMERALD, reward));
        if (restaurant != null) {
            restaurant.addOrderCompleted();
            restaurant.addMoneyEarned(reward);
            if (this.level() instanceof ServerLevel serverLevel) {
                RestaurantManager.get(serverLevel).setDirty();
            }
        }
        advanceRequest();
        player.displayClientMessage(Component.literal(message), false);
    }

    private void completeCurrentRequestFromPlate(int reward) {
        this.spawnAtLocation(new ItemStack(Items.EMERALD, reward));
        if (restaurant != null) {
            restaurant.addOrderCompleted();
            restaurant.addMoneyEarned(reward);
            if (this.level() instanceof ServerLevel serverLevel) {
                RestaurantManager.get(serverLevel).setDirty();
            }
        }
        advanceRequest();
    }

    private Optional<FoodRequest> findMissingTakeoutRequest(List<ItemStack> foods) {
        List<ItemStack> remainingFoods = foods.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (int i = currentRequestIndex; i < requests.size(); i++) {
            FoodRequest request = requests.get(i);
            if (request == null) {
                continue;
            }
            Optional<ItemStack> matchingFood = remainingFoods.stream()
                    .filter(food -> !food.isEmpty() && food.getItem() == request.getItem())
                    .findFirst();
            if (matchingFood.isEmpty()) {
                return Optional.of(request);
            }
            remainingFoods.remove(matchingFood.get());
        }
        return Optional.empty();
    }

    private void completeOrderFromContainer() {
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

        if (totalReward > 0) {
            this.spawnAtLocation(new ItemStack(Items.EMERALD, totalReward));
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
        state = CustomerState.SERVED;
        updateRequestName();
    }

    private void scanForPlateMeal() {
        if (++plateScanTimer < PLATE_SCAN_INTERVAL || assignedTablePos == null) {
            return;
        }
        plateScanTimer = 0;
        if (getCurrentRequest() == null) {
            return;
        }

        Optional<PlateBlockEntity> matchingPlate = findLinkedTablePlateWithCompleteOrder();
        if (matchingPlate.isPresent()) {
            servedPlatePos = matchingPlate.get().getBlockPos();
            state = CustomerState.EATING;
            eatingTimer = 0;
            this.setCustomName(Component.literal("Eating"));
            this.setCustomNameVisible(true);
            return;
        }

        if (wrongOrderCooldown <= 0) {
            findLinkedTablePlateWithMeals()
                    .ifPresent(plate -> {
                        Optional<FoodRequest> missingRequest = findMissingTakeoutRequest(plate.getMeals());
                        String missing = missingRequest.map(FoodRequest::getDescription).orElse("requested food");
                        reactToWrongOrder("Wrong plate: missing " + missing);
                        wrongOrderCooldown = 20 * 5;
                    });
        }
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

    private void reactToWrongOrder(String message) {
        waitTimer += TICKS_PER_MINUTE;
        setPatience(waitTimer >= IMPATIENT_TO_ANGRY_TICKS ? CustomerPatience.ANGRY : CustomerPatience.IMPATIENT);
        this.setCustomName(Component.literal(message));
        this.setCustomNameVisible(true);
    }

    private void finishEating() {
        if (servedPlatePos != null && this.level().getBlockEntity(servedPlatePos) instanceof PlateBlockEntity plate) {
            plate.clearMeals();
        }
        servedPlatePos = null;
        completeOrderFromContainer();
        releaseReservedSeat();
    }

    private void advanceRequest() {
        currentRequestIndex++;
        if (isOrderComplete()) {
            state = CustomerState.SERVED;
            this.setCustomName(Component.literal("Order complete"));
            this.setCustomNameVisible(true);
            return;
        }
        state = serviceType == CustomerServiceType.TAKEOUT ? CustomerState.WAITING_FOR_PACKAGE : CustomerState.WAITING_FOR_PLATE;
        waitTimer = 0;
        noSeatWaitTimer = 0;
        patience = CustomerPatience.HAPPY;
        updateRequestName();
    }

    private boolean findAndReserveSeat() {
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
            seatTarget = RestaurantSeatingApi.getPoint(reservedSeatPos, serverLevel)
                    .map(MarkedPoint::getInteractionPosition)
                    .orElse(Vec3.atCenterOf(reservedSeatPos));
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
        seatTarget = point.getInteractionPosition();
        assignedTablePos = RestaurantSeatingApi.getLinkedTable(reservedSeatPos, serverLevel)
                .or(() -> RestaurantSeatingApi.findNearestTable(reservedSeatPos, 4.0, serverLevel)
                .map(MarkedPoint::getPos)
                )
                .orElse(null);
        noSeatWaitTimer = 0;
        seatSearchCooldown = 0;
        return true;
    }

    private boolean findAndReserveQueueSpot() {
        if (restaurant == null || reservedQueuePos != null) {
            return reservedQueuePos != null;
        }

        reservedQueuePos = restaurant.getSignPos();
        queueTarget = Vec3.atBottomCenterOf(reservedQueuePos);
        return true;
    }

    private boolean findAndReserveCounter() {
        if (!(this.level() instanceof ServerLevel serverLevel) || restaurant == null || reservedCounterPos != null) {
            return reservedCounterPos != null;
        }
        if (counterSearchCooldown > 0) {
            return false;
        }

        Optional<BlockPos> restaurantCounter = restaurant.getCounterPos();
        if (restaurantCounter.isPresent()
                && RestaurantSeatingApi.occupyCounter(restaurantCounter.get(), this.getUUID(), serverLevel)) {
            reservedCounterPos = restaurantCounter.get();
            counterTarget = RestaurantSeatingApi.getPoint(reservedCounterPos, serverLevel)
                    .map(MarkedPoint::getInteractionPosition)
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
        counterTarget = point.getInteractionPosition();
        counterSearchCooldown = 0;
        return true;
    }

    private boolean canLeaveQueueForSeat() {
        if (restaurant == null || reservedQueuePos == null) {
            return false;
        }
        return findAndReserveSeat();
    }

    private boolean canLeaveQueueForCounter() {
        if (restaurant == null || reservedQueuePos == null) {
            return false;
        }
        return findAndReserveCounter();
    }

    private boolean moveForwardInQueue() {
        return false;
    }

    private void resetPatience() {
        this.waitTimer = 0;
        this.noSeatWaitTimer = 0;
        this.patience = CustomerPatience.HAPPY;
    }

    private void updateFoodWaitPatience() {
        CustomerPatience nextPatience;
        if (waitTimer >= IMPATIENT_TO_ANGRY_TICKS) {
            nextPatience = CustomerPatience.ANGRY;
        } else if (waitTimer >= NEUTRAL_TO_IMPATIENT_TICKS) {
            nextPatience = CustomerPatience.IMPATIENT;
        } else if (waitTimer >= HAPPY_TO_NEUTRAL_TICKS) {
            nextPatience = CustomerPatience.NEUTRAL;
        } else {
            nextPatience = CustomerPatience.HAPPY;
        }
        setPatience(nextPatience);
    }

    private void updateNoSeatPatience() {
        noSeatWaitTimer++;
        if (noSeatWaitTimer >= NO_SEAT_TO_LEAVE_TICKS) {
            leaveUnserved("Leaving: no available seats.");
        } else if (noSeatWaitTimer >= NO_SEAT_TO_IMPATIENT_TICKS) {
            setPatience(CustomerPatience.IMPATIENT);
        } else {
            setPatience(CustomerPatience.HAPPY);
        }
    }

    private int getServiceReputationReward() {
        return switch (patience) {
            case HAPPY -> 5;
            case NEUTRAL -> 3;
            case IMPATIENT -> 1;
            case ANGRY, LEAVING -> 0;
        };
    }

    private int getScaledReward(FoodRequest request) {
        return request == null ? 1 : getScaledReward(request.getReward());
    }

    private int getScaledReward(int baseReward) {
        int stars = restaurant == null ? 1 : restaurant.getStarRating();
        int percent = 100 + Math.max(0, stars - 1) * STAR_PRICE_STEP_PERCENT;
        return Math.max(1, (int) Math.ceil(baseReward * percent / 100.0));
    }

    private void setPatience(CustomerPatience patience) {
        if (this.patience != patience) {
            this.patience = patience;
            updateRequestName();
        }
    }

    private void leaveUnserved(String message) {
        if (state == CustomerState.LEAVING) {
            return;
        }
        this.patience = CustomerPatience.LEAVING;
        if (restaurant != null) {
            restaurant.addCustomerLost();
            restaurant.addOrderFailed();
            restaurant.addReputation(-1);
            if (this.level() instanceof ServerLevel serverLevel) {
                RestaurantManager.get(serverLevel).setDirty();
            }
        }
        state = CustomerState.LEAVING;
        this.setCustomName(Component.literal(message));
        this.setCustomNameVisible(true);
    }

    private void releaseReservedSeat() {
        if (reservedSeatPos != null && this.level() instanceof ServerLevel serverLevel) {
            RestaurantSeatingApi.releaseSeat(reservedSeatPos, this.getUUID(), serverLevel);
            reservedSeatPos = null;
            seatTarget = null;
            assignedTablePos = null;
        }
    }

    private void releaseReservedQueueSpot() {
        reservedQueuePos = null;
        queueTarget = null;
    }

    private void releaseReservedCounter() {
        if (reservedCounterPos != null && this.level() instanceof ServerLevel serverLevel) {
            RestaurantSeatingApi.releaseCounter(reservedCounterPos, this.getUUID(), serverLevel);
            reservedCounterPos = null;
            counterTarget = null;
        }
    }

    private void releaseAllReservations() {
        releaseReservedSeat();
        releaseReservedQueueSpot();
        releaseReservedCounter();
    }

    private void redirectOrLeaveClosedRestaurant() {
        leaveBecauseRestaurantClosed();
    }

    private Optional<Restaurant> findNearbyOpenRestaurant(ServerLevel level) {
        RestaurantManager manager = RestaurantManager.get(level);
        return manager.getRestaurants().stream()
                .filter(Restaurant::isActive)
                .filter(Restaurant::isOpen)
                .filter(Restaurant::hasMenuItems)
                .filter(restaurant -> restaurantId == null || !restaurant.getId().equals(restaurantId))
                .filter(restaurant -> restaurant.getSignPos().distSqr(this.blockPosition()) <= 96.0 * 96.0)
                .min((first, second) -> Double.compare(
                        first.getSignPos().distSqr(this.blockPosition()),
                        second.getSignPos().distSqr(this.blockPosition())
                ));
    }

    private void holdSeatPosition() {
        if (seatTarget == null) {
            return;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.distanceToSqr(seatTarget.x, seatTarget.y, seatTarget.z) > 0.5) {
            this.setPos(seatTarget.x, seatTarget.y, seatTarget.z);
        }
    }

    private void holdQueuePosition() {
        if (queueTarget == null) {
            return;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.distanceToSqr(queueTarget.x, queueTarget.y, queueTarget.z) > 0.5) {
            this.setPos(queueTarget.x, queueTarget.y, queueTarget.z);
        }
    }

    private void holdCounterPosition() {
        if (counterTarget == null) {
            return;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.distanceToSqr(counterTarget.x, counterTarget.y, counterTarget.z) > 0.5) {
            this.setPos(counterTarget.x, counterTarget.y, counterTarget.z);
        }
    }

    private void updateRequestName() {
        FoodRequest request = getCurrentRequest();
        if (request == null) {
            this.entityData.set(ORDER_SUMMARY, "");
            this.entityData.set(ORDER_ITEM_IDS, "");
            this.entityData.set(ORDER_START_INDEX, currentRequestIndex);
            return;
        }
        String serviceLabel = serviceType == CustomerServiceType.TAKEOUT ? "Takeout" : "Dine-in";
        int stars = restaurant == null ? 1 : restaurant.getStarRating();
        int reward = getScaledReward(request);
        this.setCustomName(Component.literal(serviceLabel + " - " + patience.getDisplayName()
                + " - " + stars + " stars"
                + " - Request " + (currentRequestIndex + 1) + "/" + requests.size()
                + ": " + request.getDescription()
                + " (" + reward + " emeralds)"));
        this.setCustomNameVisible(true);
        this.entityData.set(ORDER_SUMMARY, buildOrderSummary());
        this.entityData.set(ORDER_ITEM_IDS, buildOrderItemIds());
        this.entityData.set(ORDER_START_INDEX, currentRequestIndex);
    }

    private String buildOrderSummary() {
        if (requests.isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        for (int i = currentRequestIndex; i < requests.size(); i++) {
            FoodRequest order = requests.get(i);
            if (order == null) {
                lines.add((i + 1) + ". Unavailable");
                continue;
            }
            lines.add((i + 1) + ". " + order.getDescription() + " (" + getScaledReward(order) + " emeralds)");
        }
        return String.join("\n", lines);
    }

    private String buildOrderItemIds() {
        if (requests.isEmpty()) {
            return "";
        }

        List<String> ids = new ArrayList<>();
        for (int i = currentRequestIndex; i < requests.size(); i++) {
            FoodRequest request = requests.get(i);
            if (request == null) {
                ids.add("-");
                continue;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(request.getItem());
            if (id != null) {
                ids.add(id.toString());
            }
        }
        return String.join("|", ids);
    }

    private static class EnterRestaurantGoal extends Goal {
        private final CustomerEntity customer;

        public EnterRestaurantGoal(CustomerEntity customer) {
            this.customer = customer;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return (customer.state == CustomerState.ENTERING
                    || customer.state == CustomerState.MOVING_TO_SEAT
                    || customer.state == CustomerState.JOINING_QUEUE) && customer.restaurant != null;
        }

        @Override
        public void start() {
            if (customer.state == CustomerState.ENTERING && customer.findAndReserveSeat()) {
                customer.state = CustomerState.MOVING_TO_SEAT;
            } else if (customer.state == CustomerState.ENTERING && customer.findAndReserveQueueSpot()) {
                customer.state = CustomerState.JOINING_QUEUE;
            }
        }

        @Override
        public void tick() {
            if (customer.restaurant == null) {
                return;
            }

            if (customer.state == CustomerState.JOINING_QUEUE) {
                if (customer.queueTarget == null && !customer.findAndReserveQueueSpot()) {
                    customer.updateNoSeatPatience();
                    return;
                }
                customer.getNavigation().moveTo(customer.queueTarget.x, customer.queueTarget.y, customer.queueTarget.z, 1.0);
                if (customer.distanceToSqr(customer.queueTarget.x, customer.queueTarget.y, customer.queueTarget.z) <= 1.0) {
                    customer.setPos(customer.queueTarget.x, customer.queueTarget.y, customer.queueTarget.z);
                    customer.getNavigation().stop();
                    customer.state = CustomerState.WAITING_IN_QUEUE;
                    customer.waitTimer = 0;
                }
                return;
            }

            if (customer.seatTarget == null && !customer.findAndReserveSeat()) {
                if (customer.findAndReserveQueueSpot()) {
                    customer.state = CustomerState.JOINING_QUEUE;
                    return;
                }
                customer.updateNoSeatPatience();
                return;
            }

            AABB bounds = customer.restaurant.getBounds();
            double targetX = customer.seatTarget != null ? customer.seatTarget.x : bounds.getCenter().x;
            double targetY = customer.seatTarget != null ? customer.seatTarget.y : bounds.minY + 1.0;
            double targetZ = customer.seatTarget != null ? customer.seatTarget.z : bounds.getCenter().z;
            customer.getNavigation().moveTo(targetX, targetY, targetZ, 1.0);
            boolean reachedSeat = customer.seatTarget != null && customer.distanceToSqr(targetX, targetY, targetZ) <= 1.0;
            if (reachedSeat) {
                customer.setPos(targetX, targetY, targetZ);
                customer.getNavigation().stop();
                customer.state = CustomerState.SEATED;
                customer.waitTimer = 0;
                customer.state = CustomerState.WAITING_FOR_PLATE;
                customer.updateRequestName();
            }
        }
    }

    private static class WaitForFoodGoal extends Goal {
        private final CustomerEntity customer;

        public WaitForFoodGoal(CustomerEntity customer) {
            this.customer = customer;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return customer.state == CustomerState.WAITING_FOR_PLATE || customer.state == CustomerState.WAITING_FOR_PACKAGE;
        }

        @Override
        public void start() {
            customer.waitTimer = 0;
            customer.updateRequestName();
        }

        @Override
        public void tick() {
            if (customer.restaurant == null) {
                return;
            }
            customer.holdSeatPosition();
            if (customer.getCurrentRequest() != null) {
                BlockPos signPos = customer.restaurant.getSignPos();
                customer.getLookControl().setLookAt(signPos.getX() + 0.5, signPos.getY() + 0.5, signPos.getZ() + 0.5);
            }
        }
    }

    private static class CounterGoal extends Goal {
        private final CustomerEntity customer;

        public CounterGoal(CustomerEntity customer) {
            this.customer = customer;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return customer.state == CustomerState.MOVING_TO_COUNTER && customer.restaurant != null;
        }

        @Override
        public void start() {
            customer.findAndReserveCounter();
        }

        @Override
        public void tick() {
            if (customer.counterTarget == null && !customer.findAndReserveCounter()) {
                if (customer.findAndReserveQueueSpot()) {
                    customer.state = CustomerState.JOINING_QUEUE;
                    return;
                }
                customer.updateNoSeatPatience();
                return;
            }

            customer.getNavigation().moveTo(customer.counterTarget.x, customer.counterTarget.y, customer.counterTarget.z, 1.0);
            if (customer.distanceToSqr(customer.counterTarget.x, customer.counterTarget.y, customer.counterTarget.z) <= 1.0) {
                customer.setPos(customer.counterTarget.x, customer.counterTarget.y, customer.counterTarget.z);
                customer.getNavigation().stop();
                customer.state = CustomerState.WAITING_AT_COUNTER;
                customer.waitTimer = 0;
                customer.updateRequestName();
            }
        }
    }
}
