package com.restaurantmanager.client;

import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.RequestRestaurantMenuPacket;
import com.restaurantmanager.network.packet.RemoveRestaurantMenuPacket;
import com.restaurantmanager.restaurant.MenuCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RestaurantMenuManagerScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("restaurantmanager", "textures/gui/restaurant_menu_manager.png");
    private static final int TEXTURE_WIDTH = 2680;
    private static final int TEXTURE_HEIGHT = 1480;
    private static final int GUI_SOURCE_X = 338;
    private static final int GUI_SOURCE_Y = 79;
    private static final int GUI_SOURCE_WIDTH = 2013;
    private static final int GUI_SOURCE_HEIGHT = 1304;
    private static final int[] CARD_X = {137, 1034, 137, 1034, 137, 1034};
    private static final int[] CARD_Y = {334, 334, 578, 578, 822, 822};
    private static final int CARD_WIDTH = 860;
    private static final int CARD_HEIGHT = 191;
    private static final int CARD_TEXT_INSET = 245;
    private static final int CARD_TEXT_RIGHT_PADDING = 78;
    private static final int REMOVE_BUTTON_X_OFFSET = 744;
    private static final int REMOVE_BUTTON_Y_OFFSET = 22;
    private static final int REMOVE_BUTTON_SIZE = 64;

    private final RestaurantManagementScreen parent;
    private final BlockPos signPos;
    private final String[] menuTitles;
    private final int[] menuItemCounts;
    private int leftPos;
    private int topPos;
    private int guiWidth;
    private int guiHeight;

    public RestaurantMenuManagerScreen(RestaurantManagementScreen parent, BlockPos signPos, String[] menuTitles, int[] menuItemCounts) {
        super(Component.literal("Menu Manager"));
        this.parent = parent;
        this.signPos = signPos;
        this.menuTitles = normalizeTitles(menuTitles);
        this.menuItemCounts = normalizeCounts(menuItemCounts);
    }

    @Override
    protected void init() {
        this.guiWidth = Math.min(this.width - 20, (this.height - 20) * GUI_SOURCE_WIDTH / GUI_SOURCE_HEIGHT);
        this.guiHeight = this.guiWidth * GUI_SOURCE_HEIGHT / GUI_SOURCE_WIDTH;
        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, this.guiWidth, this.guiHeight,
                GUI_SOURCE_X, GUI_SOURCE_Y, GUI_SOURCE_WIDTH, GUI_SOURCE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        drawCards(graphics, mouseX, mouseY);
        drawBackButton(graphics, mouseX, mouseY);
    }

    private void drawCards(GuiGraphics graphics, int mouseX, int mouseY) {
        MenuCategory[] categories = MenuCategory.values();
        for (int i = 0; i < categories.length; i++) {
            MenuCategory category = categories[i];
            int x = textureX(CARD_X[i]);
            int y = textureY(CARD_Y[i]);
            int width = textureWidth(CARD_WIDTH);
            int height = textureHeight(CARD_HEIGHT);
            boolean hovered = isInside(mouseX, mouseY, x, y, width, height);
            if (hovered) {
                graphics.fill(x + textureWidth(150), y + textureHeight(12), x + width - textureWidth(14),
                        y + height - textureHeight(14), 0x24FFFFFF);
            }

            int textX = x + textureWidth(CARD_TEXT_INSET);
            int textY = y + textureHeight(27);
            int textWidth = width - textureWidth(CARD_TEXT_INSET + CARD_TEXT_RIGHT_PADDING);
            drawStringWithShadow(graphics, category.getDisplayName(), textX, textY, 0x3A220F);

            String title = this.menuTitles[category.ordinal()];
            if (hasMenu(category)) {
                drawFittedString(graphics, title.isBlank() ? "Untitled menu" : title, textX, textY + 13, textWidth, 0x5A381A);
                drawRemoveButton(graphics, category, mouseX, mouseY);
            } else {
                drawFittedString(graphics, "No menu selected", textX, textY + 17, textWidth, 0x7A5431);
            }
        }
    }

    private void drawRemoveButton(GuiGraphics graphics, MenuCategory category, int mouseX, int mouseY) {
        int index = category.ordinal();
        int x = textureX(CARD_X[index] + REMOVE_BUTTON_X_OFFSET);
        int y = textureY(CARD_Y[index] + REMOVE_BUTTON_Y_OFFSET);
        int size = textureWidth(REMOVE_BUTTON_SIZE);
        boolean hovered = isInside(mouseX, mouseY, x, y, size, size);
        graphics.fill(x, y, x + size, y + size, hovered ? 0xAA8E2F20 : 0x885A2A1F);
        graphics.drawCenteredString(this.font, Component.literal("x"), x + size / 2, y + Math.max(2, (size - 8) / 2), 0xFFF2D2);
    }

    private void drawBackButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = textureX(203);
        int y = textureY(1102);
        int width = textureWidth(179);
        int height = textureHeight(78);
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            graphics.fill(x, y, x + width, y + height, 0x22FFFFFF);
        }
        graphics.drawCenteredString(this.font, Component.literal("Back"), x + width / 2, y + height / 2 - 4, 0xE7DBC6);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInside(mouseX, mouseY, textureX(203), textureY(1102), textureWidth(179), textureHeight(78))) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        MenuCategory removeCategory = getRemoveCategory(mouseX, mouseY);
        if (button == 0 && removeCategory != null && hasMenu(removeCategory)) {
            clearMenu(removeCategory);
            ModNetwork.CHANNEL.sendToServer(new RemoveRestaurantMenuPacket(this.signPos, removeCategory));
            return true;
        }

        MenuCategory category = getCardCategory(mouseX, mouseY);
        if (category != null) {
            if (button == 1 && hasMenu(category)) {
                ModNetwork.CHANNEL.sendToServer(new RequestRestaurantMenuPacket(this.signPos, category));
                return true;
            }
            Minecraft.getInstance().setScreen(new RestaurantMenuSelectionScreen(this, this.signPos, category));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void setMenuSummary(MenuCategory category, String title, int itemCount) {
        if (category == null) {
            return;
        }
        this.menuTitles[category.ordinal()] = title == null ? "" : title;
        this.menuItemCounts[category.ordinal()] = Math.max(0, itemCount);
        this.parent.setMenuSummary(category, title, itemCount);
    }

    private void clearMenu(MenuCategory category) {
        setMenuSummary(category, "", 0);
    }

    private boolean hasMenu(MenuCategory category) {
        return !this.menuTitles[category.ordinal()].isBlank() || this.menuItemCounts[category.ordinal()] > 0;
    }

    private MenuCategory getCardCategory(double mouseX, double mouseY) {
        MenuCategory[] categories = MenuCategory.values();
        for (int i = 0; i < categories.length; i++) {
            if (isInside(mouseX, mouseY, textureX(CARD_X[i]), textureY(CARD_Y[i]), textureWidth(CARD_WIDTH), textureHeight(CARD_HEIGHT))) {
                return categories[i];
            }
        }
        return null;
    }

    private MenuCategory getRemoveCategory(double mouseX, double mouseY) {
        MenuCategory[] categories = MenuCategory.values();
        for (int i = 0; i < categories.length; i++) {
            int x = textureX(CARD_X[i] + REMOVE_BUTTON_X_OFFSET);
            int y = textureY(CARD_Y[i] + REMOVE_BUTTON_Y_OFFSET);
            int size = textureWidth(REMOVE_BUTTON_SIZE);
            if (isInside(mouseX, mouseY, x, y, size, size)) {
                return categories[i];
            }
        }
        return null;
    }

    private void drawStringWithShadow(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawString(this.font, Component.literal(text), x + 1, y + 1, 0x9D7A4C, false);
        graphics.drawString(this.font, Component.literal(text), x, y, color, false);
    }

    private void drawFittedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        String fitted = text;
        while (this.font.width(fitted) > maxWidth && fitted.length() > 3) {
            fitted = fitted.substring(0, fitted.length() - 4) + "...";
        }
        drawStringWithShadow(graphics, fitted, x, y, color);
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

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
