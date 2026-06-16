package com.restaurantmanager.restaurant;

import java.util.Locale;

public enum MenuCategory {
    MAIN_DISH,
    DRINK,
    DESSERT,
    BREAKFAST,
    APPETIZER,
    SPECIAL;

    public String getDisplayName() {
        String name = this.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static MenuCategory byName(String name) {
        if (name == null || name.isBlank()) {
            return MAIN_DISH;
        }
        try {
            return MenuCategory.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return MAIN_DISH;
        }
    }
}
