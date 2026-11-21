package com.restaurantmanager.client;

import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.ApplyRestaurantMenuItemPacket;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RestaurantMenuItemSelectionScreen extends Screen {
    private final RestaurantManagementScreen parent;
    private int leftPos;
    private int topPos;
    private int gridX;
    private int gridY;
    private int backX;
    private int backY;

    public RestaurantMenuItemSelectionScreen(RestaurantManagementScreen parent) {
        super(Component.literal("Select Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = 220;
        int panelHeight = 150;
        this.leftPos = (this.width - panelWidth) / 2;
        this.topPos = (this.height - panelHeight) / 2;
        this.gridX = this.leftPos + 20;
        this.gridY = this.topPos + 38;
        this.backX = this.leftPos + 75;
        this.backY = this.topPos + 122;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        CodeGuiTheme.drawScreenPanel(graphics, this.leftPos, this.topPos, 220, 150);
        CodeGuiTheme.drawWoodHeader(graphics, this.leftPos + 10, this.topPos + 8, 200, 24);
        graphics.drawString(this.font, Component.literal("Select Restaurant Menu"),
                this.leftPos + 110 - this.font.width("Select Restaurant Menu") / 2,
                this.topPos + 16, CodeGuiTheme.LIGHT_TEXT, false);

        drawInventory(graphics, mouseX, mouseY);
        drawButton(graphics, this.backX, this.backY, 70, 20, "Back", 0xFF3C6596, mouseX, mouseY);
    }

    private void drawInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        Player player = Minecraft.getInstance().player;
        ItemStack hovered = ItemStack.EMPTY;
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; slot++) {
            int x = InventoryPanelRenderer.slotX(this.gridX, slot);
            int y = InventoryPanelRenderer.slotY(this.gridY, slot);
            ItemStack stack = player.getInventory().getItem(InventoryPanelRenderer.inventoryIndexForDisplaySlot(slot));
            drawSlot(graphics, x, y, stack.is(ModItems.RESTAURANT_MENU.get()));
            InventoryPanelRenderer.drawStack(graphics, this.font, stack, x, y);
            if (isInside(mouseX, mouseY, x, y, InventoryPanelRenderer.SLOT_SIZE, InventoryPanelRenderer.SLOT_SIZE)) {
                InventoryPanelRenderer.drawHover(graphics, x, y);
                hovered = stack;
            }
        }
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInside(mouseX, mouseY, this.backX, this.backY, 70, 20)) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (int slot = 0; slot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; slot++) {
                int x = InventoryPanelRenderer.slotX(this.gridX, slot);
                int y = InventoryPanelRenderer.slotY(this.gridY, slot);
                if (isInside(mouseX, mouseY, x, y, InventoryPanelRenderer.SLOT_SIZE, InventoryPanelRenderer.SLOT_SIZE)) {
                    int inventorySlot = InventoryPanelRenderer.inventoryIndexForDisplaySlot(slot);
                    ItemStack stack = player.getInventory().getItem(inventorySlot);
                    if (stack.is(ModItems.RESTAURANT_MENU.get())) {
                        ModNetwork.CHANNEL.sendToServer(new ApplyRestaurantMenuItemPacket(parent.getSignPos(), inventorySlot));
                        Minecraft.getInstance().setScreen(parent);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, boolean menuItem) {
        if (x == this.gridX && y == this.gridY) {
            InventoryPanelRenderer.drawPanel(graphics, this.gridX, this.gridY);
        }
        InventoryPanelRenderer.drawHighlight(graphics, x, y, menuItem);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, String label, int color, int mouseX, int mouseY) {
        CodeGuiTheme.drawButton(graphics, x, y, width, height,
                isInside(mouseX, mouseY, x, y, width, height), false);
        graphics.drawString(this.font, Component.literal(label), x + (width - this.font.width(label)) / 2,
                y + (height - this.font.lineHeight) / 2, CodeGuiTheme.LIGHT_TEXT, false);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
