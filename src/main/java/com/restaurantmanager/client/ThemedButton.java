package com.restaurantmanager.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** A standard widget with the same code-drawn wood style as the restaurant screens. */
final class ThemedButton extends AbstractButton {
    private final Consumer<ThemedButton> onPress;
    private final boolean danger;

    ThemedButton(int x, int y, int width, int height, Component message,
                 Consumer<ThemedButton> onPress) {
        this(x, y, width, height, message, onPress, false);
    }

    ThemedButton(int x, int y, int width, int height, Component message,
                 Consumer<ThemedButton> onPress, boolean danger) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.danger = danger;
    }

    @Override
    public void onPress() {
        this.onPress.accept(this);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        CodeGuiTheme.drawButton(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                this.active && this.isHoveredOrFocused(), this.danger);
        int color = this.active ? CodeGuiTheme.LIGHT_TEXT : 0xFF9B8667;
        Component message = this.getMessage();
        int textX = this.getX() + (this.getWidth() - Minecraft.getInstance().font.width(message)) / 2;
        int textY = this.getY() + (this.getHeight() - Minecraft.getInstance().font.lineHeight) / 2;
        graphics.drawString(Minecraft.getInstance().font, message, textX, textY, color, false);
    }
}
