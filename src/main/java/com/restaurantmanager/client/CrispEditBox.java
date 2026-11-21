package com.restaurantmanager.client;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * EditBox behavior with text rendered without Minecraft's forced drop shadow.
 */
final class CrispEditBox extends EditBox {
    private static final int SELECTION_COLOR = 0xFF6D85B5;

    private final Font font;
    private final int textColor;

    CrispEditBox(Font font, int x, int y, int width, int height, Component message, int textColor) {
        super(font, x, y, width, height, message);
        this.font = font;
        this.textColor = textColor;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        String value = getValue();
        int cursor = getCursorPosition();
        int availableWidth = Math.max(1, getWidth());
        int displayStart = findDisplayStart(value, cursor, availableWidth);
        String visibleText = this.font.plainSubstrByWidth(value.substring(displayStart), availableWidth);
        int displayEnd = displayStart + visibleText.length();
        int textX = getX();
        int textY = getY() + (getHeight() - this.font.lineHeight) / 2;

        if (isFocused()) {
            drawSelection(graphics, value, cursor, displayStart, displayEnd, textX, textY);
        }

        graphics.drawString(this.font, Component.literal(visibleText), textX, textY, this.textColor, false);

        if (isFocused() && getHighlighted().isEmpty() && (Util.getMillis() / 300L) % 2L == 0L) {
            int visibleCursor = Math.max(displayStart, Math.min(cursor, displayEnd));
            int cursorX = textX + this.font.width(value.substring(displayStart, visibleCursor));
            graphics.fill(cursorX, textY - 1, cursorX + 1, textY + this.font.lineHeight + 1, this.textColor);
        }
    }

    private int findDisplayStart(String value, int cursor, int availableWidth) {
        String beforeCursor = value.substring(0, cursor);
        if (this.font.width(beforeCursor) <= availableWidth) {
            return 0;
        }
        String visibleBeforeCursor = this.font.plainSubstrByWidth(beforeCursor, availableWidth, true);
        return cursor - visibleBeforeCursor.length();
    }

    private void drawSelection(GuiGraphics graphics, String value, int cursor, int displayStart, int displayEnd,
                               int textX, int textY) {
        String highlighted = getHighlighted();
        if (highlighted.isEmpty()) {
            return;
        }

        int anchor = findSelectionAnchor(value, cursor, highlighted);
        int selectionStart = Math.max(displayStart, Math.min(cursor, anchor));
        int selectionEnd = Math.min(displayEnd, Math.max(cursor, anchor));
        if (selectionStart >= selectionEnd) {
            return;
        }

        int selectionX = textX + this.font.width(value.substring(displayStart, selectionStart));
        int selectionEndX = textX + this.font.width(value.substring(displayStart, selectionEnd));
        graphics.fill(selectionX, textY - 1, selectionEndX, textY + this.font.lineHeight + 1, SELECTION_COLOR);
    }

    private int findSelectionAnchor(String value, int cursor, String highlighted) {
        int length = highlighted.length();
        int before = cursor - length;
        if (before >= 0 && value.regionMatches(before, highlighted, 0, length)) {
            return before;
        }
        int after = cursor + length;
        if (after <= value.length() && value.regionMatches(cursor, highlighted, 0, length)) {
            return after;
        }
        return cursor;
    }
}
