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
        SUGGEST,
        UNAVAILABLE_ITEM,
        LOSE_REQUEST,
        REPLACE_ITEM,
        CANCEL_REPLACE_ITEM
    }

    private final int customerId;
    private final Action action;
    private final String suggestion;
    private final int requestIndex;

    public ServeCustomerPacket(int customerId, Action action, String suggestion) {
        this(customerId, action, suggestion, -1);
    }

    public ServeCustomerPacket(int customerId, Action action, String suggestion, int requestIndex) {
        this.customerId = customerId;
        this.action = action;
        this.suggestion = suggestion;
        this.requestIndex = requestIndex;
    }

    public static void encode(ServeCustomerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.customerId);
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.suggestion, 128);
        buffer.writeInt(packet.requestIndex);
    }

    public static ServeCustomerPacket decode(FriendlyByteBuf buffer) {
        return new ServeCustomerPacket(buffer.readInt(), buffer.readEnum(Action.class), buffer.readUtf(128), buffer.readInt());
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
                    case SERVE -> {
                        if (customer.isTakeoutCustomer()) {
                            customer.serveTakeoutFromHeldPackage(player);
                        } else {
                            customer.serveFromInventory(player);
                        }
                    }
                    case UNAVAILABLE -> customer.markCurrentRequestUnavailable(player);
                    case SUGGEST -> customer.serveSuggestionFromInventory(player, packet.suggestion);
                    case UNAVAILABLE_ITEM -> customer.startUnavailableNegotiation(player, packet.requestIndex);
                    case LOSE_REQUEST -> customer.loseRequest(player);
                    case REPLACE_ITEM -> customer.replaceTakeoutRequest(player, packet.requestIndex, packet.suggestion);
                    case CANCEL_REPLACE_ITEM -> customer.cancelTakeoutRequestReplacement(player, packet.requestIndex);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
