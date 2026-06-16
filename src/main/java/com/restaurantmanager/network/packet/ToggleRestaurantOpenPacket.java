package com.restaurantmanager.network.packet;

import com.restaurantmanager.entity.CustomerEntity;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.setup.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class ToggleRestaurantOpenPacket {
    private final BlockPos signPos;
    private final boolean open;

    public ToggleRestaurantOpenPacket(BlockPos signPos, boolean open) {
        this.signPos = signPos;
        this.open = open;
    }

    public static void encode(ToggleRestaurantOpenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeBoolean(packet.open);
    }

    public static ToggleRestaurantOpenPacket decode(FriendlyByteBuf buffer) {
        return new ToggleRestaurantOpenPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(ToggleRestaurantOpenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
            Optional<Restaurant> restaurant = manager.findBySignPos(packet.signPos);
            if (restaurant.isEmpty()) {
                player.sendSystemMessage(Component.literal("No registered restaurant found for this sign."));
                return;
            }

            Restaurant value = restaurant.get();
            value.setOpen(packet.open);
            int leavingCustomers = packet.open ? 0 : makeAssignedCustomersLeave(level, value);
            manager.setDirty();
            player.sendSystemMessage(Component.literal("\"" + value.getName() + "\" restaurant is "
                    + (packet.open ? "opened." : "closed. " + leavingCustomers + " customer(s) leaving.")));
        });
        context.setPacketHandled(true);
    }

    private static int makeAssignedCustomersLeave(ServerLevel level, Restaurant restaurant) {
        AABB searchBounds = restaurant.getBounds().inflate(128.0);
        int count = 0;
        for (CustomerEntity customer : level.getEntities(
                ModEntities.CUSTOMER.get(),
                searchBounds,
                customer -> restaurant.getId().equals(customer.getRestaurantId())
        )) {
            customer.leaveBecauseRestaurantClosed();
            count++;
        }
        return count;
    }
}
