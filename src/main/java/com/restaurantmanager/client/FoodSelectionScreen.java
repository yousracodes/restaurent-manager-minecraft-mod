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
        CodeGuiTheme.drawScreenPanel(graphics, this.leftPos, this.topPos, this.panelWidth, this.panelHeight);
        CodeGuiTheme.drawWoodHeader(graphics, this.leftPos + 10, this.topPos + 8, this.panelWidth - 20, 20);
        drawCenteredCrispString(graphics, "Select a food item", this.leftPos + this.panelWidth / 2,
                this.topPos + 14, CodeGuiTheme.LIGHT_TEXT);

        Player player = Minecraft.getInstance().player;
        ItemStack hovered = ItemStack.EMPTY;
        if (player != null) {
            for (int slot = 0; slot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; slot++) {
                int x = InventoryPanelRenderer.slotX(this.gridX, slot);
                int y = InventoryPanelRenderer.slotY(this.gridY, slot);
                ItemStack stack = player.getInventory().getItem(InventoryPanelRenderer.inventoryIndexForDisplaySlot(slot));
                drawSlot(graphics, x, y, stack.isEdible());
                InventoryPanelRenderer.drawStack(graphics, this.font, stack, x, y);
                if (isInside(mouseX, mouseY, x, y, InventoryPanelRenderer.SLOT_SIZE, InventoryPanelRenderer.SLOT_SIZE)) {
                    InventoryPanelRenderer.drawHover(graphics, x, y);
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
            for (int slot = 0; slot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; slot++) {
                int x = InventoryPanelRenderer.slotX(this.gridX, slot);
                int y = InventoryPanelRenderer.slotY(this.gridY, slot);
                if (isInside(mouseX, mouseY, x, y, InventoryPanelRenderer.SLOT_SIZE, InventoryPanelRenderer.SLOT_SIZE)) {
                    ItemStack stack = player.getInventory().getItem(InventoryPanelRenderer.inventoryIndexForDisplaySlot(slot));
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
        if (x == this.gridX && y == this.gridY) {
            InventoryPanelRenderer.drawPanel(graphics, this.gridX, this.gridY);
        }
        InventoryPanelRenderer.drawHighlight(graphics, x, y, edible);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, String label, int color, int mouseX, int mouseY) {
        CodeGuiTheme.drawButton(graphics, x, y, width, height,
                isInside(mouseX, mouseY, x, y, width, height), false);
        drawCenteredCrispString(graphics, label, x + width / 2,
                y + (height - this.font.lineHeight) / 2, CodeGuiTheme.LIGHT_TEXT);
    }

    private void drawCenteredCrispString(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawString(this.font, Component.literal(text), centerX - this.font.width(text) / 2, y, color, false);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
