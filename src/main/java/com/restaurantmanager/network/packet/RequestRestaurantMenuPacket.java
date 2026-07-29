package com.restaurantmanager.network.packet;

import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.restaurant.MenuCategory;
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
    private final MenuCategory category;

    public RequestRestaurantMenuPacket(BlockPos signPos) {
        this(signPos, MenuCategory.MAIN_DISH);
    }

    public RequestRestaurantMenuPacket(BlockPos signPos, MenuCategory category) {
        this.signPos = signPos;
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
    }

    public static void encode(RequestRestaurantMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeEnum(packet.category);
    }

    public static RequestRestaurantMenuPacket decode(FriendlyByteBuf buffer) {
        return new RequestRestaurantMenuPacket(buffer.readBlockPos(), buffer.readEnum(MenuCategory.class));
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
            com.restaurantmanager.restaurant.RestaurantMenu menu = restaurant.getOrCreateMenu(packet.category);
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenRestaurantMenuEditorPacket(packet.signPos, menu.getTitle(), packet.category, menu.getItems())
            );
        });
        context.setPacketHandled(true);
    }
}
