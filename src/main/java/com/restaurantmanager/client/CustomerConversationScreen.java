package com.restaurantmanager.client;

import com.restaurantmanager.entity.CustomerEntity;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.ServeCustomerPacket;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class CustomerConversationScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("restaurantmanager", "textures/gui/customer_conversation.png");
    private static final ResourceLocation TAKEOUT_BACKGROUND = new ResourceLocation("restaurantmanager", "textures/gui/takeout_customer.png");
    private static final int TEXTURE_WIDTH = 1402;
    private static final int TEXTURE_HEIGHT = 1122;
    private static final int TAKEOUT_TEXTURE_FULL_WIDTH = 1448;
    private static final int TAKEOUT_TEXTURE_FULL_HEIGHT = 1086;
    private static final int TAKEOUT_TEXTURE_SOURCE_X = 40;
    private static final int TAKEOUT_TEXTURE_SOURCE_Y = 16;
    private static final int TAKEOUT_TEXTURE_WIDTH = 1369;
    private static final int TAKEOUT_TEXTURE_HEIGHT = 1036;
    private static final int ITEM_RENDER_SIZE = 16;
    private static final int TAKEOUT_PACKAGE_SLOT_CENTER_X = 263;
    private static final int TAKEOUT_PACKAGE_SLOT_CENTER_Y = 620;
    private static final int TAKEOUT_ORDER_ICON_CENTER_X = 585;
    private static final int TAKEOUT_ORDER_ICON_CENTER_Y = 480;
    private static final int TAKEOUT_ORDER_ROW_SPACING = 139;
    private static final int TAKEOUT_ORDER_STATUS_X = 1191;
    private static final int TAKEOUT_ORDER_STATUS_Y = 435;
    private static final int TAKEOUT_ORDER_STATUS_SIZE = 88;
    private static final int TAKEOUT_TEXT_COLOR = 0x3B2717;
    private static final String[] TAKEOUT_DIALOG_LINES = {
            "I'm starving... I need:",
            "I've been craving this all day:",
            "How are you? I'll be having:",
            "I'd like to grab:",
            "I'm thinking of ordering:",
            "Great day today, I'm craving:",
            "Hello! I'd like to order:"
    };

    private final int customerId;
    private final Component customerName;
    private final String orderSummary;
    private final List<ItemStack> orderItems;
    private final boolean takeoutLayout;
    private final boolean takeoutCustomer;
    private final int orderStartIndex;
    private final boolean preferTakeoutOrderSnapshot;
    private ItemStack suggestedItem = ItemStack.EMPTY;
    private int leftPos;
    private int topPos;
    private int guiWidth;
    private int guiHeight;
    private int serveX;
    private int askX;
    private int refuseX;
    private int buttonY;
    private int buttonWidth;
    private int buttonHeight;
    private int suggestX;
    private int suggestY;
    private int suggestWidth;
    private int openInventoryX;
    private int openInventoryY;
    private int openInventoryWidth;
    private int suggestionSlotX;
    private int suggestionSlotY;
    private boolean suggestionControlsVisible;
    private int takeoutServeX;
    private int takeoutLoseX;
    private int takeoutCloseX;
    private int takeoutButtonY;
    private int takeoutButtonWidth;
    private int takeoutButtonHeight;

    public CustomerConversationScreen(CustomerEntity customer) {
        this(customer.getId(), customer.getDisplayName(), customer.getOrderSummary(), customer.getOrderItemStacks(),
                true, customer.getOrderStartIndex(), false, customer.isTakeoutCustomer());
    }

    public CustomerConversationScreen(int customerId, Component customerName) {
        this(customerId, customerName, "");
    }

    public CustomerConversationScreen(int customerId, Component customerName, String orderSummary) {
        this(customerId, customerName, orderSummary, List.of(), false, 0);
    }

    public CustomerConversationScreen(int customerId, Component customerName, String orderSummary,
                                      List<ItemStack> orderItems, boolean takeoutLayout, int orderStartIndex) {
        this(customerId, customerName, orderSummary, orderItems, takeoutLayout, orderStartIndex, false);
    }

    public CustomerConversationScreen(int customerId, Component customerName, String orderSummary,
                                      List<ItemStack> orderItems, boolean takeoutLayout, int orderStartIndex,
                                      boolean preferTakeoutOrderSnapshot) {
        this(customerId, customerName, orderSummary, orderItems, takeoutLayout, orderStartIndex,
                preferTakeoutOrderSnapshot, takeoutLayout);
    }

    public CustomerConversationScreen(int customerId, Component customerName, String orderSummary,
                                      List<ItemStack> orderItems, boolean takeoutLayout, int orderStartIndex,
                                      boolean preferTakeoutOrderSnapshot, boolean takeoutCustomer) {
        super(Component.literal("Customer"));
        this.customerId = customerId;
        this.customerName = customerName;
        this.orderSummary = orderSummary == null ? "" : orderSummary;
        this.orderItems = List.copyOf(orderItems);
        this.takeoutLayout = takeoutLayout;
        this.takeoutCustomer = takeoutCustomer;
        this.orderStartIndex = Math.max(0, orderStartIndex);
        this.preferTakeoutOrderSnapshot = preferTakeoutOrderSnapshot;
    }

    public CustomerConversationScreen(int customerId, Component customerName, ItemStack suggestedItem) {
        this(customerId, customerName);
        this.suggestedItem = suggestedItem.copyWithCount(1);
        this.suggestionControlsVisible = true;
    }

    @Override
    protected void init() {
        if (takeoutLayout) {
            initTakeoutLayout();
            return;
        }

        this.guiWidth = Math.min(520, this.width - 20);
        this.guiHeight = this.guiWidth * TEXTURE_HEIGHT / TEXTURE_WIDTH;
        if (this.guiHeight > this.height - 20) {
            this.guiHeight = this.height - 20;
            this.guiWidth = this.guiHeight * TEXTURE_WIDTH / TEXTURE_HEIGHT;
        }

        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;

        this.buttonY = textureY(825);
        this.buttonHeight = textureHeight(88);
        this.serveX = textureX(86);
        this.askX = textureX(540);
        this.refuseX = textureX(965);
        this.buttonWidth = textureWidth(340);

        this.suggestionSlotX = textureX(855);
        this.suggestionSlotY = textureY(685);
        this.suggestX = textureX(520);
        this.suggestY = textureY(935);
        this.suggestWidth = textureWidth(230);
        this.openInventoryX = textureX(485);
        this.openInventoryY = textureY(935);
        this.openInventoryWidth = textureWidth(310);
    }

    private void initTakeoutLayout() {
        this.guiHeight = Math.min(TAKEOUT_TEXTURE_HEIGHT, this.height - 20);
        this.guiWidth = this.guiHeight * TAKEOUT_TEXTURE_WIDTH / TAKEOUT_TEXTURE_HEIGHT;
        if (this.guiWidth > this.width - 20) {
            this.guiWidth = this.width - 20;
            this.guiHeight = this.guiWidth * TAKEOUT_TEXTURE_HEIGHT / TAKEOUT_TEXTURE_WIDTH;
        }

        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;
        this.takeoutServeX = takeoutX(80);
        this.takeoutLoseX = takeoutX(518);
        this.takeoutCloseX = takeoutX(929);
        this.takeoutButtonY = takeoutY(881);
        this.takeoutButtonWidth = takeoutWidth(360);
        this.takeoutButtonHeight = takeoutHeight(106);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (takeoutLayout) {
            renderTakeout(graphics, mouseX, mouseY);
            return;
        }

        this.renderBackground(graphics);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, this.guiWidth, this.guiHeight,
                0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        int textColor = 0x2B2118;
        int titleCenterX = textureX(725);
        int titleY = textureY(112);
        int nameCenterX = textureX(725);
        int nameY = textureY(203);
        int dialogueX = textureX(435);
        int dialogueY = textureY(318);
        int dialogueWidth = textureWidth(575);
        int orderX = textureX(300);
        int orderY = textureY(632);
        int orderWidth = textureWidth(595);

        graphics.drawCenteredString(this.font, Component.literal("Customer"), titleCenterX, titleY, textColor);
        graphics.drawCenteredString(this.font, Component.literal("Restaurant Guest"), nameCenterX, nameY, textColor);
        drawWrapped(graphics, Component.literal("Ah, nothing beats a good meal after traveling. I'm starving!"),
                dialogueX, dialogueY, dialogueWidth, textColor, 4);
        graphics.drawString(this.font, Component.literal("I'd like:"), orderX, orderY, textColor, false);
        drawOrderSummary(graphics, orderX, orderY + 12, orderWidth, textColor);

        if (suggestionControlsVisible) {
            graphics.drawString(this.font, Component.literal(suggestedItem.isEmpty() ? "Open inventory and pick food." : "Suggested item:"), orderX, orderY + 34, textColor, false);
            int slotSize = Math.max(18, textureWidth(45));
            graphics.fill(suggestionSlotX - 1, suggestionSlotY - 1, suggestionSlotX + slotSize + 1, suggestionSlotY + slotSize + 1, 0xFF8B8B8B);
            graphics.fill(suggestionSlotX, suggestionSlotY, suggestionSlotX + slotSize, suggestionSlotY + slotSize, 0xFF202020);
            if (!suggestedItem.isEmpty()) {
                graphics.renderItem(suggestedItem, suggestionSlotX + 3, suggestionSlotY + 3);
                graphics.renderItemDecorations(this.font, suggestedItem, suggestionSlotX + 3, suggestionSlotY + 3);
            }
        }

        drawButtonLabel(graphics, this.serveX, this.buttonY, this.buttonWidth, this.buttonHeight, "Serve", mouseX, mouseY);
        drawButtonLabel(graphics, this.askX, this.buttonY, this.buttonWidth, this.buttonHeight, "Unavailable", mouseX, mouseY);
        drawButtonLabel(graphics, this.refuseX, this.buttonY, this.buttonWidth, this.buttonHeight, "Close", mouseX, mouseY);
        if (suggestionControlsVisible) {
            if (suggestedItem.isEmpty()) {
                drawButtonLabel(graphics, this.openInventoryX, this.openInventoryY, this.openInventoryWidth, this.buttonHeight,
                        "Open Inventory", mouseX, mouseY);
            } else {
                drawButtonLabel(graphics, this.suggestX, this.suggestY, this.suggestWidth, this.buttonHeight,
                        "Clear item", mouseX, mouseY);
            }
        }
    }

    private void renderTakeout(GuiGraphics graphics, int mouseX, int mouseY) {
        this.renderBackground(graphics);
        graphics.blit(TAKEOUT_BACKGROUND, this.leftPos, this.topPos, this.guiWidth, this.guiHeight,
                TAKEOUT_TEXTURE_SOURCE_X, TAKEOUT_TEXTURE_SOURCE_Y, TAKEOUT_TEXTURE_WIDTH, TAKEOUT_TEXTURE_HEIGHT,
                TAKEOUT_TEXTURE_FULL_WIDTH, TAKEOUT_TEXTURE_FULL_HEIGHT);

        renderTakeoutDialog(graphics);
        renderTakeoutPackageSlot(graphics, mouseX, mouseY);
        renderTakeoutOrderRows(graphics, mouseX, mouseY);
        renderTakeoutButtonHover(graphics, this.takeoutServeX, this.takeoutButtonY, this.takeoutButtonWidth, this.takeoutButtonHeight, mouseX, mouseY);
        renderTakeoutButtonHover(graphics, this.takeoutLoseX, this.takeoutButtonY, this.takeoutButtonWidth, this.takeoutButtonHeight, mouseX, mouseY);
        renderTakeoutButtonHover(graphics, this.takeoutCloseX, this.takeoutButtonY, this.takeoutButtonWidth, this.takeoutButtonHeight, mouseX, mouseY);
        drawButtonLabel(graphics, this.takeoutServeX, this.takeoutButtonY, this.takeoutButtonWidth, this.takeoutButtonHeight, "Serve", mouseX, mouseY);
        drawButtonLabel(graphics, this.takeoutLoseX, this.takeoutButtonY, this.takeoutButtonWidth, this.takeoutButtonHeight, "Unavailable", mouseX, mouseY);
        drawButtonLabel(graphics, this.takeoutCloseX, this.takeoutButtonY, this.takeoutButtonWidth, this.takeoutButtonHeight, "Close", mouseX, mouseY);
    }

    private void renderTakeoutDialog(GuiGraphics graphics) {
        String dialog = TAKEOUT_DIALOG_LINES[Math.floorMod(customerId, TAKEOUT_DIALOG_LINES.length)];
        drawWrapped(graphics, Component.literal(dialog), takeoutX(352), takeoutY(158), takeoutWidth(860), TAKEOUT_TEXT_COLOR, 3);
    }

    private void renderTakeoutPackageSlot(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack packageStack = getHeldServingContainer();
        int x = takeoutCenteredItemX(TAKEOUT_PACKAGE_SLOT_CENTER_X);
        int y = takeoutCenteredItemY(TAKEOUT_PACKAGE_SLOT_CENTER_Y);
        if (!packageStack.isEmpty()) {
            graphics.renderItem(packageStack, x, y);
            graphics.renderItemDecorations(this.font, packageStack, x, y);
            if (isInside(mouseX, mouseY, x, y, ITEM_RENDER_SIZE, ITEM_RENDER_SIZE)) {
                graphics.renderTooltip(this.font, packageStack, mouseX, mouseY);
            }
        }
    }

    private void renderTakeoutOrderRows(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> names = getTakeoutOrderNames();
        List<ItemStack> items = getTakeoutOrderItems();
        for (int row = 0; row < 3; row++) {
            int iconX = takeoutCenteredItemX(TAKEOUT_ORDER_ICON_CENTER_X);
            int iconY = takeoutCenteredItemY(TAKEOUT_ORDER_ICON_CENTER_Y + row * TAKEOUT_ORDER_ROW_SPACING);
            int textX = takeoutX(656);
            int textY = takeoutY(472 + row * TAKEOUT_ORDER_ROW_SPACING);
            int statusX = takeoutX(TAKEOUT_ORDER_STATUS_X);
            int statusY = takeoutY(TAKEOUT_ORDER_STATUS_Y + row * TAKEOUT_ORDER_ROW_SPACING);
            int statusSize = takeoutWidth(TAKEOUT_ORDER_STATUS_SIZE);

            if (row < items.size() && !items.get(row).isEmpty()) {
                ItemStack stack = items.get(row);
                graphics.renderItem(stack, iconX, iconY);
                graphics.renderItemDecorations(this.font, stack, iconX, iconY);
                if (isInside(mouseX, mouseY, iconX, iconY, ITEM_RENDER_SIZE, ITEM_RENDER_SIZE)) {
                    graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }

            if (row < names.size() && !names.get(row).isBlank()) {
                drawClippedString(graphics, names.get(row), textX, textY, takeoutWidth(485), TAKEOUT_TEXT_COLOR);
                graphics.drawCenteredString(this.font, Component.literal("!"),
                        statusX + statusSize / 2, statusY + statusSize / 2 - 4, 0xFFF2D0);
                if (isInside(mouseX, mouseY, statusX, statusY, statusSize, statusSize)) {
                    graphics.fill(statusX + 3, statusY + 3, statusX + statusSize - 3, statusY + statusSize - 3, 0x44FFFFFF);
                    graphics.renderTooltip(this.font, Component.literal("Item unavailable"), mouseX, mouseY);
                }
            }
        }
    }

    private List<String> getTakeoutOrderNames() {
        List<ItemStack> items = getTakeoutOrderItems();
        List<String> names = new ArrayList<>();
        if (items.isEmpty()) {
            return getOrderLines();
        }

        List<String> orderLines = getOrderLines();
        for (int index = 0; index < items.size(); index++) {
            ItemStack stack = items.get(index);
            if (!stack.isEmpty()) {
                names.add(stack.getHoverName().getString());
            } else if (preferTakeoutOrderSnapshot) {
                names.add("");
            } else {
                names.add("");
            }
        }
        return names;
    }

    private void drawClippedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        String clipped = this.font.plainSubstrByWidth(text, maxWidth);
        if (!clipped.equals(text) && clipped.length() > 1) {
            clipped = clipped.substring(0, clipped.length() - 1) + "...";
        }
        graphics.drawString(this.font, Component.literal(clipped), x, y, color, false);
    }

    private List<String> getOrderLines() {
        List<String> lines = new ArrayList<>();
        for (String rawLine : getTakeoutOrderSummary().split("\\R")) {
            String line = rawLine.trim();
            int dot = line.indexOf(". ");
            if (dot >= 0) {
                line = line.substring(dot + 2);
            }
            int rewardStart = line.lastIndexOf(" (");
            if (rewardStart > 0) {
                line = line.substring(0, rewardStart);
            }
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private List<ItemStack> getTakeoutOrderItems() {
        if (preferTakeoutOrderSnapshot && !orderItems.isEmpty()) {
            return orderItems;
        }
        CustomerEntity customer = getLiveCustomer();
        if (customer != null) {
            List<ItemStack> liveItems = customer.getOrderItemStacks();
            if (!liveItems.isEmpty()) {
                return liveItems;
            }
        }
        return orderItems;
    }

    private String getTakeoutOrderSummary() {
        CustomerEntity customer = getLiveCustomer();
        if (customer != null && !customer.getOrderSummary().isBlank()) {
            return customer.getOrderSummary();
        }
        return orderSummary;
    }

    private int getTakeoutOrderStartIndex() {
        CustomerEntity customer = getLiveCustomer();
        return customer == null ? orderStartIndex : customer.getOrderStartIndex();
    }

    private CustomerEntity getLiveCustomer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(customerId);
        return entity instanceof CustomerEntity customer ? customer : null;
    }

    private void renderTakeoutButtonHover(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0x22FFFFFF);
        }
    }

    private void drawButtonLabel(GuiGraphics graphics, int x, int y, int width, int height, String label, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0x22FFFFFF);
        }
        graphics.drawCenteredString(this.font, Component.literal(label), x + width / 2, y + height / 2 - 4, 0xFFFFFF);
    }

    private void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color, int maxLines) {
        int line = 0;
        for (net.minecraft.util.FormattedCharSequence sequence : this.font.split(text, width)) {
            if (line >= maxLines) {
                return;
            }
            graphics.drawString(this.font, sequence, x, y + line * 10, color, false);
            line++;
        }
    }

    private void drawOrderSummary(GuiGraphics graphics, int x, int y, int width, int color) {
        String text = orderSummary.isBlank() ? customerName.getString() : orderSummary;
        int line = 0;
        for (String orderLine : text.split("\\R")) {
            for (net.minecraft.util.FormattedCharSequence sequence : this.font.split(Component.literal(orderLine), width)) {
                if (line >= 4) {
                    return;
                }
                graphics.drawString(this.font, sequence, x, y + line * 10, color, false);
                line++;
            }
        }
    }

    private void showSuggestionControls() {
        this.suggestionControlsVisible = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (takeoutLayout) {
            return mouseClickedTakeout(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
        }

        if (isInside(mouseX, mouseY, this.serveX, this.buttonY, this.buttonWidth, this.buttonHeight)) {
            if (suggestionControlsVisible && !suggestedItem.isEmpty()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(suggestedItem.getItem());
                ModNetwork.CHANNEL.sendToServer(new ServeCustomerPacket(customerId, ServeCustomerPacket.Action.SUGGEST, id == null ? "" : id.toString()));
            } else {
                ModNetwork.CHANNEL.sendToServer(new ServeCustomerPacket(customerId, ServeCustomerPacket.Action.SERVE, ""));
            }
            this.onClose();
            return true;
        }
        if (isInside(mouseX, mouseY, this.askX, this.buttonY, this.buttonWidth, this.buttonHeight)) {
            ModNetwork.CHANNEL.sendToServer(new ServeCustomerPacket(customerId, ServeCustomerPacket.Action.UNAVAILABLE, ""));
            this.onClose();
            return true;
        }
        if (isInside(mouseX, mouseY, this.refuseX, this.buttonY, this.buttonWidth, this.buttonHeight)) {
            this.onClose();
            return true;
        }
        if (suggestionControlsVisible && suggestedItem.isEmpty()
                && isInside(mouseX, mouseY, this.openInventoryX, this.openInventoryY, this.openInventoryWidth, this.buttonHeight)) {
            Minecraft.getInstance().setScreen(new FoodSelectionScreen(customerId, customerName));
            return true;
        }
        if (suggestionControlsVisible && !suggestedItem.isEmpty()
                && isInside(mouseX, mouseY, this.suggestX, this.suggestY, this.suggestWidth, this.buttonHeight)) {
            suggestedItem = ItemStack.EMPTY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean mouseClickedTakeout(double mouseX, double mouseY) {
        for (int row = 0; row < 3; row++) {
            int statusX = takeoutX(TAKEOUT_ORDER_STATUS_X);
            int statusY = takeoutY(TAKEOUT_ORDER_STATUS_Y + row * TAKEOUT_ORDER_ROW_SPACING);
            int statusSize = takeoutWidth(TAKEOUT_ORDER_STATUS_SIZE);
            List<String> names = getTakeoutOrderNames();
            if (row < names.size() && !names.get(row).isBlank()
                    && isInside(mouseX, mouseY, statusX, statusY, statusSize, statusSize)) {
                int currentOrderStartIndex = getTakeoutOrderStartIndex();
                int requestIndex = currentOrderStartIndex + row;
                ModNetwork.CHANNEL.sendToServer(new ServeCustomerPacket(
                        customerId,
                        ServeCustomerPacket.Action.UNAVAILABLE_ITEM,
                        "",
                        requestIndex
                ));
                return true;
            }
        }

        if (isInside(mouseX, mouseY, this.takeoutServeX, this.takeoutButtonY, this.takeoutButtonWidth, this.takeoutButtonHeight)) {
            ModNetwork.CHANNEL.sendToServer(new ServeCustomerPacket(customerId, ServeCustomerPacket.Action.SERVE, ""));
            this.onClose();
            return true;
        }
        if (isInside(mouseX, mouseY, this.takeoutLoseX, this.takeoutButtonY, this.takeoutButtonWidth, this.takeoutButtonHeight)) {
            ModNetwork.CHANNEL.sendToServer(new ServeCustomerPacket(customerId, ServeCustomerPacket.Action.LOSE_REQUEST, ""));
            this.onClose();
            return true;
        }
        if (isInside(mouseX, mouseY, this.takeoutCloseX, this.takeoutButtonY, this.takeoutButtonWidth, this.takeoutButtonHeight)) {
            this.onClose();
            return true;
        }
        return false;
    }

    private ItemStack getHeldServingContainer() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        if (!isViewingTakeoutCustomer()) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() == ModItems.PLATE.get()) {
                return mainHand;
            }
            ItemStack offHand = player.getOffhandItem();
            return offHand.getItem() == ModItems.PLATE.get() ? offHand : ItemStack.EMPTY;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() == ModItems.TAKEOUT_PACKAGE.get()) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        return offHand.getItem() == ModItems.TAKEOUT_PACKAGE.get() ? offHand : ItemStack.EMPTY;
    }

    private boolean isViewingTakeoutCustomer() {
        CustomerEntity customer = getLiveCustomer();
        return customer == null ? takeoutCustomer : customer.isTakeoutCustomer();
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
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

    private int takeoutX(int textureX) {
        return this.leftPos + textureX * this.guiWidth / TAKEOUT_TEXTURE_WIDTH;
    }

    private int takeoutY(int textureY) {
        return this.topPos + textureY * this.guiHeight / TAKEOUT_TEXTURE_HEIGHT;
    }

    private int takeoutWidth(int textureWidth) {
        return textureWidth * this.guiWidth / TAKEOUT_TEXTURE_WIDTH;
    }

    private int takeoutHeight(int textureHeight) {
        return textureHeight * this.guiHeight / TAKEOUT_TEXTURE_HEIGHT;
    }

    private int takeoutCenteredItemX(int textureCenterX) {
        return takeoutX(textureCenterX) - ITEM_RENDER_SIZE / 2;
    }

    private int takeoutCenteredItemY(int textureCenterY) {
        return takeoutY(textureCenterY) - ITEM_RENDER_SIZE / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
