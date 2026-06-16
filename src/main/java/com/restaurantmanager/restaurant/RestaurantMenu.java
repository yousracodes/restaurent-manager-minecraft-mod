package com.restaurantmanager.restaurant;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestaurantMenu {
    private final List<ResourceLocation> items = new ArrayList<>();
    private String title = "";
    private MenuCategory category = MenuCategory.MAIN_DISH;

    public RestaurantMenu() {
    }

    public RestaurantMenu(List<ResourceLocation> items) {
        for (ResourceLocation item : items) {
            addItem(item);
        }
    }

    public RestaurantMenu(String title, List<ResourceLocation> items) {
        this(items);
        this.title = title == null ? "" : title;
    }

    public RestaurantMenu(String title, MenuCategory category, List<ResourceLocation> items) {
        this(title, items);
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public MenuCategory getCategory() {
        return category;
    }

    public void setCategory(MenuCategory category) {
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
    }

    public List<ResourceLocation> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean addItem(ResourceLocation item) {
        if (items.contains(item)) {
            return false;
        }
        items.add(item);
        return true;
    }

    public boolean removeItem(ResourceLocation item) {
        return items.remove(item);
    }

    public void replaceItems(List<ResourceLocation> newItems) {
        items.clear();
        for (ResourceLocation item : newItems) {
            addItem(item);
        }
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
