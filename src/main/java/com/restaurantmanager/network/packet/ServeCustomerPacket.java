package com.restaurantmanager.network.packet;

import com.restaurantmanager.entity.CustomerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServeCustomerPacket {
    public enum Action {
        SERVE,
        UNAVAILABLE,
        SUGGEST
    }

    private final int customerId;
    private final Action action;
    private final String suggestion;

    public ServeCustomerPacket(int customerId, Action action, String suggestion) {
        this.customerId = customerId;
        this.action = action;
        this.suggestion = suggestion;
    }

    public static void encode(ServeCustomerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.customerId);
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.suggestion, 128);
    }

    public static ServeCustomerPacket decode(FriendlyByteBuf buffer) {
        return new ServeCustomerPacket(buffer.readInt(), buffer.readEnum(Action.class), buffer.readUtf(128));
    }

    public static void handle(ServeCustomerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.level() == null) {
                return;
            }

            Entity entity = player.level().getEntity(packet.customerId);
            if (entity instanceof CustomerEntity customer && player.distanceToSqr(customer) <= 36.0) {
                switch (packet.action) {
                    case SERVE -> customer.serveFromInventory(player);
                    case UNAVAILABLE -> customer.markCurrentRequestUnavailable(player);
                    case SUGGEST -> customer.serveSuggestionFromInventory(player, packet.suggestion);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
