package com.restaurantmanager.client;

import com.restaurantmanager.entity.CustomerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class ClientCustomerScreens {
    public static void open(int customerId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(customerId);
        if (entity instanceof CustomerEntity customer) {
            minecraft.setScreen(new CustomerConversationScreen(customer));
        } else {
            minecraft.setScreen(new CustomerConversationScreen(customerId, Component.literal("Customer")));
        }
    }

    public static void openSuggestion(int customerId, String customerName) {
        Minecraft minecraft = Minecraft.getInstance();
        Component fallbackName = Component.literal(customerName == null || customerName.isBlank() ? "Customer" : customerName);
        if (minecraft.level == null) {
            minecraft.setScreen(new CustomerConversationScreen(customerId, fallbackName, ItemStack.EMPTY));
            return;
        }

        Entity entity = minecraft.level.getEntity(customerId);
        if (entity instanceof CustomerEntity customer) {
            minecraft.setScreen(new CustomerConversationScreen(customer.getId(), customer.getDisplayName(), ItemStack.EMPTY));
        } else {
            minecraft.setScreen(new CustomerConversationScreen(customerId, fallbackName, ItemStack.EMPTY));
        }
    }
}
