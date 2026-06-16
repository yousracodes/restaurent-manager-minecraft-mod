package com.restaurantmanager.item;

import com.restaurantmanager.seating.RestaurantSeatingApi;
import com.restaurantmanager.seating.SeatingMarkSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class CounterToolItem extends Item {
    public CounterToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        if (SeatingMarkSavedData.get(level).isCounter(context.getClickedPos())) {
            RestaurantSeatingApi.unmarkCounter(context.getClickedPos(), level);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("Counter spot removed."), true);
            }
        } else {
            RestaurantSeatingApi.markCounter(context.getClickedPos(), level);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("Counter spot marked."), true);
            }
            level.sendParticles(ParticleTypes.WAX_ON, context.getClickedPos().getX() + 0.5,
                    context.getClickedPos().getY() + 1.0, context.getClickedPos().getZ() + 0.5, 10,
                    0.25, 0.25, 0.25, 0.0);
        }
        return InteractionResult.CONSUME;
    }
}
