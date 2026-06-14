package com.restaurantmanager.client;

import com.restaurantmanager.entity.CustomerEntity;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.ServeCustomerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class CustomerConversationScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("restaurantmanager", "textures/gui/customer_conversation.png");
    private static final int TEXTURE_WIDTH = 1402;
    private static final int TEXTURE_HEIGHT = 1122;

    private final int customerId;
    private final Component customerName;
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

    public CustomerConversationScreen(CustomerEntity customer) {
        this(customer.getId(), customer.getDisplayName());
    }

    public CustomerConversationScreen(int customerId, Component customerName) {
        super(Component.literal("Customer"));
        this.customerId = customerId;
        this.customerName = customerName;
    }

    public CustomerConversationScreen(int customerId, Component customerName, ItemStack suggestedItem) {
        this(customerId, customerName);
        this.suggestedItem = suggestedItem.copyWithCount(1);
        this.suggestionControlsVisible = true;
    }

    @Override
    protected void init() {
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
        drawWrapped(graphics, Component.literal(customerName.getString()), orderX, orderY + 12, orderWidth, textColor, 2);

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

    private void showSuggestionControls() {
        this.suggestionControlsVisible = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
