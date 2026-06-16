package com.restaurantmanager.seating;

import com.restaurantmanager.setup.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SeatingDebugEvents {
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            SeatingMarkSavedData data = SeatingMarkSavedData.get(level);
            for (ServerPlayer player : level.players()) {
                if (!isHoldingMarkTool(player.getMainHandItem()) && !isHoldingMarkTool(player.getOffhandItem())) {
                    continue;
                }
                for (MarkedPoint point : data.getAllPoints()) {
                    if (point.getPos().distSqr(player.blockPosition()) > 24.0 * 24.0) {
                        continue;
                    }
                    if (point.getType() == MarkType.SEAT) {
                        level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true,
                                point.getInteractionPosition().x, point.getInteractionPosition().y + 0.15, point.getInteractionPosition().z,
                                1, 0.05, 0.05, 0.05, 0.0);
                    } else {
                        level.sendParticles(player, ParticleTypes.ENCHANT, true,
                                point.getPos().getX() + 0.5, point.getPos().getY() + 1.05, point.getPos().getZ() + 0.5,
                                1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
            }
        }
    }

    private boolean isHoldingMarkTool(ItemStack stack) {
        return stack.is(ModItems.TABLE_LINK_TOOL.get());
    }
}
