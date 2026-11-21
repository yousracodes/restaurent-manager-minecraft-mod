package com.restaurantmanager.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
    private static final int HOTBAR_Y = 58;
    private static final int FRAME_COLOR = 0xFF4A2817;
    private static final int PANEL_COLOR = 0xFFE2CFA5;
    private static final int SLOT_DARK = 0xFF5C4A36;
    private static final int SLOT_LIGHT = 0xFFFFE9BB;
    private static final int SLOT_FILL = 0xFFB9A67F;
    private static final int LEGACY_FRAME_COLOR = 0xFF2E2E2E;
    private static final int LEGACY_PANEL_COLOR = 0xFFC7C5BF;
    private static final int LEGACY_SLOT_DARK = 0xFF555555;
    private static final int LEGACY_SLOT_LIGHT = 0xFFE8E8E8;
    private static final int LEGACY_SLOT_FILL = 0xFF8B8B8B;

    private InventoryPanelRenderer() {
    }

    static void drawPanel(GuiGraphics graphics, int gridX, int gridY) {
        drawPanel(graphics, gridX, gridY, 0);
    }

    /**
     * Container menus render a 16px item at the slot's coordinate, so their
     * 18px background must begin one pixel earlier to share the same center.
     */
    static void drawContainerPanel(GuiGraphics graphics, int gridX, int gridY) {
        drawPanel(graphics, gridX, gridY, -1);
    }

    static void drawLegacyContainerPanel(GuiGraphics graphics, int gridX, int gridY) {
        int slotOffset = -1;
        int panelX = gridX - PANEL_MARGIN_X + slotOffset;
        int panelY = gridY - PANEL_MARGIN_Y + slotOffset;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, LEGACY_FRAME_COLOR);
        graphics.fill(panelX + 2, panelY + 2, panelX + PANEL_WIDTH - 2, panelY + PANEL_HEIGHT - 2,
                LEGACY_PANEL_COLOR);

        for (int displaySlot = 0; displaySlot < DISPLAY_SLOT_COUNT; displaySlot++) {
            drawLegacySlotBackground(graphics, slotX(gridX, displaySlot) + slotOffset,
                    slotY(gridY, displaySlot) + slotOffset);
        }
    }

    private static void drawPanel(GuiGraphics graphics, int gridX, int gridY, int slotOffset) {
        int panelX = gridX - PANEL_MARGIN_X + slotOffset;
        int panelY = gridY - PANEL_MARGIN_Y + slotOffset;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, FRAME_COLOR);
        graphics.fill(panelX + 2, panelY + 2, panelX + PANEL_WIDTH - 2, panelY + PANEL_HEIGHT - 2, PANEL_COLOR);

        for (int displaySlot = 0; displaySlot < DISPLAY_SLOT_COUNT; displaySlot++) {
            drawSlotBackground(graphics, slotX(gridX, displaySlot) + slotOffset,
                    slotY(gridY, displaySlot) + slotOffset);
        }
    }

    private static void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_DARK);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_LIGHT);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_FILL);
    }

    private static void drawLegacySlotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, LEGACY_SLOT_DARK);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, LEGACY_SLOT_LIGHT);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, LEGACY_SLOT_FILL);
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
