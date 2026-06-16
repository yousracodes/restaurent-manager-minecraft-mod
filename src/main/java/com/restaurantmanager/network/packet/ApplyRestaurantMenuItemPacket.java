package com.restaurantmanager.network.packet;

import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.restaurant.MenuCategory;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantMenu;
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
    private final MenuCategory category;

    public ApplyRestaurantMenuItemPacket(BlockPos signPos, int slot) {
        this(signPos, slot, null);
    }

    public ApplyRestaurantMenuItemPacket(BlockPos signPos, int slot, MenuCategory category) {
        this.signPos = signPos;
        this.slot = slot;
        this.category = category;
    }

    public static void encode(ApplyRestaurantMenuItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeInt(packet.slot);
        buffer.writeBoolean(packet.category != null);
        if (packet.category != null) {
            buffer.writeEnum(packet.category);
        }
    }

    public static ApplyRestaurantMenuItemPacket decode(FriendlyByteBuf buffer) {
        BlockPos signPos = buffer.readBlockPos();
        int slot = buffer.readInt();
        MenuCategory category = buffer.readBoolean() ? buffer.readEnum(MenuCategory.class) : null;
        return new ApplyRestaurantMenuItemPacket(signPos, slot, category);
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
            MenuCategory stackCategory = RestaurantMenuItem.getMenuCategory(stack);
            if (packet.category != null && stackCategory != packet.category) {
                player.sendSystemMessage(Component.literal("That menu belongs in the "
                        + stackCategory.getDisplayName() + " slot."));
                return;
            }

            RestaurantManager manager = RestaurantManager.get(level);
            Restaurant restaurant = manager.findBySignPos(packet.signPos).orElse(null);
            if (restaurant == null) {
                return;
            }

            RestaurantMenu menu = new RestaurantMenu(
                    RestaurantMenuItem.getMenuTitle(stack),
                    RestaurantMenuItem.getMenuCategory(stack),
                    RestaurantMenuItem.getMenuItems(stack)
            );
            restaurant.addOrReplaceMenu(menu);
            manager.setDirty();
            player.sendSystemMessage(Component.literal("Restaurant menu applied: "
                    + menu.getCategory().getDisplayName() + " with " + menu.getItems().size() + " item(s)."));
        });
        context.setPacketHandled(true);
    }
}
