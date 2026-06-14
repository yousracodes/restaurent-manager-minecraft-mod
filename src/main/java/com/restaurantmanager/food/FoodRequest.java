package com.restaurantmanager.food;

import net.minecraft.world.item.Item;

public class FoodRequest {
    private final Item item;
    private final int reward;
    private final String description;

    public FoodRequest(Item item, int reward, String description) {
        this.item = item;
        this.reward = reward;
        this.description = description;
    }

    public Item getItem() {
        return item;
    }

    public int getReward() {
        return reward;
    }

    public String getDescription() {
        return description;
    }
}
