package com.restaurantmanager.client;

import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.RenameRestaurantPacket;
import com.restaurantmanager.network.packet.ToggleRestaurantOpenPacket;
import com.restaurantmanager.restaurant.MenuCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RestaurantManagementScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("restaurantmanager", "textures/gui/restaurant_management.png");
    private static final int TEXTURE_WIDTH = 1699;
    private static final int TEXTURE_HEIGHT = 1651;
    private static final int GUI_SOURCE_X = 232;
    private static final int GUI_SOURCE_Y = 312;
    private static final int GUI_SOURCE_WIDTH = 1244;
    private static final int GUI_SOURCE_HEIGHT = 1164;
    private static final int TITLE_CENTER_TEXTURE_X = 622;
    private static final int TITLE_TEXTURE_Y = 72;
    private static final int STATS_CENTER_TEXTURE_X = 622;
    private static final int STATS_START_TEXTURE_Y = 428;
    private static final int NAME_TEXT_COLOR = 0x5A2B13;
    private static final int PLATE_TEXT_COLOR = 0x5A2B13;
    private static final int LIGHT_OUTLINE_COLOR = 0xFFE8B4;
    private static final int BUTTON_TEXT_COLOR = 0xFFF6D5;
    private static final int BUTTON_OUTLINE_COLOR = 0x4B2412;

    private final BlockPos signPos;
    private boolean open;
    private final String ownerName;
    private final int reputation;
    private final int starRating;
    private final int customersServed;
    private final int ordersCompleted;
    private final int ordersFailed;
    private final int customersLost;
    private final int moneyEarned;
    private final String[] menuTitles;
    private final int[] menuItemCounts;
    private String restaurantName;
    private EditBox nameBox;
    private int leftPos;
    private int topPos;
    private int guiWidth;
    private int guiHeight;
    private int menuX;
    private int toggleX;
    private int closeX;
    private int buttonY;
    private int buttonWidth;
    private int buttonHeight;

    public RestaurantManagementScreen(BlockPos signPos, boolean open) {
        this(signPos, open, "Restaurant", "", 0, 1, 0, 0, 0, 0, 0, "", 0);
    }

    public RestaurantManagementScreen(BlockPos signPos, boolean open, String name, String ownerName, int reputation, int starRating,
                                      int customersServed, int ordersCompleted, int ordersFailed,
                                      int customersLost, int moneyEarned, String menuTitle, int menuItemCount) {
        this(signPos, open, name, ownerName, reputation, starRating, customersServed, ordersCompleted, ordersFailed,
                customersLost, moneyEarned, menuTitle, menuItemCount, null, null);
    }

    public RestaurantManagementScreen(BlockPos signPos, boolean open, String name, String ownerName, int reputation, int starRating,
                                      int customersServed, int ordersCompleted, int ordersFailed,
                                      int customersLost, int moneyEarned, String menuTitle, int menuItemCount,
                                      String[] menuTitles, int[] menuItemCounts) {
        super(Component.literal("Restaurant"));
        this.signPos = signPos;
        this.open = open;
        this.ownerName = ownerName == null || ownerName.isBlank() ? "Unknown" : ownerName;
        this.reputation = reputation;
        this.starRating = starRating;
        this.customersServed = customersServed;
        this.ordersCompleted = ordersCompleted;
        this.ordersFailed = ordersFailed;
        this.customersLost = customersLost;
        this.moneyEarned = moneyEarned;
        this.menuTitles = normalizeTitles(menuTitles);
        this.menuItemCounts = normalizeCounts(menuItemCounts);
        this.restaurantName = name == null || name.isBlank() ? "Restaurant" : name;
    }

    @Override
    protected void init() {
        this.guiWidth = Math.min(500, this.width - 20);
        this.guiHeight = this.guiWidth * GUI_SOURCE_HEIGHT / GUI_SOURCE_WIDTH;
        if (this.guiHeight > this.height - 20) {
            this.guiHeight = this.height - 20;
            this.guiWidth = this.guiHeight * GUI_SOURCE_WIDTH / GUI_SOURCE_HEIGHT;
        }
        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;

        this.nameBox = new EditBox(this.font, textureX(205), textureY(TITLE_TEXTURE_Y), textureWidth(834), 18, Component.literal("Restaurant Name"));
        this.nameBox.setValue(this.restaurantName);
        this.nameBox.setMaxLength(32);
        this.nameBox.setBordered(false);
        this.nameBox.setTextColor(NAME_TEXT_COLOR);
        this.addRenderableWidget(this.nameBox);

        this.menuX = textureX(112);
        this.toggleX = textureX(456);
        this.closeX = textureX(800);
        this.buttonY = textureY(884);
        this.buttonWidth = textureWidth(320);
        this.buttonHeight = textureHeight(124);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, this.guiWidth, this.guiHeight,
                GUI_SOURCE_X, GUI_SOURCE_Y, GUI_SOURCE_WIDTH, GUI_SOURCE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        drawCenteredOutlined(graphics, this.nameBox.getValue().isBlank() ? "Restaurant" : this.nameBox.getValue(),
                textureX(TITLE_CENTER_TEXTURE_X), textureY(TITLE_TEXTURE_Y), NAME_TEXT_COLOR, LIGHT_OUTLINE_COLOR);
        drawStats(graphics);
        drawButtonLabel(graphics, this.menuX, this.buttonY, this.buttonWidth, this.buttonHeight, "Menus", mouseX, mouseY);
        drawButtonLabel(graphics, this.toggleX, this.buttonY, this.buttonWidth, this.buttonHeight,
                this.open ? "Close" : "Open", mouseX, mouseY);
        drawButtonLabel(graphics, this.closeX, this.buttonY, this.buttonWidth, this.buttonHeight, "Close", mouseX, mouseY);
    }

    private void drawStats(GuiGraphics graphics) {
        int centerX = textureX(STATS_CENTER_TEXTURE_X);
        int y = textureY(STATS_START_TEXTURE_Y);
        drawCenteredOutlined(graphics, "Owner: " + ownerName, centerX, y, PLATE_TEXT_COLOR, LIGHT_OUTLINE_COLOR);
        drawCenteredOutlined(graphics, "Stars: " + starRating + "/5   Rep: " + reputation, centerX, y + 12, PLATE_TEXT_COLOR, LIGHT_OUTLINE_COLOR);
        drawCenteredOutlined(graphics, "Served: " + customersServed + "   Earned: " + moneyEarned, centerX, y + 24, PLATE_TEXT_COLOR, LIGHT_OUTLINE_COLOR);
        drawCenteredOutlined(graphics, "Orders: " + ordersCompleted + " OK / " + ordersFailed + " Failed", centerX, y + 36, PLATE_TEXT_COLOR, LIGHT_OUTLINE_COLOR);
        drawCenteredOutlined(graphics, "Lost: " + customersLost, centerX, y + 48, PLATE_TEXT_COLOR, LIGHT_OUTLINE_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && isInside(mouseX, mouseY, textureX(190), textureY(10), textureWidth(864), textureHeight(160))) {
            this.nameBox.setFocused(true);
            return true;
        }
        if (isInside(mouseX, mouseY, this.menuX, this.buttonY, this.buttonWidth, this.buttonHeight)) {
            saveName();
            this.minecraft.setScreen(new RestaurantMenuManagerScreen(this, this.signPos, this.menuTitles, this.menuItemCounts));
            return true;
        }
        if (isInside(mouseX, mouseY, this.toggleX, this.buttonY, this.buttonWidth, this.buttonHeight)) {
            saveName();
            this.open = !this.open;
            ModNetwork.CHANNEL.sendToServer(new ToggleRestaurantOpenPacket(this.signPos, this.open));
            return true;
        }
        if (isInside(mouseX, mouseY, this.closeX, this.buttonY, this.buttonWidth, this.buttonHeight)) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        saveName();
        super.onClose();
    }

    private void saveName() {
        this.restaurantName = this.nameBox.getValue();
        ModNetwork.CHANNEL.sendToServer(new RenameRestaurantPacket(this.signPos, this.restaurantName));
    }

    private void drawButtonLabel(GuiGraphics graphics, int x, int y, int width, int height, String label, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            graphics.fill(x + 6, y + 6, x + width - 6, y + height - 6, 0x22FFFFFF);
        }
        drawCenteredOutlined(graphics, label, x + width / 2, y + height / 2 - 4, BUTTON_TEXT_COLOR, BUTTON_OUTLINE_COLOR);
    }

    private void drawCenteredOutlined(GuiGraphics graphics, String text, int centerX, int y, int textColor, int outlineColor) {
        Component component = Component.literal(text);
        graphics.drawCenteredString(this.font, component, centerX - 1, y, outlineColor);
        graphics.drawCenteredString(this.font, component, centerX + 1, y, outlineColor);
        graphics.drawCenteredString(this.font, component, centerX, y - 1, outlineColor);
        graphics.drawCenteredString(this.font, component, centerX, y + 1, outlineColor);
        graphics.drawCenteredString(this.font, component, centerX + 1, y + 1, outlineColor);
        graphics.drawCenteredString(this.font, component, centerX, y, textColor);
    }

    public BlockPos getSignPos() {
        return signPos;
    }

    public void setMenuSummary(MenuCategory category, String title, int itemCount) {
        if (category == null) {
            return;
        }
        this.menuTitles[category.ordinal()] = title == null ? "" : title;
        this.menuItemCounts[category.ordinal()] = Math.max(0, itemCount);
    }

    public String[] copyMenuTitles() {
        return this.menuTitles.clone();
    }

    public int[] copyMenuItemCounts() {
        return this.menuItemCounts.clone();
    }

    private static String[] normalizeTitles(String[] titles) {
        String[] normalized = new String[MenuCategory.values().length];
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = titles != null && i < titles.length && titles[i] != null ? titles[i] : "";
        }
        return normalized;
    }

    private static int[] normalizeCounts(int[] counts) {
        int[] normalized = new int[MenuCategory.values().length];
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = counts != null && i < counts.length ? counts[i] : 0;
        }
        return normalized;
    }

    private int textureX(int textureX) {
        return this.leftPos + textureX * this.guiWidth / GUI_SOURCE_WIDTH;
    }

    private int textureY(int textureY) {
        return this.topPos + textureY * this.guiHeight / GUI_SOURCE_HEIGHT;
    }

    private int textureWidth(int textureWidth) {
        return textureWidth * this.guiWidth / GUI_SOURCE_WIDTH;
    }

    private int textureHeight(int textureHeight) {
        return textureHeight * this.guiHeight / GUI_SOURCE_HEIGHT;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
