package com.restaurantmanager.client;

import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.ServeCustomerPacket;
import com.restaurantmanager.food.FoodRegistryScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class TakeoutNegotiationScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("restaurantmanager", "textures/gui/takeout_negotiation.png");
    private static final int TEXTURE_WIDTH = 1448;
    private static final int TEXTURE_HEIGHT = 1086;
    private static final int ITEM_RENDER_SIZE = 16;
    private static final float SUGGESTION_ITEM_SCALE = 1.75F;
    private static final int UNAVAILABLE_SLOT_X = 166;
    private static final int UNAVAILABLE_SLOT_Y = 170;
    private static final int UNAVAILABLE_SLOT_SIZE = 206;
    private static final int SPEECH_X = 462;
    private static final int SPEECH_Y = 184;
    private static final int SPEECH_WIDTH = 828;
    private static final int SUGGESTION_SLOT_X = 544;
    private static final int SUGGESTION_SLOT_Y = 520;
    private static final int SUGGESTION_SLOT_WIDTH = 436;
    private static final int SUGGESTION_SLOT_HEIGHT = 334;
    private static final int CHECK_BUTTON_X = 204;
    private static final int CANCEL_BUTTON_X = 805;
    private static final int BUTTON_Y = 943;
    private static final int BUTTON_WIDTH = 522;
    private static final int BUTTON_HEIGHT = 104;
    private static final int BUTTON_LABEL_OFFSET_X = -18;
    private static final int BUTTON_LABEL_OFFSET_Y = -32;
    private static final int SUGGESTION_ITEM_OFFSET_X = -18;
    private static final int INVENTORY_GAP = 8;
    private static final int MIN_GUI_WIDTH = 260;

    private final int customerId;
    private final Component customerName;
    private final String orderSummary;
    private final List<ItemStack> orderItems;
    private final int orderStartIndex;
    private final int requestIndex;
    private final String unavailableItemName;
    private final ItemStack unavailableItem;
    private ItemStack suggestedItem = ItemStack.EMPTY;
    private int leftPos;
    private int topPos;
    private int guiWidth;
    private int guiHeight;
    private int inventoryX;
    private int inventoryY;
    private int checkX;
    private int cancelX;
    private int buttonY;
    private int buttonWidth;
    private int buttonHeight;

    public TakeoutNegotiationScreen(int customerId, Component customerName, String orderSummary, List<ItemStack> orderItems,
                                    int orderStartIndex, int requestIndex, String unavailableItemName) {
        super(Component.literal("Suggest Replacement"));
        this.customerId = customerId;
        this.customerName = customerName;
        this.orderSummary = orderSummary;
        this.orderItems = List.copyOf(orderItems);
        this.orderStartIndex = orderStartIndex;
        this.requestIndex = requestIndex;
        this.unavailableItemName = unavailableItemName;
        this.unavailableItem = FoodRegistryScanner.findFoodStack(unavailableItemName);
    }

    @Override
    protected void init() {
        this.guiHeight = Math.min(TEXTURE_HEIGHT, this.height - 20);
        this.guiWidth = this.guiHeight * TEXTURE_WIDTH / TEXTURE_HEIGHT;
        if (this.guiWidth > this.width - 20) {
            this.guiWidth = this.width - 20;
            this.guiHeight = this.guiWidth * TEXTURE_HEIGHT / TEXTURE_WIDTH;
        }

        boolean inventoryBesideGui = this.guiWidth + INVENTORY_GAP + InventoryPanelRenderer.PANEL_WIDTH <= this.width - 20;
        if (!inventoryBesideGui) {
            int maxGuiWidth = this.width - 20 - INVENTORY_GAP - InventoryPanelRenderer.PANEL_WIDTH;
            if (maxGuiWidth >= MIN_GUI_WIDTH) {
                this.guiWidth = Math.min(this.guiWidth, maxGuiWidth);
                this.guiHeight = this.guiWidth * TEXTURE_HEIGHT / TEXTURE_WIDTH;
                inventoryBesideGui = true;
            }
        }

        if (inventoryBesideGui) {
            int combinedWidth = this.guiWidth + INVENTORY_GAP + InventoryPanelRenderer.PANEL_WIDTH;
            this.leftPos = (this.width - combinedWidth) / 2;
            this.topPos = (this.height - this.guiHeight) / 2;
            this.inventoryX = this.leftPos + this.guiWidth + INVENTORY_GAP + InventoryPanelRenderer.PANEL_MARGIN_X;
            this.inventoryY = this.topPos + (this.guiHeight - InventoryPanelRenderer.PANEL_HEIGHT) / 2
                    + InventoryPanelRenderer.PANEL_MARGIN_Y;
        } else {
            int maxGuiHeight = this.height - 20 - INVENTORY_GAP - InventoryPanelRenderer.PANEL_HEIGHT;
            this.guiHeight = Math.min(this.guiHeight, maxGuiHeight);
            this.guiWidth = this.guiHeight * TEXTURE_WIDTH / TEXTURE_HEIGHT;
            int combinedHeight = this.guiHeight + INVENTORY_GAP + InventoryPanelRenderer.PANEL_HEIGHT;
            this.leftPos = (this.width - this.guiWidth) / 2;
            this.topPos = (this.height - combinedHeight) / 2;
            this.inventoryX = (this.width - InventoryPanelRenderer.PANEL_WIDTH) / 2
                    + InventoryPanelRenderer.PANEL_MARGIN_X;
            this.inventoryY = this.topPos + this.guiHeight + INVENTORY_GAP + InventoryPanelRenderer.PANEL_MARGIN_Y;
        }

        this.checkX = x(CHECK_BUTTON_X);
        this.cancelX = x(CANCEL_BUTTON_X);
        this.buttonY = y(BUTTON_Y);
        this.buttonWidth = w(BUTTON_WIDTH);
        this.buttonHeight = h(BUTTON_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.blit(BACKGROUND, leftPos, topPos, guiWidth, guiHeight,
                0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        renderUnavailableItem(graphics, mouseX, mouseY);
        drawWrapped(graphics, Component.literal(unavailableItemName + " is unavailable? I'll go with whatever you suggest."),
                x(SPEECH_X), y(SPEECH_Y), w(SPEECH_WIDTH), 0x3B2717, 5);
        renderSuggestionSlot(graphics, mouseX, mouseY);
        renderInventoryPanel(graphics, mouseX, mouseY);
        renderButton(graphics, checkX, buttonY, buttonWidth, buttonHeight, "Suggest", mouseX, mouseY);
        renderButton(graphics, cancelX, buttonY, buttonWidth, buttonHeight, "Skip Item", mouseX, mouseY);
        renderTooltipForHoveredInventory(graphics, mouseX, mouseY);
    }

    private void renderUnavailableItem(GuiGraphics graphics, int mouseX, int mouseY) {
        int slotX = x(UNAVAILABLE_SLOT_X);
        int slotY = y(UNAVAILABLE_SLOT_Y);
        int slotWidth = w(UNAVAILABLE_SLOT_SIZE);
        int slotHeight = h(UNAVAILABLE_SLOT_SIZE);
        if (!unavailableItem.isEmpty()) {
            renderCenteredItem(graphics, unavailableItem, slotX, slotY, slotWidth, slotHeight);
            if (isInside(mouseX, mouseY, slotX, slotY, slotWidth, slotHeight)) {
                graphics.renderTooltip(this.font, unavailableItem, mouseX, mouseY);
            }
        }
    }

    private void renderSuggestionSlot(GuiGraphics graphics, int mouseX, int mouseY) {
        int slotX = x(SUGGESTION_SLOT_X);
        int slotY = y(SUGGESTION_SLOT_Y);
        int slotWidth = w(SUGGESTION_SLOT_WIDTH);
        int slotHeight = h(SUGGESTION_SLOT_HEIGHT);
        if (!suggestedItem.isEmpty()) {
            renderCenteredScaledItem(graphics, suggestedItem, slotX, slotY, slotWidth, slotHeight, SUGGESTION_ITEM_SCALE);
            if (isInside(mouseX, mouseY, slotX, slotY, slotWidth, slotHeight)) {
                graphics.renderTooltip(this.font, suggestedItem, mouseX, mouseY);
            }
        }
    }

    private void renderCenteredItem(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height) {
        int itemX = x + (width - ITEM_RENDER_SIZE) / 2;
        int itemY = y + (height - ITEM_RENDER_SIZE) / 2;
        graphics.renderItem(stack, itemX, itemY);
        graphics.renderItemDecorations(this.font, stack, itemX, itemY);
    }

    private void renderCenteredScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height, float scale) {
        int scaledSize = Math.round(ITEM_RENDER_SIZE * scale);
        int itemX = x + (width - scaledSize) / 2 + w(SUGGESTION_ITEM_OFFSET_X);
        int itemY = y + (height - scaledSize) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(itemX, itemY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(this.font, stack, 0, 0);
        graphics.pose().popPose();
    }

    private void renderInventoryPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        InventoryPanelRenderer.drawPanel(graphics, inventoryX, inventoryY);
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        for (int slot = 0; slot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; slot++) {
            int x = InventoryPanelRenderer.slotX(inventoryX, slot);
            int y = InventoryPanelRenderer.slotY(inventoryY, slot);
            ItemStack stack = player.getInventory().getItem(InventoryPanelRenderer.inventoryIndexForDisplaySlot(slot));
            InventoryPanelRenderer.drawHighlight(graphics, x, y, !stack.isEmpty() && stack.isEdible());
            InventoryPanelRenderer.drawStack(graphics, this.font, stack, x, y);
            if (isInside(mouseX, mouseY, x, y, InventoryPanelRenderer.SLOT_SIZE, InventoryPanelRenderer.SLOT_SIZE)) {
                InventoryPanelRenderer.drawHover(graphics, x, y);
            }
        }
    }

    private void renderTooltipForHoveredInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        for (int slot = 0; slot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; slot++) {
            int x = InventoryPanelRenderer.slotX(inventoryX, slot);
            int y = InventoryPanelRenderer.slotY(inventoryY, slot);
            if (isInside(mouseX, mouseY, x, y, InventoryPanelRenderer.SLOT_SIZE, InventoryPanelRenderer.SLOT_SIZE)) {
                ItemStack stack = player.getInventory().getItem(InventoryPanelRenderer.inventoryIndexForDisplaySlot(slot));
                if (!stack.isEmpty()) {
                    graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
                return;
            }
        }
    }

    private void renderButton(GuiGraphics graphics, int x, int y, int width, int height, String label, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0x22FFFFFF);
        }
        int labelX = x + width / 2 + w(BUTTON_LABEL_OFFSET_X);
        int labelY = y + (height - this.font.lineHeight) / 2;
        labelY += h(BUTTON_LABEL_OFFSET_Y);
        graphics.drawCenteredString(this.font, Component.literal(label), labelX + 1, labelY + 1, 0x6B3F1D);
        graphics.drawCenteredString(this.font, Component.literal(label), labelX, labelY, 0xFFF2D2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (int slot = 0; slot < InventoryPanelRenderer.DISPLAY_SLOT_COUNT; slot++) {
                int x = InventoryPanelRenderer.slotX(inventoryX, slot);
                int y = InventoryPanelRenderer.slotY(inventoryY, slot);
                if (isInside(mouseX, mouseY, x, y, InventoryPanelRenderer.SLOT_SIZE, InventoryPanelRenderer.SLOT_SIZE)) {
                    ItemStack stack = player.getInventory().getItem(InventoryPanelRenderer.inventoryIndexForDisplaySlot(slot));
                    if (!stack.isEmpty() && stack.isEdible()) {
                        suggestedItem = stack.copyWithCount(1);
                    }
                    return true;
                }
            }
        }

        if (isInside(mouseX, mouseY, x(SUGGESTION_SLOT_X), y(SUGGESTION_SLOT_Y),
                w(SUGGESTION_SLOT_WIDTH), h(SUGGESTION_SLOT_HEIGHT))) {
            suggestedItem = ItemStack.EMPTY;
            return true;
        }

        if (isInside(mouseX, mouseY, checkX, buttonY, buttonWidth, buttonHeight)) {
            if (!suggestedItem.isEmpty()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(suggestedItem.getItem());
                ModNetwork.CHANNEL.sendToServer(new ServeCustomerPacket(customerId, ServeCustomerPacket.Action.REPLACE_ITEM,
                        id == null ? "" : id.toString(), requestIndex));
                Minecraft.getInstance().setScreen(createReturnScreen(true));
            }
            return true;
        }

        if (isInside(mouseX, mouseY, cancelX, buttonY, buttonWidth, buttonHeight)) {
            ModNetwork.CHANNEL.sendToServer(new ServeCustomerPacket(customerId,
                    ServeCustomerPacket.Action.CANCEL_REPLACE_ITEM, "", requestIndex));
            Minecraft.getInstance().setScreen(createReturnScreen(ItemStack.EMPTY));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private CustomerConversationScreen createReturnScreen(boolean includeReplacement) {
        return createReturnScreen(includeReplacement ? suggestedItem.copyWithCount(1) : ItemStack.EMPTY);
    }

    private CustomerConversationScreen createReturnScreen(ItemStack replacement) {
        List<ItemStack> nextOrderItems = new ArrayList<>(orderItems);
        int localIndex = requestIndex - orderStartIndex;
        if (localIndex >= 0 && localIndex < nextOrderItems.size()) {
            nextOrderItems.set(localIndex, replacement.isEmpty() ? ItemStack.EMPTY : replacement.copyWithCount(1));
        }
        return new CustomerConversationScreen(customerId, customerName, orderSummary, nextOrderItems, true, orderStartIndex, true);
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

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int x(int textureX) {
        return leftPos + textureX * guiWidth / TEXTURE_WIDTH;
    }

    private int y(int textureY) {
        return topPos + textureY * guiHeight / TEXTURE_HEIGHT;
    }

    private int w(int textureWidth) {
        return textureWidth * guiWidth / TEXTURE_WIDTH;
    }

    private int h(int textureHeight) {
        return textureHeight * guiHeight / TEXTURE_HEIGHT;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
