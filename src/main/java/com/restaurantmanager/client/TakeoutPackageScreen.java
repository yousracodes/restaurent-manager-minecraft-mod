package com.restaurantmanager.client;

import com.restaurantmanager.menu.TakeoutPackageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TakeoutPackageScreen extends AbstractContainerScreen<TakeoutPackageMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("restaurantmanager", "textures/gui/takeout_package.png");

    public TakeoutPackageScreen(TakeoutPackageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 230;
        titleLabelX = 48;
        titleLabelY = 12;
        inventoryLabelX = 48;
        inventoryLabelY = 132;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 256, 144, 256, 144);
        InventoryPanelRenderer.drawPanel(graphics, leftPos + 48, topPos + 145);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
