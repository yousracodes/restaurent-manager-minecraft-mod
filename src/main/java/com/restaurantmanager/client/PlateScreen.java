package com.restaurantmanager.client;

import com.restaurantmanager.menu.PlateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PlateScreen extends AbstractContainerScreen<PlateMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("restaurantmanager", "textures/gui/plate.png");

    public PlateScreen(PlateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 244;
        titleLabelX = 48;
        titleLabelY = 12;
        inventoryLabelX = 48;
        inventoryLabelY = 140;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 256, 144, 256, 144);
        InventoryPanelRenderer.drawPanel(graphics, leftPos + 48, topPos + 153);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
