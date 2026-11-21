package com.restaurantmanager.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Small, texture-free drawing toolkit shared by the restaurant screens.
 * Keeping the chrome here makes every screen scale cleanly without requiring
 * a large background image for each layout.
 */
final class CodeGuiTheme {
    static final int TEXT = 0xFF2B2118;
    static final int MUTED_TEXT = 0xFF6F5538;
    static final int LIGHT_TEXT = 0xFFFFE7BE;

    private static final int SHADOW = 0x88000000;
    private static final int OUTLINE = 0xFF24170F;
    private static final int WOOD_DARK = 0xFF4A2817;
    private static final int WOOD = 0xFF8A4E28;
    private static final int WOOD_LIGHT = 0xFFC27A3D;
    private static final int PARCHMENT_DARK = 0xFF9C815D;
    private static final int PARCHMENT = 0xFFE2CFA5;
    private static final int PARCHMENT_LIGHT = 0xFFFFE9BB;
    private static final int SLOT_DARK = 0xFF5C4A36;
    private static final int SLOT = 0xFFB9A67F;

    private CodeGuiTheme() {
    }

    static void drawScreenPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width < 8 || height < 8) {
            return;
        }
        graphics.fill(x + 4, y + 5, x + width + 4, y + height + 5, SHADOW);
        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, WOOD_DARK);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, WOOD);
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, PARCHMENT);
        graphics.fill(x + 5, y + 5, x + width - 5, y + 7, PARCHMENT_LIGHT);
        graphics.fill(x + 5, y + height - 7, x + width - 5, y + height - 5, PARCHMENT_DARK);
        drawNail(graphics, x + 6, y + 6);
        drawNail(graphics, x + width - 9, y + 6);
        drawNail(graphics, x + 6, y + height - 9);
        drawNail(graphics, x + width - 9, y + height - 9);
    }

    static void drawInsetPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width < 4 || height < 4) {
            return;
        }
        graphics.fill(x, y, x + width, y + height, WOOD_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PARCHMENT_DARK);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFF2DDB2);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, 0xFFFFEDC5);
    }

    static void drawWoodHeader(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width < 4 || height < 4) {
            return;
        }
        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, WOOD);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 4, WOOD_LIGHT);
        graphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 2, WOOD_DARK);
        drawNail(graphics, x + 4, y + 4);
        drawNail(graphics, x + width - 7, y + 4);
    }

    static void drawSlot(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, SLOT_DARK);
        graphics.fill(x + 2, y + 2, x + size, y + size, PARCHMENT_LIGHT);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, SLOT);
    }

    static void drawButton(GuiGraphics graphics, int x, int y, int width, int height,
                           boolean hovered, boolean danger) {
        int base = danger ? 0xFF873A2E : 0xFF6C492D;
        int light = danger ? 0xFFC45A48 : 0xFFA87545;
        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, hovered ? light : base);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 5, hovered ? 0xFFD98768 : WOOD_LIGHT);
        graphics.fill(x + 3, y + height - 5, x + width - 3, y + height - 3, WOOD_DARK);
    }

    static void drawSeparator(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, PARCHMENT_DARK);
        graphics.fill(x, y + 1, x + width, y + 2, PARCHMENT_LIGHT);
    }

    static void drawArrow(GuiGraphics graphics, int centerX, int centerY, boolean right, int color) {
        for (int offset = 0; offset <= 4; offset++) {
            int halfHeight = 4 - offset;
            int x = right ? centerX - 2 + offset : centerX + 2 - offset;
            graphics.fill(x, centerY - halfHeight, x + 1, centerY + halfHeight + 1, color);
        }
    }

    static void drawMagnifier(GuiGraphics graphics, int x, int y, int size) {
        int ring = Math.max(5, size * 2 / 3);
        int thickness = Math.max(1, size / 8);
        graphics.fill(x + thickness, y, x + ring - thickness, y + thickness, MUTED_TEXT);
        graphics.fill(x, y + thickness, x + thickness, y + ring - thickness, MUTED_TEXT);
        graphics.fill(x + ring - thickness, y + thickness, x + ring, y + ring - thickness, MUTED_TEXT);
        graphics.fill(x + thickness, y + ring - thickness, x + ring - thickness, y + ring, MUTED_TEXT);
        int handleStart = x + ring - thickness;
        int handleY = y + ring - thickness;
        graphics.fill(handleStart, handleY, x + size, y + size, MUTED_TEXT);
    }

    private static void drawNail(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 3, y + 3, OUTLINE);
        graphics.fill(x, y, x + 2, y + 2, 0xFFD5C39F);
    }
}
