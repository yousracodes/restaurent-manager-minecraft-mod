package com.restaurantmanager.client;

import com.restaurantmanager.food.FoodRegistryScanner;
import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.SaveRestaurantMenuPacket;
import com.restaurantmanager.network.packet.SaveMenuItemPacket;
import com.restaurantmanager.restaurant.MenuCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuItemEditorScreen extends Screen {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("restaurantmanager", "textures/gui/restaurant_menu_editor.png");
    private static final ResourceLocation SEARCH_ICON =
            new ResourceLocation("restaurantmanager", "textures/gui/search_icon.png");
    private static final int TEXTURE_SIZE = 1254;
    private static final int SEARCH_ICON_TEXTURE_SIZE = 1007;
    private static final int LIGHT_TEXT_COLOR = 0xFFF2D2;
    private static final int TEXT_SHADOW_COLOR = 0x6B3F1D;
    private static final int DARK_TEXT_COLOR = 0x2B2118;
    private static final int MUTED_TEXT_COLOR = 0x6F5538;
    private static final int DANGER_TEXT_COLOR = 0x9D1E16;
    private static final int VISIBLE_ROWS = 6;
    private static final float ITEM_ICON_SCALE = 0.7F;

    private final InteractionHand hand;
    private final BlockPos restaurantSignPos;
    private final String initialTitle;
    private final List<ItemStack> foodCatalog = new ArrayList<>();
    private final List<ItemStack> filteredFoods = new ArrayList<>();
    private final List<ResourceLocation> selectedItems = new ArrayList<>();
    private MenuCategory category;
    private EditBox menuNameBox;
    private EditBox searchBox;
    private int leftPos;
    private int topPos;
    private int guiSize;
    private int restaurantNameX;
    private int restaurantNameY;
    private int restaurantNameWidth;
    private int menuNameX;
    private int menuNameY;
    private int menuNameWidth;
    private int categoryX;
    private int categoryY;
    private int categoryWidth;
    private int categoryLeftX;
    private int categoryRightX;
    private int categoryButtonY;
    private int categoryButtonSize;
    private int searchX;
    private int searchY;
    private int searchWidth;
    private int foodListX;
    private int foodListY;
    private int foodListWidth;
    private int selectedListX;
    private int selectedListY;
    private int selectedListWidth;
    private int rowHeight;
    private int iconSize;
    private int removeButtonSize;
    private int saveX;
    private int saveY;
    private int cancelX;
    private int cancelY;
    private int actionWidth;
    private int actionHeight;
    private int foodScrollOffset;
    private int selectedScrollOffset;

    public MenuItemEditorScreen(InteractionHand hand, ItemStack stack) {
        super(Component.literal("Restaurant Menu"));
        this.hand = hand;
        this.restaurantSignPos = null;
        this.initialTitle = RestaurantMenuItem.getMenuTitle(stack);
        this.selectedItems.addAll(RestaurantMenuItem.getMenuItems(stack));
        this.category = RestaurantMenuItem.getMenuCategory(stack);
        loadFoodCatalog();
        updateFilteredFoods();
    }

    public MenuItemEditorScreen(BlockPos restaurantSignPos, String title, MenuCategory category, List<ResourceLocation> items) {
        super(Component.literal("Restaurant Menu"));
        this.hand = null;
        this.restaurantSignPos = restaurantSignPos;
        this.initialTitle = title == null ? "" : title;
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
        if (items != null) {
            this.selectedItems.addAll(items);
        }
        loadFoodCatalog();
        updateFilteredFoods();
    }

    @Override
    protected void init() {
        this.guiSize = Math.min(310, Math.min(this.width - 20, this.height - 20));
        this.leftPos = (this.width - this.guiSize) / 2;
        this.topPos = (this.height - this.guiSize) / 2;

        this.restaurantNameX = textureX(240);
        this.restaurantNameY = textureY(142);
        this.restaurantNameWidth = textureWidth(770);
        this.menuNameX = textureX(205);
        this.menuNameY = textureY(256);
        this.menuNameWidth = textureWidth(850);

        this.categoryLeftX = textureX(331);
        this.categoryRightX = textureX(857);
        this.categoryButtonY = textureY(318);
        this.categoryButtonSize = textureWidth(70);
        this.categoryX = textureX(426);
        this.categoryY = textureY(346);
        this.categoryWidth = textureWidth(405);

        this.searchX = textureX(230);
        this.searchY = textureY(448);
        this.searchWidth = textureWidth(303);
        this.foodListX = textureX(137);
        this.foodListY = textureY(502);
        this.foodListWidth = textureWidth(430);
        this.selectedListX = textureX(662);
        this.selectedListY = textureY(500);
        this.selectedListWidth = textureWidth(420);
        this.rowHeight = textureHeight(79);
        this.iconSize = Math.max(16, textureWidth(52));
        this.removeButtonSize = Math.max(14, textureWidth(53));

        this.saveX = textureX(252);
        this.saveY = textureY(1044);
        this.cancelX = textureX(638);
        this.cancelY = this.saveY;
        this.actionWidth = textureWidth(365);
        this.actionHeight = textureHeight(82);

        this.menuNameBox = new EditBox(this.font, this.menuNameX, this.menuNameY, this.menuNameWidth, 16,
                Component.literal("Menu Name"));
        this.menuNameBox.setValue(this.initialTitle);
        this.menuNameBox.setMaxLength(64);
        this.menuNameBox.setBordered(false);
        this.menuNameBox.setTextColor(DARK_TEXT_COLOR);
        this.menuNameBox.setTextColorUneditable(DARK_TEXT_COLOR);
        this.addRenderableWidget(this.menuNameBox);

        this.searchBox = new EditBox(this.font, this.searchX, this.searchY, this.searchWidth, 16,
                Component.literal("Search Foods"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(DARK_TEXT_COLOR);
        this.searchBox.setTextColorUneditable(DARK_TEXT_COLOR);
        this.searchBox.setResponder(value -> {
            this.foodScrollOffset = 0;
            updateFilteredFoods();
        });
        this.addRenderableWidget(this.searchBox);

        clampScrollOffsets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, this.guiSize, this.guiSize,
                0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        drawCenteredDark(graphics, "Restaurant", this.restaurantNameX + this.restaurantNameWidth / 2, this.restaurantNameY);
        drawEditableCenteredDark(graphics, this.menuNameBox, this.menuNameX + this.menuNameWidth / 2, this.menuNameY);
        drawCenteredDark(graphics, this.category.getDisplayName(), this.categoryX + this.categoryWidth / 2, this.categoryY);
        drawSearchIcon(graphics, textureX(139), textureY(427), textureWidth(52));
        renderFoodCatalog(graphics, mouseX, mouseY);
        renderSelectedItems(graphics, mouseX, mouseY);
        renderActionLabel(graphics, this.saveX, this.saveY, this.actionWidth, this.actionHeight, "Save", mouseX, mouseY);
        renderActionLabel(graphics, this.cancelX, this.cancelY, this.actionWidth, this.actionHeight, "Cancel", mouseX, mouseY);
        renderArrowHover(graphics, this.categoryLeftX, this.categoryButtonY, mouseX, mouseY);
        renderArrowHover(graphics, this.categoryRightX, this.categoryButtonY, mouseX, mouseY);

        drawEditableFittedDark(graphics, this.searchBox, this.searchX, this.searchY, this.searchWidth, "Search");
    }

    private void renderFoodCatalog(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack hovered = ItemStack.EMPTY;
        int end = Math.min(this.filteredFoods.size(), this.foodScrollOffset + VISIBLE_ROWS);
        for (int i = this.foodScrollOffset; i < end; i++) {
            int row = i - this.foodScrollOffset;
            int rowY = this.foodListY + row * this.rowHeight;
            ItemStack stack = this.filteredFoods.get(i);
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            boolean alreadySelected = id != null && this.selectedItems.contains(id);
            boolean hoveredRow = isInside(mouseX, mouseY, this.foodListX, rowY, this.foodListWidth, this.rowHeight - 3);
            if (hoveredRow) {
                graphics.fill(this.foodListX, rowY, this.foodListX + this.foodListWidth, rowY + this.rowHeight - 3, 0x22FFFFFF);
                hovered = stack;
            }
            if (alreadySelected) {
                graphics.fill(this.foodListX, rowY, this.foodListX + this.foodListWidth, rowY + this.rowHeight - 3, 0x226A8D46);
            }
            int iconX = textureX(146);
            int iconY = rowY + Math.max(0, (this.rowHeight - this.iconSize) / 2);
            renderScaledItem(graphics, stack, iconX, iconY);
            drawFittedDark(graphics, stack.getHoverName().getString(), textureX(213), rowY + this.rowHeight / 2 - 5,
                    textureWidth(345));
        }
        if (this.filteredFoods.isEmpty()) {
            drawCenteredDark(graphics, "No foods found", textureX(355), textureY(720));
        }
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    private void renderSelectedItems(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack hovered = ItemStack.EMPTY;
        int end = Math.min(this.selectedItems.size(), this.selectedScrollOffset + VISIBLE_ROWS);
        for (int i = this.selectedScrollOffset; i < end; i++) {
            int row = i - this.selectedScrollOffset;
            int rowY = this.selectedListY + row * this.rowHeight;
            ItemStack stack = stackForId(this.selectedItems.get(i));
            boolean hoveredRow = isInside(mouseX, mouseY, this.selectedListX, rowY, this.selectedListWidth, this.rowHeight - 3);
            if (hoveredRow) {
                graphics.fill(this.selectedListX, rowY, this.selectedListX + this.selectedListWidth, rowY + this.rowHeight - 3, 0x22FFFFFF);
                hovered = stack;
            }
            int iconX = textureX(672);
            int iconY = rowY + Math.max(0, (this.rowHeight - this.iconSize) / 2);
            if (!stack.isEmpty()) {
                renderScaledItem(graphics, stack, iconX, iconY);
                drawFittedDark(graphics, stack.getHoverName().getString(), textureX(735), rowY + this.rowHeight / 2 - 5,
                        textureWidth(270));
            } else {
                drawFittedDark(graphics, this.selectedItems.get(i).toString(), textureX(735), rowY + this.rowHeight / 2 - 5,
                        textureWidth(270));
            }
            int removeX = textureX(1023);
            int removeY = rowY + textureHeight(10);
            if (isInside(mouseX, mouseY, removeX, removeY, this.removeButtonSize, this.removeButtonSize)) {
                graphics.fill(removeX, removeY, removeX + this.removeButtonSize, removeY + this.removeButtonSize, 0x22FFFFFF);
            }
            drawCentered(graphics, "X", removeX + this.removeButtonSize / 2,
                    removeY + (this.removeButtonSize - this.font.lineHeight) / 2,
                    DANGER_TEXT_COLOR);
        }
        if (this.selectedItems.isEmpty()) {
            drawCenteredDark(graphics, "No items", textureX(888), textureY(720));
        }
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInside(mouseX, mouseY, this.menuNameX, this.menuNameY - 8, this.menuNameWidth, 26)) {
            this.menuNameBox.setFocused(true);
            this.searchBox.setFocused(false);
            this.setFocused(this.menuNameBox);
            return true;
        }
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.menuNameBox.setFocused(false);
            this.setFocused(this.searchBox);
            return true;
        }
        if (isInside(mouseX, mouseY, this.categoryLeftX, this.categoryButtonY, this.categoryButtonSize, this.categoryButtonSize)) {
            cycleCategory(-1);
            return true;
        }
        if (isInside(mouseX, mouseY, this.categoryRightX, this.categoryButtonY, this.categoryButtonSize, this.categoryButtonSize)) {
            cycleCategory(1);
            return true;
        }
        if (isInside(mouseX, mouseY, this.saveX, this.saveY, this.actionWidth, this.actionHeight)) {
            saveMenu();
            this.onClose();
            return true;
        }
        if (isInside(mouseX, mouseY, this.cancelX, this.cancelY, this.actionWidth, this.actionHeight)) {
            this.onClose();
            return true;
        }
        if (handleFoodCatalogClick(mouseX, mouseY)) {
            return true;
        }
        if (handleSelectedItemsClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.menuNameBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_ESCAPE)) {
            this.menuNameBox.setFocused(false);
            this.setFocused(null);
            return true;
        }
        if (this.menuNameBox.keyPressed(keyCode, scanCode, modifiers) || this.menuNameBox.canConsumeInput()) {
            return true;
        }
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers) || this.searchBox.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.menuNameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInside(mouseX, mouseY, textureX(115), textureY(406), textureWidth(500), textureHeight(620))) {
            this.foodScrollOffset = scroll(this.foodScrollOffset, getMaxFoodScroll(), delta);
            return true;
        }
        if (isInside(mouseX, mouseY, textureX(636), textureY(406), textureWidth(500), textureHeight(620))) {
            this.selectedScrollOffset = scroll(this.selectedScrollOffset, getMaxSelectedScroll(), delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean handleFoodCatalogClick(double mouseX, double mouseY) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = this.foodScrollOffset + row;
            if (index >= this.filteredFoods.size()) {
                break;
            }
            int rowY = this.foodListY + row * this.rowHeight;
            if (isInside(mouseX, mouseY, this.foodListX, rowY, this.foodListWidth, this.rowHeight - 3)) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(this.filteredFoods.get(index).getItem());
                if (id != null && !this.selectedItems.contains(id)) {
                    this.selectedItems.add(id);
                    this.selectedScrollOffset = getMaxSelectedScroll();
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleSelectedItemsClick(double mouseX, double mouseY) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = this.selectedScrollOffset + row;
            if (index >= this.selectedItems.size()) {
                break;
            }
            int rowY = this.selectedListY + row * this.rowHeight;
            int removeX = textureX(1023);
            int removeY = rowY + textureHeight(10);
            if (isInside(mouseX, mouseY, removeX, removeY, this.removeButtonSize, this.removeButtonSize)) {
                this.selectedItems.remove(index);
                this.selectedScrollOffset = Math.min(this.selectedScrollOffset, getMaxSelectedScroll());
                return true;
            }
        }
        return false;
    }

    private void loadFoodCatalog() {
        if (!FoodRegistryScanner.hasEdibleFoods()) {
            FoodRegistryScanner.scanEdibleFoods();
        }
        this.foodCatalog.clear();
        for (Item item : FoodRegistryScanner.getEdibleItems()) {
            this.foodCatalog.add(new ItemStack(item));
        }
    }

    private void updateFilteredFoods() {
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        this.filteredFoods.clear();
        for (ItemStack stack : this.foodCatalog) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            String itemId = id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query) || itemId.contains(query)) {
                this.filteredFoods.add(stack);
            }
        }
        clampScrollOffsets();
    }

    private void saveMenu() {
        if (this.restaurantSignPos != null) {
            ModNetwork.CHANNEL.sendToServer(new SaveRestaurantMenuPacket(this.restaurantSignPos,
                    this.menuNameBox.getValue(), this.category, this.selectedItems));
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new SaveMenuItemPacket(this.hand, this.menuNameBox.getValue(), this.category, this.selectedItems));
    }

    private void cycleCategory(int direction) {
        MenuCategory[] categories = MenuCategory.values();
        int index = (this.category.ordinal() + direction) % categories.length;
        if (index < 0) {
            index += categories.length;
        }
        this.category = categories[index];
    }

    private ItemStack stackForId(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private int scroll(int offset, int maxOffset, double delta) {
        if (delta < 0) {
            return Math.min(maxOffset, offset + 1);
        }
        if (delta > 0) {
            return Math.max(0, offset - 1);
        }
        return offset;
    }

    private void clampScrollOffsets() {
        this.foodScrollOffset = Math.min(this.foodScrollOffset, getMaxFoodScroll());
        this.selectedScrollOffset = Math.min(this.selectedScrollOffset, getMaxSelectedScroll());
    }

    private int getMaxFoodScroll() {
        return Math.max(0, this.filteredFoods.size() - VISIBLE_ROWS);
    }

    private int getMaxSelectedScroll() {
        return Math.max(0, this.selectedItems.size() - VISIBLE_ROWS);
    }

    private void renderActionLabel(GuiGraphics graphics, int x, int y, int width, int height, String label, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0x22FFFFFF);
        }
        drawCenteredDark(graphics, label, x + width / 2, y + height / 2 - 4);
    }

    private void renderArrowHover(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x, y, this.categoryButtonSize, this.categoryButtonSize)) {
            graphics.fill(x + 3, y + 3, x + this.categoryButtonSize - 3, y + this.categoryButtonSize - 3, 0x22FFFFFF);
        }
    }

    private void drawFittedDark(GuiGraphics graphics, String text, int x, int y, int maxWidth) {
        if (maxWidth <= 0) {
            return;
        }
        String fitted = text;
        if (this.font.width(fitted) > maxWidth) {
            fitted = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("..."))) + "...";
        }
        graphics.drawString(this.font, Component.literal(fitted), x, y, DARK_TEXT_COLOR, false);
    }

    private void renderScaledItem(GuiGraphics graphics, ItemStack stack, int slotX, int slotY) {
        int renderSize = Math.round(16 * ITEM_ICON_SCALE);
        int itemX = slotX + (this.iconSize - renderSize) / 2 - textureWidth(14);
        int itemY = slotY + (this.iconSize - renderSize) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(itemX, itemY, 0.0F);
        graphics.pose().scale(ITEM_ICON_SCALE, ITEM_ICON_SCALE, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(this.font, stack, 0, 0);
        graphics.pose().popPose();
    }

    private void drawSearchIcon(GuiGraphics graphics, int x, int y, int size) {
        graphics.blit(SEARCH_ICON, x, y, size, size, 0.0F, 0.0F,
                SEARCH_ICON_TEXTURE_SIZE, SEARCH_ICON_TEXTURE_SIZE,
                SEARCH_ICON_TEXTURE_SIZE, SEARCH_ICON_TEXTURE_SIZE);
    }

    private void drawEditableCenteredDark(GuiGraphics graphics, EditBox box, int centerX, int y) {
        String value = box.getValue();
        String text = value.isEmpty() && !box.isFocused() ? "Menu Name" : value;
        if (box.isFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            text += "_";
        }
        drawCentered(graphics, text, centerX, y, value.isEmpty() ? MUTED_TEXT_COLOR : DARK_TEXT_COLOR);
    }

    private void drawEditableFittedDark(GuiGraphics graphics, EditBox box, int x, int y, int maxWidth, String placeholder) {
        String value = box.getValue();
        String text = value.isEmpty() && !box.isFocused() ? placeholder : value;
        if (box.isFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            text += "_";
        }
        if (this.font.width(text) > maxWidth) {
            text = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("..."))) + "...";
        }
        graphics.drawString(this.font, Component.literal(text), x, y, value.isEmpty() ? MUTED_TEXT_COLOR : DARK_TEXT_COLOR, false);
    }

    private void drawCenteredLight(GuiGraphics graphics, String text, int centerX, int y) {
        graphics.drawString(this.font, Component.literal(text), centerX - this.font.width(text) / 2 + 1, y + 1,
                TEXT_SHADOW_COLOR, false);
        graphics.drawString(this.font, Component.literal(text), centerX - this.font.width(text) / 2, y,
                LIGHT_TEXT_COLOR, false);
    }

    private void drawCenteredDark(GuiGraphics graphics, String text, int centerX, int y) {
        drawCentered(graphics, text, centerX, y, DARK_TEXT_COLOR);
    }

    private void drawCentered(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawString(this.font, Component.literal(text), centerX - this.font.width(text) / 2, y, color, false);
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
