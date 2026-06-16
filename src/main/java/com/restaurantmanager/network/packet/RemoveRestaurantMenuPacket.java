package com.restaurantmanager.network.packet;

import com.restaurantmanager.restaurant.MenuCategory;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RemoveRestaurantMenuPacket {
    private final BlockPos signPos;
    private final MenuCategory category;

    public RemoveRestaurantMenuPacket(BlockPos signPos, MenuCategory category) {
        this.signPos = signPos;
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
    }

    public static void encode(RemoveRestaurantMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeEnum(packet.category);
    }

    public static RemoveRestaurantMenuPacket decode(FriendlyByteBuf buffer) {
        return new RemoveRestaurantMenuPacket(buffer.readBlockPos(), buffer.readEnum(MenuCategory.class));
    }

    public static void handle(RemoveRestaurantMenuPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            if (player.distanceToSqr(packet.signPos.getX() + 0.5, packet.signPos.getY() + 0.5, packet.signPos.getZ() + 0.5) > 64.0) {
                return;
            }

            RestaurantManager manager = RestaurantManager.get(level);
            Restaurant restaurant = manager.findBySignPos(packet.signPos).orElse(null);
            if (restaurant == null) {
                return;
            }

            if (restaurant.removeMenu(packet.category)) {
                manager.setDirty();
                player.sendSystemMessage(Component.literal(packet.category.getDisplayName() + " menu removed."));
            }
        });
        context.setPacketHandled(true);
    }
}
