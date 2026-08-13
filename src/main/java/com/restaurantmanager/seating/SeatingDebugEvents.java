package com.restaurantmanager.seating;

import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.restaurant.SeatTableLink;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SeatingDebugEvents {
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 10 != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            SeatingMarkSavedData data = SeatingMarkSavedData.get(level);
            RestaurantManager manager = RestaurantManager.get(level);
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
                for (Restaurant restaurant : manager.getRestaurants()) {
                    if (!restaurant.isActive()) {
                        continue;
                    }
                    for (SeatTableLink link : restaurant.getSeatTableLinks()) {
                        if (isNearby(player.blockPosition(), link.getChairPos()) || isNearby(player.blockPosition(), link.getTablePos())) {
                            showLinkedPair(level, player, link);
                        }
                    }
                }
            }
        }
    }

    private boolean isHoldingMarkTool(ItemStack stack) {
        return stack.is(ModItems.TABLE_LINK_TOOL.get());
    }

    private boolean isNearby(BlockPos playerPos, BlockPos markPos) {
        return markPos.distSqr(playerPos) <= 24.0 * 24.0;
    }

    private void showLinkedPair(ServerLevel level, ServerPlayer player, SeatTableLink link) {
        Vec3 chair = Vec3.atCenterOf(link.getChairPos()).add(0.0, 0.35, 0.0);
        Vec3 table = Vec3.atCenterOf(link.getTablePos()).add(0.0, 0.7, 0.0);
        level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true,
                chair.x, chair.y, chair.z, 2, 0.08, 0.08, 0.08, 0.0);
        level.sendParticles(player, ParticleTypes.ENCHANT, true,
                table.x, table.y, table.z, 2, 0.1, 0.1, 0.1, 0.0);

        Vec3 delta = table.subtract(chair);
        int steps = Math.max(2, (int) (delta.length() * 2.0));
        for (int i = 1; i < steps; i++) {
            Vec3 point = chair.add(delta.scale(i / (double) steps));
            level.sendParticles(player, ParticleTypes.END_ROD, true,
                    point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }
}
