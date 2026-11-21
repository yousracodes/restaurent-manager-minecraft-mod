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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RestaurantMenuManagerScreen extends Screen {
    private static final int PRIMARY_TEXT = 0xFF2B2118;
    private static final int SECONDARY_TEXT = 0xFF5A422B;
    private static final int EMPTY_TEXT = 0xFF6B543B;
    private static final int GUI_SOURCE_WIDTH = 2013;
    private static final int GUI_SOURCE_HEIGHT = 1000;
    private static final int MAX_GUI_WIDTH = 340;
    private static final int[] CARD_X = {137, 1034, 137, 1034, 137, 1034};
    private static final int[] CARD_Y = {210, 210, 405, 405, 600, 600};
    private static final int CARD_WIDTH = 860;
    private static final int CARD_HEIGHT = 170;
    private static final int CARD_TEXT_INSET = 205;
    private static final int CARD_TEXT_RIGHT_PADDING = 120;
    private static final int REMOVE_BUTTON_X_OFFSET = 744;
    private static final int REMOVE_BUTTON_SIZE = 64;
    private static final int BACK_BUTTON_X = 203;
    private static final int BACK_BUTTON_Y = 840;
    private static final int BACK_BUTTON_WIDTH = 179;
    private static final int BACK_BUTTON_HEIGHT = 78;

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
        this.guiWidth = Math.min(MAX_GUI_WIDTH,
                Math.min(this.width - 20, (this.height - 20) * GUI_SOURCE_WIDTH / GUI_SOURCE_HEIGHT));
        this.guiHeight = this.guiWidth * GUI_SOURCE_HEIGHT / GUI_SOURCE_WIDTH;
        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        CodeGuiTheme.drawScreenPanel(graphics, this.leftPos, this.topPos, this.guiWidth, this.guiHeight);
        int headerX = this.leftPos + textureWidth(120);
        int headerY = this.topPos + textureHeight(45);
        int headerWidth = this.guiWidth - textureWidth(240);
        int headerHeight = textureHeight(125);
        CodeGuiTheme.drawWoodHeader(graphics, headerX, headerY, headerWidth, headerHeight);
        drawCenteredCrispString(graphics, "MENU MANAGER", this.leftPos + this.guiWidth / 2,
                headerY + Math.max(5, (headerHeight - this.font.lineHeight) / 2), CodeGuiTheme.LIGHT_TEXT);

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
            CodeGuiTheme.drawInsetPanel(graphics, x, y, width, height);
            if (hovered) {
                graphics.fill(x + 2, y + 2, x + width - 2,
                        y + height - textureHeight(14), 0x24FFFFFF);
            }

            int slotSize = Math.max(18, Math.min(28, height - 6));
            int slotX = x + textureWidth(38);
            int slotY = y + (height - slotSize) / 2;
            CodeGuiTheme.drawSlot(graphics, slotX, slotY, slotSize);
            ItemStack icon = categoryIcon(category);
            graphics.renderItem(icon, slotX + (slotSize - 16) / 2, slotY + (slotSize - 16) / 2);

            int textX = x + textureWidth(CARD_TEXT_INSET);
            int textY = y + textureHeight(27);
            int textWidth = width - textureWidth(CARD_TEXT_INSET + CARD_TEXT_RIGHT_PADDING);
            drawCrispString(graphics, category.getDisplayName(), textX, textY, PRIMARY_TEXT);

            String title = this.menuTitles[category.ordinal()];
            if (hasMenu(category)) {
                drawFittedString(graphics, title.isBlank() ? "Untitled menu" : title,
                        textX, textY + 13, textWidth, SECONDARY_TEXT);
                drawRemoveButton(graphics, category, mouseX, mouseY);
            } else {
                drawFittedString(graphics, "No menu selected", textX, textY + 13, textWidth, EMPTY_TEXT);
            }
        }
    }

    private void drawRemoveButton(GuiGraphics graphics, MenuCategory category, int mouseX, int mouseY) {
        int index = category.ordinal();
        int x = textureX(CARD_X[index] + REMOVE_BUTTON_X_OFFSET);
        int size = textureWidth(REMOVE_BUTTON_SIZE);
        int y = removeButtonY(index, size);
        boolean hovered = isInside(mouseX, mouseY, x, y, size, size);
        CodeGuiTheme.drawButton(graphics, x, y, size, size, hovered, true);
        drawCenteredCrispString(graphics, "X", x + size / 2,
                y + Math.max(0, (size - this.font.lineHeight) / 2), CodeGuiTheme.LIGHT_TEXT);
    }

    private void drawBackButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = textureX(BACK_BUTTON_X);
        int y = textureY(BACK_BUTTON_Y);
        int width = textureWidth(BACK_BUTTON_WIDTH);
        int height = textureHeight(BACK_BUTTON_HEIGHT);
        CodeGuiTheme.drawButton(graphics, x, y, width, height,
                isInside(mouseX, mouseY, x, y, width, height), false);
        drawCenteredCrispString(graphics, "Back", x + width / 2,
                y + (height - this.font.lineHeight) / 2, CodeGuiTheme.LIGHT_TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInside(mouseX, mouseY, textureX(BACK_BUTTON_X), textureY(BACK_BUTTON_Y),
                textureWidth(BACK_BUTTON_WIDTH), textureHeight(BACK_BUTTON_HEIGHT))) {
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
            int size = textureWidth(REMOVE_BUTTON_SIZE);
            int y = removeButtonY(i, size);
            if (isInside(mouseX, mouseY, x, y, size, size)) {
                return categories[i];
            }
        }
        return null;
    }

    private int removeButtonY(int cardIndex, int buttonSize) {
        int cardY = textureY(CARD_Y[cardIndex]);
        int cardHeight = textureHeight(CARD_HEIGHT);
        return cardY + (cardHeight - buttonSize) / 2;
    }

    private void drawCrispString(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawString(this.font, Component.literal(text), x, y, color, false);
    }

    private void drawCenteredCrispString(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawString(this.font, Component.literal(text), centerX - this.font.width(text) / 2, y, color, false);
    }

    private void drawFittedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        String fitted = text;
        while (this.font.width(fitted) > maxWidth && fitted.length() > 3) {
            fitted = fitted.substring(0, fitted.length() - 4) + "...";
        }
        drawCrispString(graphics, fitted, x, y, color);
    }

    private ItemStack categoryIcon(MenuCategory category) {
        return new ItemStack(switch (category) {
            case MAIN_DISH -> Items.COOKED_BEEF;
            case DRINK -> Items.HONEY_BOTTLE;
            case DESSERT -> Items.CAKE;
            case BREAKFAST -> Items.BREAD;
            case APPETIZER -> Items.BAKED_POTATO;
            case SPECIAL -> Items.NETHER_STAR;
        });
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
