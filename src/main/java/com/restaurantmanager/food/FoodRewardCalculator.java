package com.restaurantmanager.food;

import net.minecraft.world.food.FoodProperties;

public class FoodRewardCalculator {
    public static int calculateReward(FoodProperties food) {
        if (food == null) {
            return 1;
        }

        double base = food.getNutrition() * 0.4 + food.getSaturationModifier() * 1.25;
        int reward = (int) Math.ceil(base / 2.0);
        return Math.max(1, Math.min(reward, 20));
    }
}
