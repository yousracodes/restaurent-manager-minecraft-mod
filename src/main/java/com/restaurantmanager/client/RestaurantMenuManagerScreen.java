package com.restaurantmanager.client;

import com.restaurantmanager.network.ModNetwork;
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
    private static final int TEXTURE_WIDTH = 1792;
    private static final int TEXTURE_HEIGHT = 1024;
    private static final int[] CARD_X = {318, 918, 318, 918, 318, 918};
    private static final int[] CARD_Y = {286, 286, 455, 455, 623, 623};
    private static final int CARD_WIDTH = 575;
    private static final int CARD_HEIGHT = 132;

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
        this.guiWidth = Math.min(this.width - 20, (this.height - 20) * TEXTURE_WIDTH / TEXTURE_HEIGHT);
        this.guiHeight = this.guiWidth * TEXTURE_HEIGHT / TEXTURE_WIDTH;
        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, this.guiWidth, this.guiHeight,
                0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

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

            int textX = x + textureWidth(170);
            int textY = y + textureHeight(27);
            int textWidth = width - textureWidth(225);
            drawStringWithShadow(graphics, category.getDisplayName(), textX, textY, 0x3A220F);

            String title = this.menuTitles[category.ordinal()];
            int count = this.menuItemCounts[category.ordinal()];
            if (hasMenu(category)) {
                drawFittedString(graphics, title.isBlank() ? "Untitled menu" : title, textX, textY + 13, textWidth, 0x5A381A);
                drawStringWithShadow(graphics, count + " item(s)", textX, textY + 25, 0x7A5431);
                drawRemoveButton(graphics, category, mouseX, mouseY);
            } else {
                drawFittedString(graphics, "No menu selected", textX, textY + 17, textWidth, 0x7A5431);
            }
        }
    }

    private void drawRemoveButton(GuiGraphics graphics, MenuCategory category, int mouseX, int mouseY) {
        int index = category.ordinal();
        int x = textureX(CARD_X[index] + CARD_WIDTH - 54);
        int y = textureY(CARD_Y[index] + 24);
        int size = textureWidth(28);
        boolean hovered = isInside(mouseX, mouseY, x, y, size, size);
        graphics.fill(x, y, x + size, y + size, hovered ? 0xAA8E2F20 : 0x885A2A1F);
        graphics.drawCenteredString(this.font, Component.literal("x"), x + size / 2, y + Math.max(2, (size - 8) / 2), 0xFFF2D2);
    }

    private void drawBackButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = textureX(362);
        int y = textureY(817);
        int width = textureWidth(120);
        int height = textureHeight(54);
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            graphics.fill(x, y, x + width, y + height, 0x22FFFFFF);
        }
        graphics.drawCenteredString(this.font, Component.literal("Back"), x + width / 2, y + height / 2 - 4, 0xE7DBC6);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInside(mouseX, mouseY, textureX(362), textureY(817), textureWidth(120), textureHeight(54))) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        MenuCategory removeCategory = getRemoveCategory(mouseX, mouseY);
        if (removeCategory != null && hasMenu(removeCategory)) {
            clearMenu(removeCategory);
            ModNetwork.CHANNEL.sendToServer(new RemoveRestaurantMenuPacket(this.signPos, removeCategory));
            return true;
        }

        MenuCategory category = getCardCategory(mouseX, mouseY);
        if (category != null) {
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
            int x = textureX(CARD_X[i] + CARD_WIDTH - 54);
            int y = textureY(CARD_Y[i] + 24);
            int size = textureWidth(28);
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
        return this.leftPos + textureX * this.guiWidth / TEXTURE_WIDTH;
    }

    private int textureY(int textureY) {
        return this.topPos + textureY * this.guiHeight / TEXTURE_HEIGHT;
    }

    private int textureWidth(int textureWidth) {
        return textureWidth * this.guiWidth / TEXTURE_WIDTH;
    }

    private int textureHeight(int textureHeight) {
        return textureHeight * this.guiHeight / TEXTURE_HEIGHT;
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
