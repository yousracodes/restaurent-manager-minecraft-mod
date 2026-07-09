package com.restaurantmanager.entity;

import com.restaurantmanager.client.ClientCustomerScreens;
import com.restaurantmanager.block.CashRegisterBlock;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public abstract class CustomerEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> ORDER_SUMMARY =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ORDER_ITEM_IDS =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SYNCED_SERVICE_TYPE =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SYNCED_STATE =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SYNCED_PATIENCE =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ORDER_START_INDEX =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final Random REQUEST_RANDOM = new Random();
    private static final int TICKS_PER_MINUTE = 20 * 60;
    private static final int HAPPY_TO_NEUTRAL_TICKS = TICKS_PER_MINUTE * 8;
    private static final int NEUTRAL_TO_IMPATIENT_TICKS = TICKS_PER_MINUTE * 18;
    private static final int IMPATIENT_TO_ANGRY_TICKS = TICKS_PER_MINUTE * 28;
    protected static final int ANGRY_TO_LEAVE_TICKS = TICKS_PER_MINUTE * 38;
    protected static final int NO_SEAT_TO_IMPATIENT_TICKS = 0;
    protected static final int NO_SEAT_TO_LEAVE_TICKS = TICKS_PER_MINUTE * 3;
    protected static final int EATING_TICKS = 20 * 10;
    protected static final int PLATE_SCAN_INTERVAL = 20;
    protected static final int TAKEOUT_NEGOTIATION_CHANCE_PERCENT = 20;
    protected static final int STAR_PRICE_STEP_PERCENT = 25;
    private static final int CLOSED_RESTAURANT_DESPAWN_TICKS = TICKS_PER_MINUTE;
    private static final int CLOSED_RESTAURANT_SEARCH_INTERVAL = 20 * 5;
    private static final int RESTAURANT_SEARCH_INTERVAL = 20;
    protected static final int RESERVATION_SEARCH_INTERVAL = 10;
    private static final double LEAVING_REACHED_DISTANCE_SQR = 4.0;
    private static final double COUNTER_REACHED_DISTANCE_SQR = 1.5;
    private static final double COUNTER_BLOCK_REACHED_DISTANCE_SQR = 4.0;
    private static final int TAKEOUT_COUNTER_SLOT_COUNT = 2;
    private static final Map<CounterSlotKey, UUID[]> TAKEOUT_COUNTER_SLOTS = new HashMap<>();

    protected final List<FoodRequest> requests = new ArrayList<>();
    protected int currentRequestIndex;
    private UUID restaurantId;
    protected Restaurant restaurant;
    protected CustomerState state = CustomerState.SPAWN;
    protected CustomerServiceType serviceType = CustomerServiceType.DINE_IN;
    private CustomerServiceType forcedServiceType;
    private CustomerPatience patience = CustomerPatience.HAPPY;
    protected int waitTimer;
    protected int noSeatWaitTimer;
    protected int eatingTimer;
    protected int plateScanTimer;
    protected int wrongOrderCooldown;
    private int closedRestaurantLeaveTimer;
    private int restaurantSearchCooldown;
    protected int seatSearchCooldown;
    protected int counterSearchCooldown;
    private boolean leavingClosedRestaurant;
    private boolean reachedLeavingExit;
    protected BlockPos reservedSeatPos;
    protected BlockPos reservedQueuePos;
    protected BlockPos reservedCounterPos;
    private int reservedCounterSlot = -1;
    protected BlockPos assignedTablePos;
    protected BlockPos servedPlatePos;
    private Vec3 entranceTarget;
    private Vec3 exitTarget;
    private Vec3 finalLeaveTarget;
    protected Vec3 seatTarget;
    protected Vec3 queueTarget;
    protected Vec3 counterTarget;

    public CustomerEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        this.serviceType = getConfiguredServiceType();
    }

    protected abstract CustomerServiceType getConfiguredServiceType();

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ORDER_SUMMARY, "");
        this.entityData.define(ORDER_ITEM_IDS, "");
        this.entityData.define(SYNCED_SERVICE_TYPE, getConfiguredServiceType().name());
        this.entityData.define(SYNCED_STATE, CustomerState.SPAWN.name());
        this.entityData.define(SYNCED_PATIENCE, CustomerPatience.HAPPY.name());
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
        compound.putBoolean("ReachedLeavingExit", reachedLeavingExit);
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
        reachedLeavingExit = compound.getBoolean("ReachedLeavingExit");
        currentRequestIndex = compound.getInt("CurrentRequestIndex");
        readRequests(compound);
        reservedSeatPos = compound.contains("ReservedSeat") ? NbtUtils.readBlockPos(compound.getCompound("ReservedSeat")) : null;
        reservedQueuePos = compound.contains("ReservedQueue") ? NbtUtils.readBlockPos(compound.getCompound("ReservedQueue")) : null;
        reservedCounterPos = compound.contains("ReservedCounter") ? NbtUtils.readBlockPos(compound.getCompound("ReservedCounter")) : null;
        assignedTablePos = compound.contains("AssignedTable") ? NbtUtils.readBlockPos(compound.getCompound("AssignedTable")) : null;
        servedPlatePos = compound.contains("ServedPlate") ? NbtUtils.readBlockPos(compound.getCompound("ServedPlate")) : null;
        seatTarget = reservedSeatPos == null ? null : getSeatTarget(reservedSeatPos);
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

    private CustomerPatience readPatience(String savedPatience) {
        if (savedPatience == null || savedPatience.isBlank()) {
            return CustomerPatience.HAPPY;
        }
        try {
            return CustomerPatience.valueOf(savedPatience);
        } catch (IllegalArgumentException exception) {
            return CustomerPatience.HAPPY;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new EnterRestaurantGoal(this));
        this.goalSelector.addGoal(3, new CounterGoal(this));
        this.goalSelector.addGoal(4, new WaitForFoodGoal(this));
        this.goalSelector.addGoal(5, new CustomerRandomStrollGoal(this));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            handleState();
            if (!shouldStaySeated()) {
                this.noPhysics = false;
            }
            syncState();
            syncPatience();
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
            setConfiguredServiceType();
            createRequests();
            state = serviceType == CustomerServiceType.TAKEOUT ? CustomerState.MOVING_TO_COUNTER : CustomerState.ENTERING;
            resetPatience();
        }

        if (state == CustomerState.REQUESTING || state == CustomerState.WAITING_FOR_PLATE) {
            holdSeatPosition();
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

        if (state == CustomerState.WAITING_FOR_PACKAGE) {
            holdCounterPosition();
            waitTimer++;
            if (wrongOrderCooldown > 0) {
                wrongOrderCooldown--;
            }
            updateFoodWaitPatience();
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
            reachedLeavingExit = false;
            prepareToLeave();
            state = CustomerState.LEAVING;
            this.setCustomName(Component.literal("Leaving"));
            this.setCustomNameVisible(true);
        }

        if (state == CustomerState.LEAVING && restaurant != null) {
            releaseAllReservations();
            if (leavingClosedRestaurant && handleClosedRestaurantLeaving()) {
                return;
            }
            if ((exitTarget == null || finalLeaveTarget == null) && this.level() instanceof ServerLevel serverLevel) {
                updateRestaurantTargets(serverLevel);
            }
            moveAlongLeavingRoute();
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

    private void moveAlongLeavingRoute() {
        Optional<Vec3> leavingTarget = getLeavingTarget();
        if (leavingTarget.isEmpty()) {
            this.setCustomName(Component.literal(reachedLeavingExit ? "Leaving: no sign" : "Leaving: no doorbell"));
            this.setCustomNameVisible(true);
            return;
        }

        Vec3 target = leavingTarget.get();
        this.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        if (this.distanceToSqr(target.x, target.y, target.z) > LEAVING_REACHED_DISTANCE_SQR) {
            return;
        }

        if (!reachedLeavingExit && exitTarget != null && finalLeaveTarget != null) {
            reachedLeavingExit = true;
            this.getNavigation().stop();
            return;
        }
        this.discard();
    }

    private Optional<Vec3> getLeavingTarget() {
        if (!reachedLeavingExit && exitTarget != null) {
            return Optional.of(exitTarget);
        }
        if (finalLeaveTarget != null) {
            return Optional.of(finalLeaveTarget);
        }
        return Optional.empty();
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
                reachedLeavingExit = false;
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

    protected int staggeredCooldown(int interval) {
        return interval + Math.floorMod(this.getId(), interval);
    }

    private void updateRestaurantTargets(ServerLevel level) {
        if (restaurant == null) {
            entranceTarget = null;
            exitTarget = null;
            finalLeaveTarget = null;
            return;
        }
        BlockPos center = BlockPos.containing(restaurant.getBounds().getCenter());
        double radius = Math.max(16.0, Math.max(restaurant.getBounds().getXsize(), restaurant.getBounds().getZsize()) + 8.0);
        if (restaurant.getDoorBellPos().isEmpty()) {
            RestaurantManager.get(level).refreshLayoutBlocks(restaurant, level);
        }
        Optional<Vec3> doorBellTarget = restaurant.getDoorBellPos()
                .flatMap(pos -> RestaurantSeatingApi.getPoint(pos, level))
                .map(MarkedPoint::getInteractionPosition)
                .or(() -> restaurant.getDoorBellPos().map(Vec3::atBottomCenterOf));
        entranceTarget = doorBellTarget.or(() -> RestaurantSeatingApi.findNearestEntrance(center, radius, level)
                .map(MarkedPoint::getInteractionPosition)
        ).orElse(null);
        exitTarget = doorBellTarget.orElse(null);
        finalLeaveTarget = getSignLeavingTarget(center, level);
    }

    private Vec3 getSignLeavingTarget(BlockPos restaurantCenter, ServerLevel level) {
        BlockPos signPos = restaurant.getSignPos();
        int dx = Integer.compare(signPos.getX(), restaurantCenter.getX());
        int dz = Integer.compare(signPos.getZ(), restaurantCenter.getZ());
        BlockPos target = Math.abs(signPos.getX() - restaurantCenter.getX()) >= Math.abs(signPos.getZ() - restaurantCenter.getZ())
                ? signPos.offset(dx == 0 ? 1 : dx, 0, 0)
                : signPos.offset(0, 0, dz == 0 ? 1 : dz);
        return Vec3.atBottomCenterOf(findGroundedLeavingPos(target, level));
    }

    private BlockPos findGroundedLeavingPos(BlockPos pos, ServerLevel level) {
        BlockPos target = pos;
        for (int i = 0; i < 3 && target.getY() > level.getMinBuildHeight(); i++) {
            if (!level.getBlockState(target.below()).isAir()) {
                return target;
            }
            target = target.below();
        }
        return pos;
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
        reachedLeavingExit = false;
        if (state == CustomerState.LEAVING) {
            return;
        }
        prepareToLeave();
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

    public boolean isSeatedForRendering() {
        CustomerState syncedState = readState(this.entityData.get(SYNCED_STATE));
        return !isTakeoutCustomer()
                && (syncedState == CustomerState.SEATED
                || syncedState == CustomerState.REQUESTING
                || syncedState == CustomerState.WAITING_FOR_PLATE
                || syncedState == CustomerState.EATING);
    }

    public CustomerPatience getPatienceForRendering() {
        return readPatience(this.entityData.get(SYNCED_PATIENCE));
    }

    public String getStateLabelForRendering() {
        return readState(this.entityData.get(SYNCED_STATE)).getDisplayName();
    }

    public int getOrderStartIndex() {
        return this.entityData.get(ORDER_START_INDEX);
    }

    private void syncState() {
        String stateName = state.name();
        if (!stateName.equals(this.entityData.get(SYNCED_STATE))) {
            this.entityData.set(SYNCED_STATE, stateName);
        }
    }

    private void syncPatience() {
        String patienceName = patience.name();
        if (!patienceName.equals(this.entityData.get(SYNCED_PATIENCE))) {
            this.entityData.set(SYNCED_PATIENCE, patienceName);
        }
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

    private void setConfiguredServiceType() {
        if (forcedServiceType != null) {
            serviceType = forcedServiceType;
            this.entityData.set(SYNCED_SERVICE_TYPE, serviceType.name());
            return;
        }
        serviceType = getConfiguredServiceType();
        this.entityData.set(SYNCED_SERVICE_TYPE, serviceType.name());
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !shouldStaySeated() && super.isPushable();
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

    public boolean receivePlate(Player player, ItemStack plateStack) {
        player.displayClientMessage(Component.literal("This customer is not waiting for dine-in food."), false);
        return false;
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
    protected FoodRequest getCurrentRequest() {
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

    protected boolean isOrderComplete() {
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

    protected void completeCurrentRequest(Player player, int reward, String message) {
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

    protected void completeCurrentRequestFromPlate(int reward) {
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

    protected int findMatchingRemainingRequestIndex(List<ItemStack> foods) {
        for (int i = currentRequestIndex; i < requests.size(); i++) {
            FoodRequest request = requests.get(i);
            if (request == null) {
                continue;
            }
            for (ItemStack food : foods) {
                if (!food.isEmpty() && food.getItem() == request.getItem()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void completePlateRequest(Player player, ItemStack plateStack, List<ItemStack> foods, int requestIndex) {
        FoodRequest request = requests.get(requestIndex);
        if (request == null) {
            return;
        }

        int reward = getScaledReward(request);
        this.spawnAtLocation(new ItemStack(Items.EMERALD, reward));
        if (restaurant != null) {
            restaurant.addOrderCompleted();
            restaurant.addMoneyEarned(reward);
            if (this.level() instanceof ServerLevel serverLevel) {
                RestaurantManager.get(serverLevel).setDirty();
            }
        }

        removeServedFoodFromPlate(plateStack, foods, request);
        requests.set(requestIndex, null);
        while (currentRequestIndex < requests.size() && requests.get(currentRequestIndex) == null) {
            currentRequestIndex++;
        }

        if (isOrderComplete()) {
            state = CustomerState.SERVED;
            this.setCustomName(Component.literal("Order complete"));
            this.setCustomNameVisible(true);
            releaseReservedSeat();
        } else {
            state = CustomerState.WAITING_FOR_PLATE;
            waitTimer = 0;
            noSeatWaitTimer = 0;
            patience = CustomerPatience.HAPPY;
            updateRequestName();
        }
        player.displayClientMessage(Component.literal("Plate accepted. Here's your change."), false);
    }

    protected void removeServedFoodFromPlate(ItemStack plateStack, List<ItemStack> foods, FoodRequest request) {
        List<ItemStack> remainingFoods = foods.stream()
                .map(ItemStack::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (int i = 0; i < remainingFoods.size(); i++) {
            if (remainingFoods.get(i).getItem() == request.getItem()) {
                remainingFoods.remove(i);
                break;
            }
        }
        PlateItem.setFoods(plateStack, remainingFoods);
    }

    protected Optional<FoodRequest> findMissingTakeoutRequest(List<ItemStack> foods) {
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

    protected void completeOrderFromContainer() {
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

    protected void scanForPlateMeal() {
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

    protected void reactToWrongOrder(String message) {
        waitTimer += TICKS_PER_MINUTE * 7;
        setPatience(waitTimer >= IMPATIENT_TO_ANGRY_TICKS ? CustomerPatience.ANGRY : CustomerPatience.IMPATIENT);
        this.setCustomName(Component.literal(message));
        this.setCustomNameVisible(true);
    }

    protected void finishEating() {
    }

    protected void advanceRequest() {
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

    protected boolean findAndReserveSeat() {
        return false;
    }

    protected Vec3 getSeatTarget(BlockPos seatPos) {
        return new Vec3(seatPos.getX() + 0.5, seatPos.getY() - 0.1, seatPos.getZ() + 0.5);
    }

    protected Vec3 getSeatApproachTarget() {
        if (reservedSeatPos == null) {
            return seatTarget;
        }
        return new Vec3(reservedSeatPos.getX() + 0.5, reservedSeatPos.getY() + 0.5, reservedSeatPos.getZ() + 0.5);
    }

    protected boolean findAndReserveQueueSpot() {
        if (restaurant == null || reservedQueuePos != null) {
            return reservedQueuePos != null;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        AABB bounds = restaurant.getBounds();
        BlockPos center = BlockPos.containing(bounds.getCenter());
        double radius = Math.max(8.0, Math.max(bounds.getXsize(), bounds.getZsize()) + 4.0);
        Optional<MarkedPoint> queueSpot = RestaurantSeatingApi.findNearestAvailableQueueSpot(center, radius, serverLevel);
        if (queueSpot.isEmpty()) {
            return false;
        }

        MarkedPoint point = queueSpot.get();
        if (!RestaurantSeatingApi.occupyQueueSpot(point.getPos(), this.getUUID(), serverLevel)) {
            return false;
        }
        reservedQueuePos = point.getPos();
        queueTarget = point.getInteractionPosition();
        return true;
    }

    protected boolean findAndReserveCounter() {
        if (!(this.level() instanceof ServerLevel serverLevel) || restaurant == null) {
            return false;
        }
        if (reservedCounterPos != null) {
            refreshCounterTarget();
            return true;
        }
        if (counterSearchCooldown > 0) {
            return false;
        }

        Optional<BlockPos> restaurantCounter = restaurant.getCounterPos();
        if (restaurantCounter.isPresent()) {
            BlockPos counterPos = restaurantCounter.get();
            RestaurantSeatingApi.markCounter(counterPos, serverLevel);
            if (reserveTakeoutCounterSlot(counterPos)) {
                reservedCounterPos = counterPos;
                refreshCounterTarget();
                counterSearchCooldown = 0;
                return true;
            }
            counterSearchCooldown = staggeredCooldown(RESERVATION_SEARCH_INTERVAL);
            return false;
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
        if (!reserveTakeoutCounterSlot(point.getPos())) {
            counterSearchCooldown = staggeredCooldown(RESERVATION_SEARCH_INTERVAL);
            return false;
        }
        reservedCounterPos = point.getPos();
        counterTarget = getCounterTarget(point.getPos(), serverLevel)
                .orElse(point.getInteractionPosition());
        counterSearchCooldown = 0;
        return true;
    }

    protected Optional<Vec3> getCounterTarget(BlockPos pos, ServerLevel serverLevel) {
        return getCashRegisterInteractionPosition(pos, serverLevel)
                .or(() -> RestaurantSeatingApi.getPoint(pos, serverLevel)
                        .map(MarkedPoint::getInteractionPosition));
    }

    protected Optional<Vec3> getCashRegisterInteractionPosition(BlockPos pos, ServerLevel serverLevel) {
        var state = serverLevel.getBlockState(pos);
        if (!state.is(com.restaurantmanager.setup.ModBlocks.CASH_REGISTER.get()) || !state.hasProperty(CashRegisterBlock.FACING)) {
            return Optional.empty();
        }
        return Optional.of(RestaurantSeatingApi.getCounterInteractionPosition(pos, state.getValue(CashRegisterBlock.FACING), serverLevel));
    }

    protected void refreshCounterTarget() {
        if (!(this.level() instanceof ServerLevel serverLevel) || reservedCounterPos == null) {
            return;
        }
        RestaurantSeatingApi.markCounter(reservedCounterPos, serverLevel);
        Vec3 baseTarget = getCounterTarget(reservedCounterPos, serverLevel)
                .orElse(Vec3.atBottomCenterOf(reservedCounterPos.above()));
        counterTarget = offsetCounterTargetForSlot(baseTarget, reservedCounterPos, serverLevel);
    }

    protected boolean canLeaveQueueForSeat() {
        return false;
    }

    protected boolean canLeaveQueueForCounter() {
        return restaurant != null && findAndReserveCounter();
    }

    protected boolean reserveTakeoutCounterSlot(BlockPos counterPos) {
        if (restaurant == null) {
            return false;
        }
        CounterSlotKey key = new CounterSlotKey(restaurant.getId(), counterPos);
        UUID[] slots = TAKEOUT_COUNTER_SLOTS.computeIfAbsent(key, ignored -> new UUID[TAKEOUT_COUNTER_SLOT_COUNT]);
        UUID customerId = this.getUUID();
        for (int i = 0; i < slots.length; i++) {
            if (customerId.equals(slots[i])) {
                reservedCounterSlot = i;
                return true;
            }
        }
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                slots[i] = customerId;
                reservedCounterSlot = i;
                return true;
            }
        }
        return false;
    }

    private void releaseTakeoutCounterSlot() {
        if (restaurant == null || reservedCounterPos == null || reservedCounterSlot < 0) {
            reservedCounterSlot = -1;
            return;
        }
        CounterSlotKey key = new CounterSlotKey(restaurant.getId(), reservedCounterPos);
        UUID[] slots = TAKEOUT_COUNTER_SLOTS.get(key);
        if (slots != null && reservedCounterSlot < slots.length && this.getUUID().equals(slots[reservedCounterSlot])) {
            slots[reservedCounterSlot] = null;
            if (java.util.Arrays.stream(slots).allMatch(java.util.Objects::isNull)) {
                TAKEOUT_COUNTER_SLOTS.remove(key);
            }
        }
        reservedCounterSlot = -1;
    }

    protected Vec3 offsetCounterTargetForSlot(Vec3 baseTarget, BlockPos counterPos, ServerLevel serverLevel) {
        if (reservedCounterSlot <= 0) {
            return baseTarget;
        }
        Vec3 counterCenter = Vec3.atCenterOf(counterPos);
        double dx = baseTarget.x - counterCenter.x;
        double dz = baseTarget.z - counterCenter.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001) {
            return baseTarget;
        }
        double sideX = -dz / length;
        double sideZ = dx / length;
        return new Vec3(baseTarget.x + sideX, baseTarget.y, baseTarget.z + sideZ);
    }

    protected boolean moveForwardInQueue() {
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

    protected void updateNoSeatPatience() {
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

    protected int getScaledReward(FoodRequest request) {
        return request == null ? 1 : getScaledReward(request.getReward());
    }

    protected int getScaledReward(int baseReward) {
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
        prepareToLeave();
        reachedLeavingExit = false;
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

    protected void releaseReservedSeat() {
    }

    protected void releaseReservedQueueSpot() {
        if (reservedQueuePos != null && this.level() instanceof ServerLevel serverLevel) {
            RestaurantSeatingApi.releaseQueueSpot(reservedQueuePos, this.getUUID(), serverLevel);
        }
        reservedQueuePos = null;
        queueTarget = null;
    }

    protected void releaseReservedCounter() {
        if (reservedCounterPos != null && this.level() instanceof ServerLevel serverLevel) {
            releaseTakeoutCounterSlot();
            RestaurantSeatingApi.releaseCounter(reservedCounterPos, this.getUUID(), serverLevel);
            reservedCounterPos = null;
            counterTarget = null;
        } else {
            releaseTakeoutCounterSlot();
        }
    }

    protected void releaseAllReservations() {
        releaseReservedSeat();
        releaseReservedQueueSpot();
        releaseReservedCounter();
    }

    protected void prepareToLeave() {
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

    protected void holdSeatPosition() {
    }

    protected void holdQueuePosition() {
        if (queueTarget == null) {
            return;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.distanceToSqr(queueTarget.x, queueTarget.y, queueTarget.z) > 0.5) {
            this.setPos(queueTarget.x, queueTarget.y, queueTarget.z);
        }
    }

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

    protected boolean shouldStaySeated() {
        return false;
    }

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

    protected void faceAssignedTable() {
    }

    protected void updateRequestName() {
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
            Vec3 approachTarget = customer.getSeatApproachTarget();
            double targetX = approachTarget != null ? approachTarget.x : bounds.getCenter().x;
            double targetY = approachTarget != null ? approachTarget.y : bounds.minY + 1.0;
            double targetZ = approachTarget != null ? approachTarget.z : bounds.getCenter().z;
            customer.getNavigation().moveTo(targetX, targetY, targetZ, 1.0);
            boolean reachedSeat = customer.seatTarget != null
                    && customer.distanceToSqr(targetX, targetY, targetZ) <= 2.0;
            if (reachedSeat) {
                customer.holdSeatPosition();
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
            if (customer.state == CustomerState.WAITING_FOR_PLATE) {
                customer.holdSeatPosition();
            } else if (customer.state == CustomerState.WAITING_FOR_PACKAGE) {
                customer.holdCounterPosition();
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
            customer.refreshCounterTarget();
            if (customer.counterTarget == null && !customer.findAndReserveCounter()) {
                if (customer.findAndReserveQueueSpot()) {
                    customer.state = CustomerState.JOINING_QUEUE;
                    return;
                }
                customer.updateNoSeatPatience();
                return;
            }

            boolean moving = customer.getNavigation().moveTo(customer.counterTarget.x, customer.counterTarget.y, customer.counterTarget.z, 1.0);
            boolean reachedTarget = customer.distanceToSqr(customer.counterTarget.x, customer.counterTarget.y, customer.counterTarget.z) <= COUNTER_REACHED_DISTANCE_SQR;
            boolean reachedCounterBlock = customer.reservedCounterPos != null
                    && customer.distanceToSqr(Vec3.atCenterOf(customer.reservedCounterPos)) <= COUNTER_BLOCK_REACHED_DISTANCE_SQR;
            if (reachedTarget || (!moving && reachedCounterBlock) || (customer.getNavigation().isDone() && reachedCounterBlock)) {
                customer.setPos(customer.counterTarget.x, customer.counterTarget.y, customer.counterTarget.z);
                customer.getNavigation().stop();
                customer.state = CustomerState.WAITING_AT_COUNTER;
                customer.waitTimer = 0;
                customer.updateRequestName();
            }
        }
    }

    private static class CustomerRandomStrollGoal extends RandomStrollGoal {
        private final CustomerEntity customer;

        public CustomerRandomStrollGoal(CustomerEntity customer) {
            super(customer, 0.6);
            this.customer = customer;
        }

        @Override
        public boolean canUse() {
            return customer.state == CustomerState.SPAWN && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return customer.state == CustomerState.SPAWN && super.canContinueToUse();
        }
    }

    private record CounterSlotKey(UUID restaurantId, BlockPos counterPos) {
    }
}
