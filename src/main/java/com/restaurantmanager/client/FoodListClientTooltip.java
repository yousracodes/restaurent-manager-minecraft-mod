package com.restaurantmanager.client;

import com.restaurantmanager.item.FoodListTooltipComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FoodListClientTooltip implements ClientTooltipComponent {
    private static final int ROW_HEIGHT = 18;
    private static final int ICON_TEXT_GAP = 4;
    private final List<ItemStack> foods;

    public FoodListClientTooltip(FoodListTooltipComponent component) {
        this.foods = component.getFoods();
    }

    @Override
    public int getHeight() {
        return Math.max(0, foods.size() * ROW_HEIGHT);
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (ItemStack food : foods) {
            width = Math.max(width, 16 + ICON_TEXT_GAP + font.width(food.getHoverName()));
        }
        return width;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        for (int i = 0; i < foods.size(); i++) {
            ItemStack food = foods.get(i);
            int rowY = y + i * ROW_HEIGHT;
            graphics.renderItem(food, x, rowY);
            graphics.renderItemDecorations(font, food, x, rowY);
            graphics.drawString(font, food.getHoverName(), x + 16 + ICON_TEXT_GAP, rowY + 4, 0xFFFFAA, false);
        }
    }
}
