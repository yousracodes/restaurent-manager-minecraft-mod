package com.restaurantmanager.client;

import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.ApplyRestaurantMenuItemPacket;
import com.restaurantmanager.restaurant.MenuCategory;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RestaurantMenuSelectionScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 184;
    private static final int TEXT_COLOR = 0x2B2118;

    private final RestaurantMenuManagerScreen parent;
    private final BlockPos signPos;
    private final MenuCategory category;
    private int leftPos;
    private int topPos;
    private int gridX;
    private int gridY;
    private int backX;
    private int backY;

    public RestaurantMenuSelectionScreen(RestaurantMenuManagerScreen parent, BlockPos signPos, MenuCategory category) {
        super(Component.literal("Choose Menu"));
        this.parent = parent;
        this.signPos = signPos;
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;
        this.gridX = this.leftPos + 39;
        this.gridY = this.topPos + 50;
        this.backX = this.leftPos + (PANEL_WIDTH - 70) / 2;
        this.backY = this.topPos + PANEL_HEIGHT - 30;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, 0xFF2E2E2E);
        graphics.fill(this.leftPos + 3, this.topPos + 3, this.leftPos + PANEL_WIDTH - 3, this.topPos + PANEL_HEIGHT - 3, 0xFFC7C5BF);
        graphics.fill(this.leftPos + 8, this.topPos + 8, this.leftPos + PANEL_WIDTH - 8, this.topPos + PANEL_HEIGHT - 8, 0xFFB6B3AB);
        drawFittedCentered(graphics, "Choose menu - " + this.category.getDisplayName(),
                this.leftPos + PANEL_WIDTH / 2, this.topPos + 15, PANEL_WIDTH - 24, TEXT_COLOR);

        drawInventory(graphics, mouseX, mouseY);
        drawButton(graphics, this.backX, this.backY, 70, 20, "Back", 0xFF3C6596, mouseX, mouseY);
    }

    private void drawInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        InventoryPanelRenderer.drawPanel(graphics, this.gridX, this.gridY);
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ItemStack hovered = ItemStack.EMPTY;
        for (int displaySlot = 0; displaySlot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; displaySlot++) {
            int inventorySlot = InventoryPanelRenderer.inventoryIndexForDisplaySlot(displaySlot);
            int x = InventoryPanelRenderer.slotX(this.gridX, displaySlot);
            int y = InventoryPanelRenderer.slotY(this.gridY, displaySlot);
            ItemStack stack = player.getInventory().getItem(inventorySlot);
            boolean validMenu = isMatchingMenu(stack);
            InventoryPanelRenderer.drawHighlight(graphics, x, y, validMenu);
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
            for (int displaySlot = 0; displaySlot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; displaySlot++) {
                int x = InventoryPanelRenderer.slotX(this.gridX, displaySlot);
                int y = InventoryPanelRenderer.slotY(this.gridY, displaySlot);
                if (isInside(mouseX, mouseY, x, y, InventoryPanelRenderer.SLOT_SIZE, InventoryPanelRenderer.SLOT_SIZE)) {
                    int inventorySlot = InventoryPanelRenderer.inventoryIndexForDisplaySlot(displaySlot);
                    ItemStack stack = player.getInventory().getItem(inventorySlot);
                    if (isMatchingMenu(stack)) {
                        ModNetwork.CHANNEL.sendToServer(new ApplyRestaurantMenuItemPacket(this.signPos, inventorySlot, this.category));
                        parent.setMenuSummary(this.category, RestaurantMenuItem.getMenuTitle(stack), RestaurantMenuItem.getMenuItems(stack).size());
                        Minecraft.getInstance().setScreen(parent);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMatchingMenu(ItemStack stack) {
        return stack.is(ModItems.RESTAURANT_MENU.get()) && RestaurantMenuItem.getMenuCategory(stack) == this.category;
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, String label, int color, int mouseX, int mouseY) {
        int fill = isInside(mouseX, mouseY, x, y, width, height) ? brighten(color) : color;
        graphics.fill(x, y, x + width, y + height, 0xFF1F1F1F);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
        graphics.drawCenteredString(this.font, Component.literal(label), x + width / 2, y + 6, 0xFFFFFF);
    }

    private void drawFittedCentered(GuiGraphics graphics, String text, int centerX, int y, int maxWidth, int color) {
        String fitted = text;
        if (this.font.width(fitted) > maxWidth) {
            fitted = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("..."))) + "...";
        }
        graphics.drawString(this.font, Component.literal(fitted), centerX - this.font.width(fitted) / 2, y, color, false);
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
