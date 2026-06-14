package com.restaurantmanager.restaurant;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestaurantMenu {
    private final List<ResourceLocation> items = new ArrayList<>();
    private String title = "";

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
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
