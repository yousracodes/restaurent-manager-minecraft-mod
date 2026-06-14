package com.restaurantmanager.network.packet;

import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ApplyRestaurantMenuItemPacket {
    private final BlockPos signPos;
    private final int slot;

    public ApplyRestaurantMenuItemPacket(BlockPos signPos, int slot) {
        this.signPos = signPos;
        this.slot = slot;
    }

    public static void encode(ApplyRestaurantMenuItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeInt(packet.slot);
    }

    public static ApplyRestaurantMenuItemPacket decode(FriendlyByteBuf buffer) {
        return new ApplyRestaurantMenuItemPacket(buffer.readBlockPos(), buffer.readInt());
    }

    public static void handle(ApplyRestaurantMenuItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            if (player.distanceToSqr(packet.signPos.getX() + 0.5, packet.signPos.getY() + 0.5, packet.signPos.getZ() + 0.5) > 64.0) {
                return;
            }
            if (packet.slot < 0 || packet.slot >= player.getInventory().getContainerSize()) {
                return;
            }

            ItemStack stack = player.getInventory().getItem(packet.slot);
            if (!stack.is(ModItems.RESTAURANT_MENU.get())) {
                return;
            }

            RestaurantManager manager = RestaurantManager.get(level);
            Restaurant restaurant = manager.findBySignPos(packet.signPos).orElse(null);
            if (restaurant == null) {
                return;
            }

            restaurant.getMenu().setTitle(RestaurantMenuItem.getMenuTitle(stack));
            restaurant.getMenu().replaceItems(RestaurantMenuItem.getMenuItems(stack));
            manager.setDirty();
            player.sendSystemMessage(Component.literal("Restaurant menu applied: "
                    + restaurant.getMenu().getItems().size() + " item(s)."));
        });
        context.setPacketHandled(true);
    }
}
