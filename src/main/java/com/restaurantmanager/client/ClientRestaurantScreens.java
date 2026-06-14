package com.restaurantmanager.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ClientRestaurantScreens {
    public static void openManagement(BlockPos signPos, boolean open) {
        Minecraft.getInstance().setScreen(new RestaurantManagementScreen(signPos, open));
    }

    public static void openManagement(BlockPos signPos, boolean open, String name, String ownerName, int reputation,
                                      int customersServed, int ordersCompleted, int ordersFailed,
                                      int customersLost, int moneyEarned, String menuTitle, int menuItemCount) {
        Minecraft.getInstance().setScreen(new RestaurantManagementScreen(signPos, open, name, ownerName, reputation,
                customersServed, ordersCompleted, ordersFailed, customersLost, moneyEarned, menuTitle, menuItemCount));
    }

    public static void openMenu(BlockPos signPos, List<ResourceLocation> items) {
        Minecraft.getInstance().setScreen(new RestaurantMenuScreen(signPos, items));
    }

    public static void openMenuItemEditor(InteractionHand hand, ItemStack stack) {
        Minecraft.getInstance().setScreen(new MenuItemEditorScreen(hand, stack));
    }
}
