package com.restaurantmanager.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FoodListTooltipComponent implements TooltipComponent {
    private final List<ItemStack> foods;

    public FoodListTooltipComponent(List<ItemStack> foods) {
        this.foods = foods.stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> stack.copyWithCount(1))
                .toList();
    }

    public List<ItemStack> getFoods() {
        return foods;
    }
}
