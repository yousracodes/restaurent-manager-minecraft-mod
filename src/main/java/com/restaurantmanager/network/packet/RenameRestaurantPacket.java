package com.restaurantmanager.network.packet;

import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RenameRestaurantPacket {
    private final BlockPos signPos;
    private final String name;

    public RenameRestaurantPacket(BlockPos signPos, String name) {
        this.signPos = signPos;
        this.name = name;
    }

    public static void encode(RenameRestaurantPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeUtf(packet.name, 64);
    }

    public static RenameRestaurantPacket decode(FriendlyByteBuf buffer) {
        return new RenameRestaurantPacket(buffer.readBlockPos(), buffer.readUtf(64));
    }

    public static void handle(RenameRestaurantPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
            restaurant.setName(packet.name);
            manager.setDirty();
        });
        context.setPacketHandled(true);
    }
}
