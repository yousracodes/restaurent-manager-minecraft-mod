package com.restaurantmanager.entity;

import com.restaurantmanager.client.ClientCustomerScreens;
import com.restaurantmanager.food.FoodRegistryScanner;
import com.restaurantmanager.food.FoodRequest;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.OpenCustomerSuggestionPacket;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.seating.MarkedPoint;
import com.restaurantmanager.seating.RestaurantSeatingApi;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

public class CustomerEntity extends PathfinderMob {
    private static final Random REQUEST_RANDOM = new Random();

    private final List<FoodRequest> requests = new ArrayList<>();
    private int currentRequestIndex;
    private Restaurant restaurant;
    private CustomerState state = CustomerState.SPAWN;
    private int waitTimer;
    private BlockPos reservedSeatPos;
    private Vec3 seatTarget;

    public CustomerEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EnterRestaurantGoal(this));
        this.goalSelector.addGoal(2, new WaitForFoodGoal(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            handleState();
        }
    }

    private void handleState() {
        if (restaurant == null) {
            findRestaurant();
        }

        if (state == CustomerState.SPAWN && restaurant != null) {
            createRequests();
            state = CustomerState.ENTERING;
        }

        if (state == CustomerState.REQUESTING) {
            holdSeatPosition();
            waitTimer++;
            if (waitTimer > 600) {
                if (restaurant != null) {
                    restaurant.addCustomerLost();
                    restaurant.addOrderFailed();
                    if (this.level() instanceof ServerLevel serverLevel) {
                        RestaurantManager.get(serverLevel).setDirty();
                    }
                }
                state = CustomerState.LEAVING;
            }
        }

        if (state == CustomerState.SERVED) {
            processService();
            state = CustomerState.LEAVING;
        }

        if (state == CustomerState.LEAVING && restaurant != null) {
            releaseReservedSeat();
            if (this.distanceToSqr(restaurant.getSignPos().getX() + 0.5, restaurant.getSignPos().getY(), restaurant.getSignPos().getZ() + 0.5) > 256.0) {
                this.discard();
            }
        }
    }

    private void findRestaurant() {
        if (this.level() instanceof ServerLevel serverLevel) {
            RestaurantManager manager = RestaurantManager.get(serverLevel);
            this.restaurant = manager.getRestaurants().stream()
                    .filter(Restaurant::isActive)
                    .filter(Restaurant::isOpen)
                    .findFirst()
                    .orElse(null);
            if (restaurant != null) {
                this.setPos(restaurant.getSignPos().getX() + 1.5, restaurant.getSignPos().getY(), restaurant.getSignPos().getZ() + 0.5);
            }
        }
    }

    private void processService() {
        if (restaurant != null) {
            if (isOrderComplete()) {
                restaurant.addCustomerServed();
                restaurant.addReputation(1);
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                RestaurantManager.get(serverLevel).setDirty();
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseReservedSeat();
        super.remove(reason);
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
        FoodRequest request = getCurrentRequest();
        if (request == null) {
            player.displayClientMessage(Component.literal("I'm still deciding what to order."), false);
            return false;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() == request.getItem()) {
                stack.shrink(1);
                completeCurrentRequest(player, request.getReward(), "Thank you! Here's your change.");
                return true;
            }
        }

        player.displayClientMessage(Component.literal("You don't have: " + request.getDescription()), false);
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
                int reward = FoodRegistryScanner.calculateReward(suggestedStack);
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
        return requests.get(currentRequestIndex);
    }

    private boolean isOrderComplete() {
        return !requests.isEmpty() && currentRequestIndex >= requests.size();
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
        if (restaurant != null && !restaurant.getMenu().isEmpty()) {
            List<net.minecraft.resources.ResourceLocation> menuItems = restaurant.getMenu().getItems();
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

    private void advanceRequest() {
        currentRequestIndex++;
        if (isOrderComplete()) {
            state = CustomerState.SERVED;
            this.setCustomName(Component.literal("Order complete"));
            this.setCustomNameVisible(true);
            return;
        }
        state = CustomerState.REQUESTING;
        waitTimer = 0;
        updateRequestName();
    }

    private boolean findAndReserveSeat() {
        if (!(this.level() instanceof ServerLevel serverLevel) || restaurant == null || reservedSeatPos != null) {
            return reservedSeatPos != null;
        }

        AABB bounds = restaurant.getBounds();
        BlockPos center = BlockPos.containing(bounds.getCenter());
        double radius = Math.max(8.0, Math.max(bounds.getXsize(), bounds.getZsize()) + 4.0);
        Optional<MarkedPoint> seat = RestaurantSeatingApi.findNearestAvailableSeat(center, radius, serverLevel);
        if (seat.isEmpty()) {
            return false;
        }

        MarkedPoint point = seat.get();
        if (!RestaurantSeatingApi.occupySeat(point.getPos(), this.getUUID(), serverLevel)) {
            return false;
        }
        reservedSeatPos = point.getPos();
        seatTarget = point.getInteractionPosition();
        return true;
    }

    private void releaseReservedSeat() {
        if (reservedSeatPos != null && this.level() instanceof ServerLevel serverLevel) {
            RestaurantSeatingApi.releaseSeat(reservedSeatPos, this.getUUID(), serverLevel);
            reservedSeatPos = null;
            seatTarget = null;
        }
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

    private void updateRequestName() {
        FoodRequest request = getCurrentRequest();
        if (request == null) {
            return;
        }
        this.setCustomName(Component.literal("Request " + (currentRequestIndex + 1) + "/" + requests.size() + ": " + request.getDescription()));
        this.setCustomNameVisible(true);
    }

    private static class EnterRestaurantGoal extends Goal {
        private final CustomerEntity customer;

        public EnterRestaurantGoal(CustomerEntity customer) {
            this.customer = customer;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return (customer.state == CustomerState.ENTERING || customer.state == CustomerState.SEATING) && customer.restaurant != null;
        }

        @Override
        public void start() {
            if (customer.state == CustomerState.ENTERING && customer.findAndReserveSeat()) {
                customer.state = CustomerState.SEATING;
            }
        }

        @Override
        public void tick() {
            AABB bounds = customer.restaurant.getBounds();
            double targetX = customer.seatTarget != null ? customer.seatTarget.x : bounds.getCenter().x;
            double targetY = customer.seatTarget != null ? customer.seatTarget.y : bounds.minY + 1.0;
            double targetZ = customer.seatTarget != null ? customer.seatTarget.z : bounds.getCenter().z;
            customer.getNavigation().moveTo(targetX, targetY, targetZ, 1.0);
            boolean reachedSeat = customer.seatTarget != null && customer.distanceToSqr(targetX, targetY, targetZ) <= 1.0;
            boolean reachedRestaurant = customer.seatTarget == null && bounds.contains(customer.blockPosition().getX(), customer.blockPosition().getY(), customer.blockPosition().getZ());
            if (reachedSeat || reachedRestaurant) {
                if (reachedSeat) {
                    customer.setPos(targetX, targetY, targetZ);
                    customer.getNavigation().stop();
                }
                customer.state = CustomerState.WAITING;
                customer.waitTimer = 0;
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
            return customer.state == CustomerState.WAITING;
        }

        @Override
        public void start() {
            customer.state = CustomerState.REQUESTING;
            customer.waitTimer = 0;
        }

        @Override
        public void tick() {
            customer.holdSeatPosition();
            if (customer.getCurrentRequest() != null) {
                BlockPos signPos = customer.restaurant.getSignPos();
                customer.getLookControl().setLookAt(signPos.getX() + 0.5, signPos.getY() + 0.5, signPos.getZ() + 0.5);
            }
        }
    }
}
