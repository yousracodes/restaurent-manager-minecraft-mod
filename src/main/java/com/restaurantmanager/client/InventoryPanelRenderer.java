package com.restaurantmanager.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class InventoryPanelRenderer {
    static final int SLOT_SIZE = 18;
    static final int COLUMNS = 9;
    static final int ROWS = 4;
    static final int PANEL_WIDTH = 176;
    static final int PANEL_HEIGHT = 90;
    static final int PANEL_MARGIN_X = 7;
    static final int PANEL_MARGIN_Y = 7;
    static final int SLOT_STEP = 18;
    static final int DISPLAY_SLOT_COUNT = 36;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/inventory.png");
    private static final int INVENTORY_TEXTURE_Y = 76;
    private static final int HOTBAR_Y = 58;

    private InventoryPanelRenderer() {
    }

    static void drawPanel(GuiGraphics graphics, int gridX, int gridY) {
        graphics.blit(TEXTURE, gridX - PANEL_MARGIN_X, gridY - PANEL_MARGIN_Y, 0, INVENTORY_TEXTURE_Y,
                PANEL_WIDTH, PANEL_HEIGHT);
    }

    static int slotX(int gridX, int displaySlot) {
        return gridX + (displaySlot % COLUMNS) * SLOT_STEP;
    }

    static int slotY(int gridY, int displaySlot) {
        if (displaySlot < 3 * COLUMNS) {
            return gridY + (displaySlot / COLUMNS) * SLOT_STEP;
        }
        return gridY + HOTBAR_Y;
    }

    static int inventoryIndexForDisplaySlot(int displaySlot) {
        if (displaySlot < 3 * COLUMNS) {
            return displaySlot + COLUMNS;
        }
        return displaySlot - 3 * COLUMNS;
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
