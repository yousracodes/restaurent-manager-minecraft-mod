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

import java.util.ArrayList;
import java.util.List;

public class RestaurantMenuSelectionScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 178;

    private final RestaurantMenuManagerScreen parent;
    private final BlockPos signPos;
    private final MenuCategory category;
    private final List<Integer> matchingSlots = new ArrayList<>();
    private int leftPos;
    private int topPos;
    private int gridX;
    private int gridY;
    private int backX;
    private int backY;

    public RestaurantMenuSelectionScreen(RestaurantMenuManagerScreen parent, BlockPos signPos, MenuCategory category) {
        super(Component.literal("Select Menu"));
        this.parent = parent;
        this.signPos = signPos;
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;
        this.gridX = this.leftPos + 22;
        this.gridY = this.topPos + 48;
        this.backX = this.leftPos + 95;
        this.backY = this.topPos + 150;
        refreshMatchingSlots();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, 0xFF2E2E2E);
        graphics.fill(this.leftPos + 3, this.topPos + 3, this.leftPos + PANEL_WIDTH - 3, this.topPos + PANEL_HEIGHT - 3, 0xFFC7C5BF);
        graphics.fill(this.leftPos + 8, this.topPos + 8, this.leftPos + PANEL_WIDTH - 8, this.topPos + PANEL_HEIGHT - 8, 0xFFB6B3AB);
        graphics.drawCenteredString(this.font, Component.literal("Select " + this.category.getDisplayName() + " Menu"),
                this.leftPos + PANEL_WIDTH / 2, this.topPos + 17, 0x2B2118);

        drawMenus(graphics, mouseX, mouseY);
        drawButton(graphics, this.backX, this.backY, 70, 18, "Back", 0xFF3C6596, mouseX, mouseY);
    }

    private void drawMenus(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.matchingSlots.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal("No matching menus in inventory"),
                    this.leftPos + PANEL_WIDTH / 2, this.topPos + 82, 0x5A381A);
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < this.matchingSlots.size() && i < 18; i++) {
            int slot = this.matchingSlots.get(i);
            int x = this.gridX + (i % 6) * 36;
            int y = this.gridY + (i / 6) * 30;
            ItemStack stack = player.getInventory().getItem(slot);
            boolean hoveredSlot = isInside(mouseX, mouseY, x, y, 28, 28);
            graphics.fill(x, y, x + 28, y + 28, hoveredSlot ? 0xFF6A8D46 : 0xFF6C5B4C);
            graphics.fill(x + 2, y + 2, x + 26, y + 26, 0xFFE0D4B7);
            graphics.renderItem(stack, x + 6, y + 6);
            graphics.renderItemDecorations(this.font, stack, x + 6, y + 6);
            if (hoveredSlot) {
                hovered = stack;
            }
        }
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInside(mouseX, mouseY, this.backX, this.backY, 70, 18)) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (int i = 0; i < this.matchingSlots.size() && i < 18; i++) {
                int x = this.gridX + (i % 6) * 36;
                int y = this.gridY + (i / 6) * 30;
                if (isInside(mouseX, mouseY, x, y, 28, 28)) {
                    int slot = this.matchingSlots.get(i);
                    ItemStack stack = player.getInventory().getItem(slot);
                    ModNetwork.CHANNEL.sendToServer(new ApplyRestaurantMenuItemPacket(this.signPos, slot, this.category));
                    parent.setMenuSummary(this.category, RestaurantMenuItem.getMenuTitle(stack), RestaurantMenuItem.getMenuItems(stack).size());
                    Minecraft.getInstance().setScreen(parent);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void refreshMatchingSlots() {
        this.matchingSlots.clear();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.RESTAURANT_MENU.get()) && RestaurantMenuItem.getMenuCategory(stack) == this.category) {
                this.matchingSlots.add(slot);
            }
        }
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, String label, int color, int mouseX, int mouseY) {
        int fill = isInside(mouseX, mouseY, x, y, width, height) ? brighten(color) : color;
        graphics.fill(x, y, x + width, y + height, 0xFF1F1F1F);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
        graphics.drawCenteredString(this.font, Component.literal(label), x + width / 2, y + 5, 0xFFFFFF);
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
