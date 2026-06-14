package com.restaurantmanager.network.packet;

import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class UpdateRestaurantMenuPacket {
    public enum Action {
        ADD,
        REMOVE
    }

    private final BlockPos signPos;
    private final ResourceLocation itemId;
    private final Action action;

    public UpdateRestaurantMenuPacket(BlockPos signPos, ResourceLocation itemId, Action action) {
        this.signPos = signPos;
        this.itemId = itemId;
        this.action = action;
    }

    public static void encode(UpdateRestaurantMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeResourceLocation(packet.itemId);
        buffer.writeEnum(packet.action);
    }

    public static UpdateRestaurantMenuPacket decode(FriendlyByteBuf buffer) {
        return new UpdateRestaurantMenuPacket(buffer.readBlockPos(), buffer.readResourceLocation(), buffer.readEnum(Action.class));
    }

    public static void handle(UpdateRestaurantMenuPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            if (player.distanceToSqr(packet.signPos.getX() + 0.5, packet.signPos.getY() + 0.5, packet.signPos.getZ() + 0.5) > 64.0) {
                return;
            }

            Restaurant restaurant = RestaurantManager.get(level).findBySignPos(packet.signPos).orElse(null);
            if (restaurant == null) {
                player.sendSystemMessage(Component.literal("No registered restaurant found for this sign."));
                return;
            }

            if (packet.action == Action.ADD) {
                Item item = ForgeRegistries.ITEMS.getValue(packet.itemId);
                if (item == null || !new ItemStack(item).isEdible()) {
                    player.sendSystemMessage(Component.literal("Only edible items can be added to the menu."));
                    return;
                }
                restaurant.getMenu().addItem(packet.itemId);
            } else {
                restaurant.getMenu().removeItem(packet.itemId);
            }

            RestaurantManager.get(level).setDirty();
        });
        context.setPacketHandled(true);
    }
}
