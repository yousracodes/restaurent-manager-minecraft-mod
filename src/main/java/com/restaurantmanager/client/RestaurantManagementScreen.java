package com.restaurantmanager.client;

import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.RenameRestaurantPacket;
import com.restaurantmanager.network.packet.ToggleRestaurantOpenPacket;
import com.restaurantmanager.restaurant.MenuCategory;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RestaurantManagementScreen extends Screen {
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 238;
    private static final int MARGIN = 9;
    private static final int CARD_GAP = 4;
    private static final int CARD_HEIGHT = 29;
    private static final int FIRST_CARD_Y = 69;

    private static final int OUTLINE = 0xFF000000;
    private static final int PANEL_SHADOW = 0xFF9C815D;
    private static final int PANEL_DARK_SHADOW = 0xFF5C4A36;
    private static final int MUTED_TEXT = 0xFF5F5F5F;
    private static final int VALUE_TEXT = 0xFF202020;
    private static final int WOOD = 0xFF9A5B2E;
    private static final int WOOD_LIGHT = 0xFFC17A3D;
    private static final int WOOD_DARK = 0xFF4A2817;
    private static final int OPEN_GREEN = 0xFF39753B;
    private static final int CLOSED_RED = 0xFF8B3434;

    private static final ResourceLocation REPUTATION_ICON = icon("heart");
    private static final ResourceLocation EARNINGS_ICON = icon("coin");
    private static final ResourceLocation COMPLETED_ICON = icon("completed");
    private static final ResourceLocation FAILED_ICON = icon("failed");

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
    private CrispEditBox nameBox;
    private int leftPos;
    private int topPos;

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
        super(Component.literal("Restaurant Management"));
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
        if (menuTitles == null && this.menuTitles.length > 0 && menuTitle != null && !menuTitle.isBlank()) {
            this.menuTitles[0] = menuTitle;
        }
        if (menuItemCounts == null && this.menuItemCounts.length > 0) {
            this.menuItemCounts[0] = Math.max(0, menuItemCount);
        }
        this.restaurantName = name == null || name.isBlank() ? "Restaurant" : name;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        this.nameBox = new CrispEditBox(this.font, this.leftPos + 46, this.topPos + 23, 184, 21,
                Component.literal("Restaurant Name"), CodeGuiTheme.TEXT);
        this.nameBox.setValue(this.restaurantName);
        this.nameBox.setMaxLength(32);
        this.nameBox.setBordered(false);
        this.nameBox.setFocused(false);
        this.addRenderableWidget(this.nameBox);

        int buttonY = this.topPos + 209;
        int buttonWidth = 97;
        this.addRenderableWidget(new ThemedButton(this.leftPos + MARGIN, buttonY, buttonWidth, 20,
                Component.literal("Menus"), button -> {
                    saveName();
                    this.minecraft.setScreen(new RestaurantMenuManagerScreen(this, this.signPos, this.menuTitles, this.menuItemCounts));
                }));

        this.addRenderableWidget(new ThemedButton(this.leftPos + MARGIN + buttonWidth + CARD_GAP, buttonY,
                buttonWidth, 20, toggleLabel(), button -> {
                    saveName();
                    this.open = !this.open;
                    button.setMessage(toggleLabel());
                    ModNetwork.CHANNEL.sendToServer(new ToggleRestaurantOpenPacket(this.signPos, this.open));
                }));

        this.addRenderableWidget(new ThemedButton(this.leftPos + MARGIN + (buttonWidth + CARD_GAP) * 2,
                buttonY, 100, 20, Component.literal("Done"), button -> this.onClose()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        drawRaisedPanel(graphics, this.leftPos, this.topPos, GUI_WIDTH, GUI_HEIGHT);
        drawHeader(graphics);
        drawSectionTitle(graphics, "OVERVIEW", this.topPos + 57);
        drawStats(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphics graphics) {
        int x = this.leftPos + MARGIN;
        int y = this.topPos + MARGIN;
        int width = GUI_WIDTH - MARGIN * 2;
        drawWoodPanel(graphics, x, y, width, 40);

        drawItemSlot(graphics, x + 6, y + 9);
        graphics.renderItem(new ItemStack(ModItems.RESTAURANT_SIGN.get()), x + 9, y + 12);

        graphics.drawString(this.font, Component.literal("RESTAURANT"), x + 35, y + 5, 0xFFFFD99A, false);
        CodeGuiTheme.drawInsetPanel(graphics, this.leftPos + 42, this.topPos + 21, 192, 25);
        drawStatusBadge(graphics, x + width - 72, y + 14, 66, 21);
    }

    private void drawSectionTitle(GuiGraphics graphics, String title, int y) {
        int centerX = this.leftPos + GUI_WIDTH / 2;
        int titleWidth = this.font.width(title);
        int left = this.leftPos + MARGIN;
        int right = this.leftPos + GUI_WIDTH - MARGIN;
        int lineY = y + 4;
        graphics.fill(left, lineY, centerX - titleWidth / 2 - 7, lineY + 1, PANEL_SHADOW);
        graphics.fill(centerX + titleWidth / 2 + 7, lineY, right, lineY + 1, PANEL_SHADOW);
        graphics.drawString(this.font, Component.literal(title), centerX - titleWidth / 2, y, MUTED_TEXT, false);
    }

    private void drawStats(GuiGraphics graphics) {
        int contentX = this.leftPos + MARGIN;
        int contentWidth = GUI_WIDTH - MARGIN * 2;
        int halfWidth = (contentWidth - CARD_GAP) / 2;
        int row1 = this.topPos + FIRST_CARD_Y;
        int row2 = row1 + CARD_HEIGHT + CARD_GAP;
        int row3 = row2 + CARD_HEIGHT + CARD_GAP;
        int row4 = row3 + CARD_HEIGHT + CARD_GAP;

        drawStatCard(graphics, contentX, row1, halfWidth, CARD_HEIGHT,
                new ItemStack(ModItems.RESTAURANT_MENU.get()), "Menus", menuSummary());
        drawStatCard(graphics, contentX + halfWidth + CARD_GAP, row1, halfWidth, CARD_HEIGHT,
                new ItemStack(Items.PLAYER_HEAD), "Owner", this.ownerName);

        drawStatCard(graphics, contentX, row2, halfWidth, CARD_HEIGHT,
                new ItemStack(Items.NETHER_STAR), "Star rating", this.starRating + " / 5");
        drawReputationCard(graphics, contentX + halfWidth + CARD_GAP, row2, halfWidth, CARD_HEIGHT);

        drawStatCard(graphics, contentX, row3, halfWidth, CARD_HEIGHT,
                new ItemStack(Items.NAME_TAG), "Guests served", Integer.toString(this.customersServed));
        drawIconStatCard(graphics, contentX + halfWidth + CARD_GAP, row3, halfWidth, CARD_HEIGHT,
                EARNINGS_ICON, "Money earned", Integer.toString(this.moneyEarned));

        int thirdWidth = (contentWidth - CARD_GAP * 2) / 3;
        drawCompactIconStatCard(graphics, contentX, row4, thirdWidth, CARD_HEIGHT,
                COMPLETED_ICON, "Completed", this.ordersCompleted);
        drawCompactIconStatCard(graphics, contentX + thirdWidth + CARD_GAP, row4, thirdWidth, CARD_HEIGHT,
                FAILED_ICON, "Failed", this.ordersFailed);
        drawCompactStatCard(graphics, contentX + (thirdWidth + CARD_GAP) * 2, row4,
                contentWidth - thirdWidth * 2 - CARD_GAP * 2, CARD_HEIGHT,
                new ItemStack(Items.BARRIER), "Lost", this.customersLost);
    }

    private void drawStatCard(GuiGraphics graphics, int x, int y, int width, int height,
                              ItemStack icon, String label, String value) {
        drawStatPanel(graphics, x, y, width, height);
        drawItemSlot(graphics, x + 4, y + 4);
        graphics.renderItem(icon, x + 7, y + 7);
        graphics.drawString(this.font, fitText(label, width - 35), x + 32, y + 4, MUTED_TEXT, false);
        graphics.drawString(this.font, fitText(value, width - 35), x + 32, y + 16, VALUE_TEXT, false);
    }

    private void drawIconStatCard(GuiGraphics graphics, int x, int y, int width, int height,
                                  ResourceLocation icon, String label, String value) {
        drawStatPanel(graphics, x, y, width, height);
        drawItemSlot(graphics, x + 4, y + 4);
        drawIcon(graphics, icon, x + 7, y + 7);
        graphics.drawString(this.font, fitText(label, width - 35), x + 32, y + 4, MUTED_TEXT, false);
        graphics.drawString(this.font, fitText(value, width - 35), x + 32, y + 16, VALUE_TEXT, false);
    }

    private void drawReputationCard(GuiGraphics graphics, int x, int y, int width, int height) {
        drawStatPanel(graphics, x, y, width, height);
        drawItemSlot(graphics, x + 4, y + 4);
        drawIcon(graphics, REPUTATION_ICON, x + 7, y + 7);
        graphics.drawString(this.font, fitText("Reputation: " + this.reputation, width - 38),
                x + 32, y + 4, MUTED_TEXT, false);

        int barX = x + 32;
        int barY = y + 17;
        int barWidth = width - 39;
        graphics.fill(barX, barY, barX + barWidth, barY + 7, OUTLINE);
        graphics.fill(barX + 1, barY + 1, barX + barWidth - 1, barY + 6, PANEL_DARK_SHADOW);
        int progressWidth = (int) ((barWidth - 2) * Math.min(1.0D, Math.max(0, this.reputation) / 1000.0D));
        if (progressWidth > 0) {
            graphics.fill(barX + 1, barY + 1, barX + 1 + progressWidth, barY + 6, 0xFF55AA22);
            graphics.fill(barX + 1, barY + 1, barX + 1 + progressWidth, barY + 3, 0xFF80D43A);
        }
    }

    private void drawCompactIconStatCard(GuiGraphics graphics, int x, int y, int width, int height,
                                         ResourceLocation icon, String label, int value) {
        drawStatPanel(graphics, x, y, width, height);
        drawItemSlot(graphics, x + 4, y + 4);
        drawIcon(graphics, icon, x + 7, y + 7);
        graphics.drawString(this.font, fitText(label, width - 34), x + 32, y + 4, MUTED_TEXT, false);
        graphics.drawString(this.font, Integer.toString(value), x + 32, y + 16, VALUE_TEXT, false);
    }

    private void drawCompactStatCard(GuiGraphics graphics, int x, int y, int width, int height,
                                     ItemStack icon, String label, int value) {
        drawStatPanel(graphics, x, y, width, height);
        drawItemSlot(graphics, x + 4, y + 4);
        graphics.renderItem(icon, x + 7, y + 7);
        graphics.drawString(this.font, fitText(label, width - 34), x + 32, y + 4, MUTED_TEXT, false);
        graphics.drawString(this.font, Integer.toString(value), x + 32, y + 16, VALUE_TEXT, false);
    }

    private void drawIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y) {
        graphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);
    }

    private void drawRaisedPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        CodeGuiTheme.drawScreenPanel(graphics, x, y, width, height);
    }

    private void drawStatPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        CodeGuiTheme.drawInsetPanel(graphics, x, y, width, height);
    }

    private void drawItemSlot(GuiGraphics graphics, int x, int y) {
        CodeGuiTheme.drawSlot(graphics, x, y, 22);
    }

    private void drawWoodPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        CodeGuiTheme.drawWoodHeader(graphics, x, y, width, height);
    }

    private void drawStatusBadge(GuiGraphics graphics, int x, int y, int width, int height) {
        int color = this.open ? OPEN_GREEN : CLOSED_RED;
        graphics.fill(x, y, x + width, y + height, WOOD_DARK);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, color);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 5, brighten(color, 28));
        graphics.fill(x + 6, y + 8, x + 10, y + 12, this.open ? 0xFF83D275 : 0xFFE06A5E);
        String status = this.open ? "OPEN" : "CLOSED";
        graphics.drawString(this.font, Component.literal(status),
                x + width / 2 + 4 - this.font.width(status) / 2,
                y + (height - this.font.lineHeight) / 2, 0xFFFFFFFF, false);
    }

    private int brighten(int color, int amount) {
        int red = Math.min(255, ((color >> 16) & 255) + amount);
        int green = Math.min(255, ((color >> 8) & 255) + amount);
        int blue = Math.min(255, (color & 255) + amount);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static ResourceLocation icon(String name) {
        return new ResourceLocation("restaurantmanager", "textures/gui/icons/" + name + ".png");
    }

    private Component toggleLabel() {
        return Component.literal(this.open ? "Close" : "Open");
    }

    private String menuSummary() {
        int menuCount = 0;
        int itemCount = 0;
        for (int i = 0; i < this.menuTitles.length; i++) {
            if (!this.menuTitles[i].isBlank() || this.menuItemCounts[i] > 0) {
                menuCount++;
            }
            itemCount += this.menuItemCounts[i];
        }
        String menus = menuCount == 1 ? " menu" : " menus";
        String items = itemCount == 1 ? " item" : " items";
        return menuCount + menus + " / " + itemCount + items;
    }

    private String fitText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        return this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width(suffix))) + suffix;
    }

    @Override
    public void onClose() {
        saveName();
        super.onClose();
    }

    private void saveName() {
        if (this.nameBox == null) {
            return;
        }
        this.restaurantName = this.nameBox.getValue().isBlank() ? "Restaurant" : this.nameBox.getValue();
        ModNetwork.CHANNEL.sendToServer(new RenameRestaurantPacket(this.signPos, this.restaurantName));
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
            normalized[i] = counts != null && i < counts.length ? Math.max(0, counts[i]) : 0;
        }
        return normalized;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
