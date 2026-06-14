package com.restaurantmanager.network.packet;

import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class RequestRestaurantMenuPacket {
    private final BlockPos signPos;

    public RequestRestaurantMenuPacket(BlockPos signPos) {
        this.signPos = signPos;
    }

    public static void encode(RequestRestaurantMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
    }

    public static RequestRestaurantMenuPacket decode(FriendlyByteBuf buffer) {
        return new RequestRestaurantMenuPacket(buffer.readBlockPos());
    }

    public static void handle(RequestRestaurantMenuPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            RestaurantManager manager = RestaurantManager.get(level);
            Restaurant restaurant = manager.findBySignPos(packet.signPos).orElse(null);
            if (restaurant == null) {
                player.sendSystemMessage(Component.literal("No registered restaurant found for this sign."));
                return;
            }
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenRestaurantMenuPacket(packet.signPos, restaurant.getMenu().getItems())
            );
        });
        context.setPacketHandled(true);
    }
}
