package com.restaurantmanager.food;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FoodRegistryScanner {
    private static final List<Item> edibleItems = new ArrayList<>();
    private static final List<Item> foodRequestBag = new ArrayList<>();
    private static final Random random = new Random();

    public static void scanEdibleFoods() {
        edibleItems.clear();
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEdible()) {
                FoodProperties food = stack.getFoodProperties(null);
                if (food != null && food.getNutrition() > 0) {
                    edibleItems.add(item);
                }
            }
        }
        Collections.shuffle(edibleItems, random);
        refillRequestBag();
    }

    public static boolean hasEdibleFoods() {
        return !edibleItems.isEmpty();
    }

    public static Item getRandomFood() {
        if (edibleItems.isEmpty()) {
            return Items.BREAD;
        }
        if (foodRequestBag.isEmpty()) {
            refillRequestBag();
        }
        return foodRequestBag.remove(foodRequestBag.size() - 1);
    }

    public static FoodRequest createRequest() {
        Item item = getRandomFood();
        return createRequest(item);
    }

    public static FoodRequest createRequest(Item item) {
        ItemStack stack = new ItemStack(item);
        int reward = calculateReward(stack);
        String description = "I'm craving " + stack.getHoverName().getString() + ".";
        return new FoodRequest(item, reward, description);
    }

    public static ItemStack findFoodStack(String input) {
        String normalized = input == null ? "" : input.trim();
        if (normalized.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ResourceLocation id = ResourceLocation.tryParse(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
            ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
            return stack.isEdible() ? stack : ItemStack.EMPTY;
        }

        String displayName = normalized.replace('_', ' ').toLowerCase();
        for (Item item : edibleItems) {
            ItemStack stack = new ItemStack(item);
            if (stack.getHoverName().getString().toLowerCase().equals(displayName)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static int calculateReward(ItemStack stack) {
        return FoodRewardCalculator.calculateReward(stack.getFoodProperties(null));
    }

    private static void refillRequestBag() {
        foodRequestBag.clear();
        foodRequestBag.addAll(edibleItems);
        Collections.shuffle(foodRequestBag, random);
    }
}
