package com.restaurantmanager.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.restaurantmanager.block.PlateBlock;
import com.restaurantmanager.block.PlateBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.List;

public class PlateBlockEntityRenderer implements BlockEntityRenderer<PlateBlockEntity> {
    private static final double[][] SLOT_POSITIONS = {
            {0.33, 0.18, 0.38, -18.0},
            {0.50, 0.19, 0.52, 6.0},
            {0.67, 0.18, 0.38, 22.0}
    };

    private final ItemRenderer itemRenderer;

    public PlateBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PlateBlockEntity plate, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (plate.getBlockState().hasProperty(PlateBlock.FILLED)
                && !plate.getBlockState().getValue(PlateBlock.FILLED)) {
            return;
        }

        List<ItemStack> meals = plate.getMeals();
        for (int slot = 0; slot < meals.size() && slot < SLOT_POSITIONS.length; slot++) {
            ItemStack meal = meals.get(slot);
            if (!meal.isEmpty()) {
                renderMeal(plate, meal, slot, poseStack, buffer, packedLight);
            }
        }
    }

    private void renderMeal(PlateBlockEntity plate, ItemStack meal, int slot, PoseStack poseStack,
                            MultiBufferSource buffer, int packedLight) {
        double[] position = SLOT_POSITIONS[slot];
        poseStack.pushPose();
        poseStack.translate(position[0], position[1], position[2]);
        poseStack.mulPose(new Quaternionf().rotationXYZ((float) Math.toRadians(90.0), 0.0F, (float) Math.toRadians(position[3])));
        poseStack.scale(0.32F, 0.32F, 0.32F);
        itemRenderer.renderStatic(
                meal,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                plate.getLevel() == null ? Minecraft.getInstance().level : plate.getLevel(),
                slot
        );
        poseStack.popPose();
    }
}
