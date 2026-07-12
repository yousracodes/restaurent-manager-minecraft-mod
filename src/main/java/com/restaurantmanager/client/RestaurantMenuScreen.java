package com.restaurantmanager.client;

import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.UpdateRestaurantMenuPacket;
import com.restaurantmanager.restaurant.MenuCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class RestaurantMenuScreen extends Screen {
    private static final ResourceLocation MENU_TEXTURE = new ResourceLocation("restaurantmanager", "textures/gui/restaurant_menu.png");
    private static final int TEXTURE_SIZE = 1024;
    private static final int TEXT_COLOR = 0x2B2118;
    private static final int MUTED_TEXT_COLOR = 0x5A3E25;
    private static final int ERROR_TEXT_COLOR = 0x8A1F1F;

    private final BlockPos signPos;
    private final MenuCategory category;
    private final List<ResourceLocation> menuItems = new ArrayList<>();
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private int listX;
    private int listY;
    private int gridX;
    private int gridY;
    private int backX;
    private int backY;

    public RestaurantMenuScreen(BlockPos signPos, MenuCategory category, List<ResourceLocation> menuItems) {
        super(Component.literal("Restaurant Menu"));
        this.signPos = signPos;
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
        this.menuItems.addAll(menuItems);
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(360, this.width - 20);
        this.panelHeight = Math.min(250, this.height - 20);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        this.listX = this.leftPos + 92;
        this.listY = this.topPos + 42;
        this.gridX = this.leftPos + 24;
        this.gridY = this.topPos + 154;
        this.backX = this.leftPos + this.panelWidth - 82;
        this.backY = this.topPos + this.panelHeight - 28;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF2E2E2E);
        graphics.fill(this.leftPos + 3, this.topPos + 3, this.leftPos + this.panelWidth - 3, this.topPos + this.panelHeight - 3, 0xFFC7C5BF);
        graphics.fill(this.leftPos + 8, this.topPos + 8, this.leftPos + this.panelWidth - 8, this.topPos + this.panelHeight - 8, 0xFFB6B3AB);

        graphics.blit(MENU_TEXTURE, this.leftPos + 18, this.topPos + 18, 52, 52,
                0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        int textX = this.leftPos + 86;
        int headerWidth = this.leftPos + this.panelWidth - 16 - textX;
        drawFittedString(graphics, this.category.getDisplayName(), textX, this.topPos + 18, headerWidth, TEXT_COLOR);
        drawFittedString(graphics, "Add food below. Click list item to remove.", textX, this.topPos + 30,
                headerWidth, MUTED_TEXT_COLOR);

        drawMenuItems(graphics, mouseX, mouseY);
        drawInventory(graphics, mouseX, mouseY);
        drawButton(graphics, this.backX, this.backY, 64, 20, "Back", 0xFF3C6596, mouseX, mouseY);
    }

    private void drawMenuItems(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(this.listX - 6, this.listY - 6, this.leftPos + this.panelWidth - 20, this.listY + 92, 0xFFE8DFC9);
        int maxRows = 7;
        if (menuItems.isEmpty()) {
            drawFittedString(graphics, "No food on this menu yet.", this.listX, this.listY,
                    this.leftPos + this.panelWidth - 24 - this.listX, MUTED_TEXT_COLOR);
            return;
        }
        for (int i = 0; i < menuItems.size() && i < maxRows; i++) {
            ResourceLocation id = menuItems.get(i);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
            int y = this.listY + i * 12;
            if (isInside(mouseX, mouseY, this.listX - 2, y - 2, this.panelWidth - 120, 12)) {
                graphics.fill(this.listX - 2, y - 2, this.leftPos + this.panelWidth - 24, y + 10, 0x44FFFFFF);
            }
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, this.listX, y - 4);
                drawFittedString(graphics, stack.getHoverName().getString(), this.listX + 20, y,
                        this.leftPos + this.panelWidth - 24 - (this.listX + 20), TEXT_COLOR);
            } else {
                drawFittedString(graphics, id.toString(), this.listX + 20, y,
                        this.leftPos + this.panelWidth - 24 - (this.listX + 20), ERROR_TEXT_COLOR);
            }
        }
    }

    private void drawInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        drawFittedString(graphics, "Inventory", this.gridX, this.gridY - 13,
                this.leftPos + this.panelWidth - 20 - this.gridX, TEXT_COLOR);
        Player player = Minecraft.getInstance().player;
        ItemStack hovered = ItemStack.EMPTY;
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; slot++) {
            int x = InventoryPanelRenderer.slotX(this.gridX, slot);
            int y = InventoryPanelRenderer.slotY(this.gridY, slot);
            ItemStack stack = player.getInventory().getItem(InventoryPanelRenderer.inventoryIndexForDisplaySlot(slot));
            drawSlot(graphics, x, y, !stack.isEmpty() && stack.isEdible());
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
        if (isInside(mouseX, mouseY, this.backX, this.backY, 64, 20)) {
            this.onClose();
            return true;
        }

        for (int i = 0; i < menuItems.size() && i < 7; i++) {
            int y = this.listY + i * 12;
            if (isInside(mouseX, mouseY, this.listX - 2, y - 2, this.panelWidth - 120, 12)) {
                ResourceLocation id = menuItems.remove(i);
                ModNetwork.CHANNEL.sendToServer(new UpdateRestaurantMenuPacket(this.signPos, this.category, id, UpdateRestaurantMenuPacket.Action.REMOVE));
                return true;
            }
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (int slot = 0; slot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; slot++) {
                int x = InventoryPanelRenderer.slotX(this.gridX, slot);
                int y = InventoryPanelRenderer.slotY(this.gridY, slot);
                if (isInside(mouseX, mouseY, x, y, InventoryPanelRenderer.SLOT_SIZE, InventoryPanelRenderer.SLOT_SIZE)) {
                    ItemStack stack = player.getInventory().getItem(InventoryPanelRenderer.inventoryIndexForDisplaySlot(slot));
                    ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (id != null && stack.isEdible() && !menuItems.contains(id)) {
                        menuItems.add(id);
                        ModNetwork.CHANNEL.sendToServer(new UpdateRestaurantMenuPacket(this.signPos, this.category, id, UpdateRestaurantMenuPacket.Action.ADD));
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
        int fill = isInside(mouseX, mouseY, x, y, width, height) ? brighten(color) : color;
        graphics.fill(x, y, x + width, y + height, 0xFF1F1F1F);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
        graphics.drawCenteredString(this.font, Component.literal(label), x + width / 2, y + 6, 0xFFFFFF);
    }

    private void drawFittedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) {
            return;
        }

        String fitted = text;
        if (this.font.width(fitted) > maxWidth) {
            fitted = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("..."))) + "...";
        }
        graphics.drawString(this.font, Component.literal(fitted), x, y, color, false);
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
