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
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 220, this.topPos + 150, 0xFF2E2E2E);
        graphics.fill(this.leftPos + 3, this.topPos + 3, this.leftPos + 217, this.topPos + 147, 0xFFC7C5BF);
        graphics.fill(this.leftPos + 8, this.topPos + 8, this.leftPos + 212, this.topPos + 142, 0xFFB6B3AB);
        graphics.drawCenteredString(this.font, Component.literal("Select Restaurant Menu"), this.leftPos + 110, this.topPos + 16, 0x2B2118);

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
