package com.restaurantmanager.block;

import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.setup.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class RestaurantSignProtectionEvents {
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(ModBlocks.RESTAURANT_SIGN.get()) || !(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof Level level)) {
            return;
        }

        Optional<Restaurant> restaurant = RestaurantSignBlock.getAnchoredRestaurant(level, event.getPos());
        if (restaurant.isPresent() && !restaurant.get().isOwner(player.getUUID())) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("Only the restaurant owner can remove this sign."));
        }
    }
}
