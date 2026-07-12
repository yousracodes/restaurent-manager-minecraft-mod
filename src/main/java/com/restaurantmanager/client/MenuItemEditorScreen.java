package com.restaurantmanager.client;

import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.SaveMenuItemPacket;
import com.restaurantmanager.restaurant.MenuCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class MenuItemEditorScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("restaurantmanager", "textures/gui/restaurant_menu_editor.png");
    private static final int TEXTURE_SIZE = 1254;
    private static final int LIGHT_TEXT_COLOR = 0xFFF2D2;
    private static final int TEXT_SHADOW_COLOR = 0x6B3F1D;
    private static final int DARK_TEXT_COLOR = 0x2B2118;

    private final InteractionHand hand;
    private final List<ResourceLocation> menuItems = new ArrayList<>();
    private MenuCategory category;
    private EditBox titleBox;
    private int leftPos;
    private int topPos;
    private int guiSize;
    private int listX;
    private int listY;
    private int gridX;
    private int gridY;
    private int saveX;
    private int saveY;
    private int closeX;
    private int closeY;
    private int categoryX;
    private int categoryY;
    private int categoryWidth;
    private int categoryHeight;
    private int categoryLeftX;
    private int categoryRightX;
    private int categoryArrowY;
    private int categoryArrowSize;
    private int titleCenterX;
    private int buttonWidth;
    private int buttonHeight;
    private int scrollOffset;
    private int visibleRows;
    private int rowHeight;
    private int itemNameWidth;

    public MenuItemEditorScreen(InteractionHand hand, ItemStack stack) {
        super(Component.literal("Restaurant Menu"));
        this.hand = hand;
        this.menuItems.addAll(RestaurantMenuItem.getMenuItems(stack));
        this.category = RestaurantMenuItem.getMenuCategory(stack);
    }

    @Override
    protected void init() {
        this.guiSize = Math.min(260, Math.min(this.width - 20, this.height - 90));
        this.leftPos = (this.width - this.guiSize) / 2;
        this.topPos = Math.max(8, (this.height - this.guiSize - 78) / 2);

        this.titleBox = new EditBox(this.font, textureX(200), textureY(158), textureWidth(860), 18, Component.literal("Menu Title"));
        ItemStack stack = Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getItemInHand(this.hand);
        this.titleBox.setValue(RestaurantMenuItem.getMenuTitle(stack));
        this.titleBox.setMaxLength(32);
        this.titleBox.setBordered(false);
        this.titleBox.setTextColor(LIGHT_TEXT_COLOR);
        this.titleBox.setTextColorUneditable(LIGHT_TEXT_COLOR);
        this.addRenderableWidget(this.titleBox);
        this.titleCenterX = textureX(627);

        this.listX = textureX(165);
        this.listY = textureY(420);
        this.saveX = textureX(196);
        this.saveY = textureY(1018);
        this.closeX = textureX(646);
        this.closeY = textureY(1018);
        this.categoryX = textureX(160);
        this.categoryY = textureY(294);
        this.categoryWidth = textureWidth(960);
        this.categoryHeight = textureHeight(66);
        this.categoryArrowSize = Math.max(18, textureWidth(54));
        this.categoryLeftX = this.categoryX + textureWidth(24);
        this.categoryRightX = this.categoryX + this.categoryWidth - this.categoryArrowSize - textureWidth(24);
        this.categoryArrowY = this.categoryY + (this.categoryHeight - this.categoryArrowSize) / 2;
        this.buttonWidth = textureWidth(420);
        this.buttonHeight = textureHeight(96);
        this.gridX = this.leftPos + (this.guiSize - 162) / 2;
        this.gridY = this.topPos + this.guiSize + 8;
        this.rowHeight = 17;
        this.visibleRows = 4;
        this.itemNameWidth = textureWidth(760);
        this.scrollOffset = Math.min(this.scrollOffset, getMaxScroll());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, this.guiSize, this.guiSize,
                0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        drawTitleShadow(graphics);
        drawMenuItems(graphics, mouseX, mouseY);
        drawCategory(graphics, mouseX, mouseY);
        drawButtonLabel(graphics, this.saveX, this.saveY, this.buttonWidth, this.buttonHeight, "Save", mouseX, mouseY);
        drawButtonLabel(graphics, this.closeX, this.closeY, this.buttonWidth, this.buttonHeight, "Close", mouseX, mouseY);
        centerTitleBox();
        this.titleBox.render(graphics, mouseX, mouseY, partialTick);
        drawInventory(graphics, mouseX, mouseY);
    }

    private void drawCategory(GuiGraphics graphics, int mouseX, int mouseY) {
        drawArrowButton(graphics, this.categoryLeftX, this.categoryArrowY, "<", mouseX, mouseY);
        drawArrowButton(graphics, this.categoryRightX, this.categoryArrowY, ">", mouseX, mouseY);
        drawCenteredDark(graphics, this.category.getDisplayName(),
                this.categoryX + this.categoryWidth / 2, this.categoryY + this.categoryHeight / 2 - 4);
    }

    private void drawMenuItems(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menuItems.isEmpty()) {
            drawLightString(graphics, "Select edible items from inventory below.", this.listX, this.listY);
            return;
        }
        for (int row = 0; row < this.visibleRows; row++) {
            int index = this.scrollOffset + row;
            if (index >= menuItems.size()) {
                break;
            }
            ResourceLocation id = menuItems.get(index);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
            int y = this.listY + row * this.rowHeight;
            if (isInside(mouseX, mouseY, this.listX - 2, y - 3, textureWidth(700), this.rowHeight)) {
                graphics.fill(this.listX - 2, y - 3, this.listX + textureWidth(700), y + this.rowHeight - 3, 0x44FFFFFF);
            }
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, this.listX, y - 5);
                drawWrappedItemName(graphics, stack.getHoverName(), this.listX + 20, y, this.itemNameWidth, row);
            }
        }
        if (menuItems.size() > this.visibleRows) {
            drawLightString(graphics,
                    (this.scrollOffset + 1) + "-" + Math.min(menuItems.size(), this.scrollOffset + this.visibleRows) + "/" + menuItems.size(),
                    this.listX + textureWidth(575), this.listY + this.visibleRows * this.rowHeight + 2);
        }
    }

    private void drawInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        drawLightString(graphics, "Inventory", this.gridX, this.gridY - 12);
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
        if (button == 1 && isInside(mouseX, mouseY, textureX(190), textureY(132), textureWidth(880), textureHeight(120))) {
            this.titleBox.setFocused(true);
            return true;
        }
        if (isInside(mouseX, mouseY, this.saveX, this.saveY, this.buttonWidth, this.buttonHeight)) {
            saveMenu();
            this.onClose();
            return true;
        }
        if (isInside(mouseX, mouseY, this.categoryLeftX, this.categoryArrowY, this.categoryArrowSize, this.categoryArrowSize)) {
            cycleCategory(-1);
            return true;
        }
        if (isInside(mouseX, mouseY, this.categoryRightX, this.categoryArrowY, this.categoryArrowSize, this.categoryArrowSize)) {
            cycleCategory(1);
            return true;
        }
        if (isInside(mouseX, mouseY, this.closeX, this.closeY, this.buttonWidth, this.buttonHeight)) {
            this.onClose();
            return true;
        }
        for (int row = 0; row < this.visibleRows; row++) {
            int index = this.scrollOffset + row;
            if (index >= menuItems.size()) {
                break;
            }
            int y = this.listY + row * this.rowHeight;
            if (isInside(mouseX, mouseY, this.listX - 2, y - 3, textureWidth(700), this.rowHeight)) {
                menuItems.remove(index);
                this.scrollOffset = Math.min(this.scrollOffset, getMaxScroll());
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
                        this.scrollOffset = getMaxScroll();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInside(mouseX, mouseY, this.listX - 8, this.listY - 8, textureWidth(750), textureHeight(470))) {
            if (delta < 0) {
                this.scrollOffset = Math.min(getMaxScroll(), this.scrollOffset + 1);
            } else if (delta > 0) {
                this.scrollOffset = Math.max(0, this.scrollOffset - 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int getMaxScroll() {
        return Math.max(0, this.menuItems.size() - this.visibleRows);
    }

    private void saveMenu() {
        ModNetwork.CHANNEL.sendToServer(new SaveMenuItemPacket(this.hand, this.titleBox.getValue(), this.category, this.menuItems));
    }

    private void cycleCategory(int direction) {
        MenuCategory[] categories = MenuCategory.values();
        int index = (this.category.ordinal() + direction) % categories.length;
        if (index < 0) {
            index += categories.length;
        }
        this.category = categories[index];
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, boolean edible) {
        if (x == this.gridX && y == this.gridY) {
            InventoryPanelRenderer.drawPanel(graphics, this.gridX, this.gridY);
        }
        InventoryPanelRenderer.drawHighlight(graphics, x, y, edible);
    }

    private void drawButtonLabel(GuiGraphics graphics, int x, int y, int width, int height, String label, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0x22FFFFFF);
        }
        drawCenteredLight(graphics, label, x + width / 2, y + height / 2 - 4);
    }

    private void drawArrowButton(GuiGraphics graphics, int x, int y, String label, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x, y, this.categoryArrowSize, this.categoryArrowSize)) {
            graphics.fill(x, y, x + this.categoryArrowSize, y + this.categoryArrowSize, 0x22FFFFFF);
        }
        drawCenteredDark(graphics, label, x + this.categoryArrowSize / 2, y + this.categoryArrowSize / 2 - 4);
    }

    private void drawWrappedItemName(GuiGraphics graphics, Component name, int x, int y, int width, int visibleRow) {
        int line = 0;
        for (net.minecraft.util.FormattedCharSequence sequence : this.font.split(name, width)) {
            if (line >= 2) {
                return;
            }
            if (line == 1 && visibleRow + 1 < this.visibleRows) {
                return;
            }
            drawLightString(graphics, sequence, x, y + line * 10);
            line++;
        }
    }

    private void drawTitleShadow(GuiGraphics graphics) {
        String title = this.titleBox.getValue();
        if (!title.isEmpty()) {
            graphics.drawString(this.font, Component.literal(title), this.titleBox.getX() + 1, this.titleBox.getY() + 1,
                    TEXT_SHADOW_COLOR, false);
        }
    }

    private void centerTitleBox() {
        String title = this.titleBox.getValue();
        int textWidth = this.font.width(title.isEmpty() ? " " : title);
        this.titleBox.setX(this.titleCenterX - textWidth / 2);
    }

    private void drawCenteredLight(GuiGraphics graphics, String text, int centerX, int y) {
        drawLightString(graphics, text, centerX - this.font.width(text) / 2, y);
    }

    private void drawCenteredDark(GuiGraphics graphics, String text, int centerX, int y) {
        graphics.drawString(this.font, Component.literal(text), centerX - this.font.width(text) / 2, y, DARK_TEXT_COLOR, false);
    }

    private void drawLightString(GuiGraphics graphics, String text, int x, int y) {
        drawLightString(graphics, Component.literal(text).getVisualOrderText(), x, y);
    }

    private void drawLightString(GuiGraphics graphics, net.minecraft.util.FormattedCharSequence text, int x, int y) {
        graphics.drawString(this.font, text, x + 1, y + 1, TEXT_SHADOW_COLOR, false);
        graphics.drawString(this.font, text, x, y, LIGHT_TEXT_COLOR, false);
    }

    private int textureX(int textureX) {
        return this.leftPos + textureX * this.guiSize / TEXTURE_SIZE;
    }

    private int textureY(int textureY) {
        return this.topPos + textureY * this.guiSize / TEXTURE_SIZE;
    }

    private int textureWidth(int textureWidth) {
        return textureWidth * this.guiSize / TEXTURE_SIZE;
    }

    private int textureHeight(int textureHeight) {
        return textureHeight * this.guiSize / TEXTURE_SIZE;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
