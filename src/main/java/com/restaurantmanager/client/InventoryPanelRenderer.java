package com.restaurantmanager.client;

import com.restaurantmanager.RestaurantManagerMod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class InventoryPanelRenderer {
    static final int SLOT_SIZE = 18;
    static final int COLUMNS = 9;
    static final int ROWS = 4;
    static final int PANEL_WIDTH = 172;
    static final int PANEL_HEIGHT = 73;
    static final int PANEL_MARGIN_X = 5;
    static final int PANEL_MARGIN_Y = 1;
    static final int SLOT_STEP = 18;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RestaurantManagerMod.MODID, "textures/gui/inventory_panel.png");

    private InventoryPanelRenderer() {
    }

    static void drawPanel(GuiGraphics graphics, int gridX, int gridY) {
        graphics.blit(TEXTURE, gridX - PANEL_MARGIN_X, gridY - PANEL_MARGIN_Y, 0, 0,
                PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
    }

    static void drawHighlight(GuiGraphics graphics, int x, int y, boolean valid) {
        if (valid) {
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x446A8D46);
        }
    }

    static void drawHover(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x66FFFFFF);
    }

    static void drawStack(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }
}
