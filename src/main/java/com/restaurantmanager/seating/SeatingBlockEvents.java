package com.restaurantmanager.seating;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Keeps persisted seating data in sync when a marked block is destroyed. */
public class SeatingBlockEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            RestaurantSeatingApi.clearSeatTableGroupAt(event.getPos(), level);
        }
    }
}
