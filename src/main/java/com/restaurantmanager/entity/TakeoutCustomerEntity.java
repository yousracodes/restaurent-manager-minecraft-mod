package com.restaurantmanager.entity;

import com.restaurantmanager.food.FoodRequest;
import com.restaurantmanager.item.TakeoutPackageItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

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

}
