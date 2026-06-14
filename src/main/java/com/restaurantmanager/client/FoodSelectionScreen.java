package com.restaurantmanager.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FoodSelectionScreen extends Screen {
    private final int customerId;
    private final Component customerName;
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private int gridX;
    private int gridY;
    private int backX;
    private int backY;
    private int backWidth;
    private int backHeight;

    public FoodSelectionScreen(int customerId, Component customerName) {
        super(Component.literal("Select Food"));
        this.customerId = customerId;
        this.customerName = customerName;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(220, this.width - 20);
        this.panelHeight = 160;
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        this.gridX = this.leftPos + 17;
        this.gridY = this.topPos + 34;
        this.backWidth = 70;
        this.backHeight = 20;
        this.backX = this.leftPos + (this.panelWidth - this.backWidth) / 2;
        this.backY = this.topPos + this.panelHeight - 27;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF2E2E2E);
        graphics.fill(this.leftPos + 3, this.topPos + 3, this.leftPos + this.panelWidth - 3, this.topPos + this.panelHeight - 3, 0xFFC7C5BF);
        graphics.fill(this.leftPos + 8, this.topPos + 8, this.leftPos + this.panelWidth - 8, this.topPos + this.panelHeight - 8, 0xFFB6B3AB);
        graphics.drawCenteredString(this.font, Component.literal("Select a food item"), this.leftPos + this.panelWidth / 2, this.topPos + 14, 0x2B2118);

        Player player = Minecraft.getInstance().player;
        ItemStack hovered = ItemStack.EMPTY;
        if (player != null) {
            for (int slot = 0; slot < 36; slot++) {
                int x = this.gridX + (slot % 9) * 20;
                int y = this.gridY + (slot / 9) * 18;
                ItemStack stack = player.getInventory().getItem(slot);
                drawSlot(graphics, x, y, stack.isEdible());
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, x + 1, y + 1);
                    graphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
                }
                if (isInside(mouseX, mouseY, x, y, 18, 18)) {
                    graphics.fill(x, y, x + 18, y + 18, 0x66FFFFFF);
                    hovered = stack;
                }
            }
        }

        drawButton(graphics, this.backX, this.backY, this.backWidth, this.backHeight, "Back", 0xFF3C6596, mouseX, mouseY);
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInside(mouseX, mouseY, this.backX, this.backY, this.backWidth, this.backHeight)) {
            Minecraft.getInstance().setScreen(new CustomerConversationScreen(customerId, customerName));
            return true;
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (int slot = 0; slot < 36; slot++) {
                int x = this.gridX + (slot % 9) * 20;
                int y = this.gridY + (slot / 9) * 18;
                if (isInside(mouseX, mouseY, x, y, 18, 18)) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (!stack.isEmpty() && stack.isEdible()) {
                        Minecraft.getInstance().setScreen(new CustomerConversationScreen(customerId, customerName, stack));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, boolean edible) {
        graphics.fill(x, y, x + 18, y + 18, edible ? 0xFF6A8D46 : 0xFF565656);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8D8D8D);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF303030);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, String label, int color, int mouseX, int mouseY) {
        int fill = isInside(mouseX, mouseY, x, y, width, height) ? brighten(color) : color;
        graphics.fill(x, y, x + width, y + height, 0xFF1F1F1F);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
        graphics.drawCenteredString(this.font, Component.literal(label), x + width / 2, y + 6, 0xFFFFFF);
    }

    private int brighten(int color) {
        int r = Math.min(255, ((color >> 16) & 255) + 28);
        int g = Math.min(255, ((color >> 8) & 255) + 28);
        int b = Math.min(255, (color & 255) + 28);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
