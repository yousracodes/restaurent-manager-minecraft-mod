package com.restaurantmanager.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.restaurantmanager.RestaurantManagerMod;
import com.restaurantmanager.network.packet.SeatingOutlinesPacket;
import com.restaurantmanager.seating.SeatingDebugEvents;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RestaurantManagerMod.MODID, value = Dist.CLIENT)
public final class SeatingOutlines {
    // Okabe-Ito blue and vermillion remain distinguishable with common red-green
    // color-vision deficiencies.
    private static final float CHAIR_RED = 0x00 / 255.0F;
    private static final float CHAIR_GREEN = 0x72 / 255.0F;
    private static final float CHAIR_BLUE = 0xB2 / 255.0F;
    private static final float TABLE_RED = 0xD5 / 255.0F;
    private static final float TABLE_GREEN = 0x5E / 255.0F;
    private static final float TABLE_BLUE = 0x00 / 255.0F;

    private static ClientLevel snapshotLevel;
    private static SeatingOutlinesPacket snapshot;

    private SeatingOutlines() {
    }

    public static void accept(SeatingOutlinesPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.dimension().location().equals(packet.dimension())) {
            snapshotLevel = minecraft.level;
            snapshot = packet;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level != snapshotLevel) {
            snapshotLevel = null;
            snapshot = null;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        ItemStack tool = heldTool(minecraft);
        if (tool.isEmpty()) {
            return;
        }
        BlockPos selected = tool.hasTag() && tool.getTag().contains("SelectedSeat", Tag.TAG_COMPOUND)
                ? NbtUtils.readBlockPos(tool.getTag().getCompound("SelectedSeat")) : null;
        BlockPos aimed = minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos() : null;
        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(SeatingOutlineRenderType.OUTLINES);
        if (snapshot != null && snapshotLevel == minecraft.level) {
            for (BlockPos chair : snapshot.chairs()) {
                outline(minecraft, poses, lines, chair, true);
            }
            for (BlockPos table : snapshot.tables()) {
                outline(minecraft, poses, lines, table, false);
            }
            if (aimed != null) {
                for (var link : snapshot.links()) {
                    if (aimed.equals(link.chair()) || aimed.equals(link.table())) {
                        connector(poses, lines, Vec3.atCenterOf(link.chair()), Vec3.atCenterOf(link.table()));
                    }
                }
            }
        }
        if (selected != null) {
            outline(minecraft, poses, lines, selected, true);
        }
        if (aimed != null) {
            boolean chair = selected == null || aimed.equals(selected);
            // Existing markers retain their actual color when aimed at.
            if (snapshot != null && snapshotLevel == minecraft.level) {
                if (snapshot.chairs().contains(aimed)) {
                    chair = true;
                } else if (snapshot.tables().contains(aimed)) {
                    chair = false;
                }
            }
            outline(minecraft, poses, lines, aimed, chair);
            if (selected != null && !selected.equals(aimed)
                    && selected.distSqr(minecraft.player.blockPosition()) <= SeatingDebugEvents.RANGE * SeatingDebugEvents.RANGE
                    && minecraft.level.hasChunkAt(selected)) {
                connector(poses, lines, Vec3.atCenterOf(selected), Vec3.atCenterOf(aimed));
            }
        }
        buffers.endBatch(SeatingOutlineRenderType.OUTLINES);
        poses.popPose();
    }

    private static ItemStack heldTool(Minecraft minecraft) {
        ItemStack main = minecraft.player.getMainHandItem();
        if (main.is(ModItems.TABLE_LINK_TOOL.get())) {
            return main;
        }
        ItemStack off = minecraft.player.getOffhandItem();
        return off.is(ModItems.TABLE_LINK_TOOL.get()) ? off : ItemStack.EMPTY;
    }

    private static void outline(Minecraft minecraft, PoseStack poses, VertexConsumer lines, BlockPos pos, boolean chair) {
        if (pos.distSqr(minecraft.player.blockPosition()) > SeatingDebugEvents.RANGE * SeatingDebugEvents.RANGE
                || !minecraft.level.hasChunkAt(pos) || minecraft.level.getBlockState(pos).isAir()) {
            return;
        }
        var shape = minecraft.level.getBlockState(pos).getShape(minecraft.level, pos, CollisionContext.of(minecraft.player));
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }
        float red = chair ? CHAIR_RED : TABLE_RED;
        float green = chair ? CHAIR_GREEN : TABLE_GREEN;
        float blue = chair ? CHAIR_BLUE : TABLE_BLUE;
        poses.pushPose();
        poses.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        poses.scale(1.004F, 1.004F, 1.004F);
        poses.translate(-0.5, -0.5, -0.5);
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) ->
                line(poses, lines, x1, y1, z1, x2, y2, z2, red, green, blue, 1.0F));
        poses.popPose();
    }

    private static void connector(PoseStack poses, VertexConsumer lines, Vec3 chair, Vec3 table) {
        line(poses, lines, chair.x, chair.y + 0.55, chair.z, table.x, table.y + 0.55, table.z,
                1.0F, 0.9F, 0.5F, 0.8F);
    }

    private static void line(PoseStack poses, VertexConsumer lines, double x1, double y1, double z1,
                             double x2, double y2, double z2, float red, float green, float blue, float alpha) {
        double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        if (length < 1.0E-6) {
            return;
        }
        float nx = (float) ((x2 - x1) / length);
        float ny = (float) ((y2 - y1) / length);
        float nz = (float) ((z2 - z1) / length);
        PoseStack.Pose pose = poses.last();
        lines.vertex(pose.pose(), (float) x1, (float) y1, (float) z1).color(red, green, blue, alpha)
                .normal(pose.normal(), nx, ny, nz).endVertex();
        lines.vertex(pose.pose(), (float) x2, (float) y2, (float) z2).color(red, green, blue, alpha)
                .normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
