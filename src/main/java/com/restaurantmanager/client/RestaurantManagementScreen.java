package com.restaurantmanager.client;

import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.RenameRestaurantPacket;
import com.restaurantmanager.network.packet.ToggleRestaurantOpenPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class RestaurantManagementScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("restaurantmanager", "textures/gui/restaurant_management.png");
    private static final int TEXTURE_SIZE = 1254;

    private final BlockPos signPos;
    private boolean open;
    private final String ownerName;
    private final int reputation;
    private final int customersServed;
    private final int ordersCompleted;
    private final int ordersFailed;
    private final int customersLost;
    private final int moneyEarned;
    private String restaurantName;
    private String menuTitle;
    private int menuItemCount;
    private EditBox nameBox;
    private int leftPos;
    private int topPos;
    private int guiSize;
    private int menuX;
    private int toggleX;
    private int closeX;
    private int buttonY;
    private int buttonWidth;
    private int buttonHeight;

    public RestaurantManagementScreen(BlockPos signPos, boolean open) {
        this(signPos, open, "Restaurant", "", 0, 0, 0, 0, 0, 0, "", 0);
    }

    public RestaurantManagementScreen(BlockPos signPos, boolean open, String name, String ownerName, int reputation,
                                      int customersServed, int ordersCompleted, int ordersFailed,
                                      int customersLost, int moneyEarned, String menuTitle, int menuItemCount) {
        super(Component.literal("Restaurant"));
        this.signPos = signPos;
        this.open = open;
        this.ownerName = ownerName == null || ownerName.isBlank() ? "Unknown" : ownerName;
        this.reputation = reputation;
        this.customersServed = customersServed;
        this.ordersCompleted = ordersCompleted;
        this.ordersFailed = ordersFailed;
        this.customersLost = customersLost;
        this.moneyEarned = moneyEarned;
        this.restaurantName = name == null || name.isBlank() ? "Restaurant" : name;
        this.menuTitle = menuTitle == null ? "" : menuTitle;
        this.menuItemCount = menuItemCount;
    }

    public RestaurantManagementScreen withSelectedMenu(ItemStack menuStack) {
        this.menuTitle = RestaurantMenuItem.getMenuTitle(menuStack);
        this.menuItemCount = RestaurantMenuItem.getMenuItems(menuStack).size();
        return this;
    }

    @Override
    protected void init() {
        this.guiSize = Math.min(360, Math.min(this.width - 20, this.height - 20));
        this.leftPos = (this.width - this.guiSize) / 2;
        this.topPos = (this.height - this.guiSize) / 2;

        this.nameBox = new EditBox(this.font, textureX(245), textureY(90), textureWidth(755), 18, Component.literal("Restaurant Name"));
        this.nameBox.setValue(this.restaurantName);
        this.nameBox.setMaxLength(32);
        this.nameBox.setBordered(false);
        this.nameBox.setTextColor(0xFFF2D2);
        this.addRenderableWidget(this.nameBox);

        this.menuX = textureX(120);
        this.toggleX = textureX(460);
        this.closeX = textureX(800);
        this.buttonY = textureY(895);
        this.buttonWidth = textureWidth(330);
        this.buttonHeight = textureHeight(145);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, this.guiSize, this.guiSize,
                0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        this.nameBox.render(graphics, mouseX, mouseY, partialTick);
        drawMenuPreview(graphics);
        drawStats(graphics);
        drawButtonLabel(graphics, this.menuX, this.buttonY, this.buttonWidth, this.buttonHeight, "Menu", mouseX, mouseY);
        drawButtonLabel(graphics, this.toggleX, this.buttonY, this.buttonWidth, this.buttonHeight,
                this.open ? "Close" : "Open", mouseX, mouseY);
        drawButtonLabel(graphics, this.closeX, this.buttonY, this.buttonWidth, this.buttonHeight, "Close", mouseX, mouseY);
    }

    private void drawMenuPreview(GuiGraphics graphics) {
        String label;
        if (menuTitle.isBlank() && menuItemCount <= 0) {
            label = "No menu selected";
        } else {
            label = (menuTitle.isBlank() ? "Restaurant Menu" : menuTitle) + " (" + menuItemCount + ")";
        }
        drawCenteredLight(graphics, label, textureX(627), textureY(760));
    }

    private void drawStats(GuiGraphics graphics) {
        int centerX = textureX(627);
        int y = textureY(465);
        drawCenteredLight(graphics, "Owner: " + ownerName, centerX, y);
        drawCenteredLight(graphics, "Served: " + customersServed + "   Rep: " + reputation, centerX, y + 12);
        drawCenteredLight(graphics, "Orders: " + ordersCompleted + " OK / " + ordersFailed + " Failed", centerX, y + 24);
        drawCenteredLight(graphics, "Lost: " + customersLost + "   Earned: " + moneyEarned, centerX, y + 36);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && isInside(mouseX, mouseY, textureX(235), textureY(78), textureWidth(775), textureHeight(95))) {
            this.nameBox.setFocused(true);
            return true;
        }
        if (isInside(mouseX, mouseY, this.menuX, this.buttonY, this.buttonWidth, this.buttonHeight)) {
            saveName();
            this.minecraft.setScreen(new RestaurantMenuItemSelectionScreen(this));
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
        drawCenteredLight(graphics, label, x + width / 2, y + height / 2 - 4);
    }

    private void drawCenteredLight(GuiGraphics graphics, String text, int centerX, int y) {
        graphics.drawCenteredString(this.font, Component.literal(text), centerX + 1, y + 1, 0x6B3F1D);
        graphics.drawCenteredString(this.font, Component.literal(text), centerX, y, 0xFFF2D2);
    }

    public BlockPos getSignPos() {
        return signPos;
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
