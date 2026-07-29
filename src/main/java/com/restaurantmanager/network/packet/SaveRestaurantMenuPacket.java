package com.restaurantmanager.network.packet;

import com.restaurantmanager.restaurant.MenuCategory;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.restaurant.RestaurantMenu;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SaveRestaurantMenuPacket {
    private final BlockPos signPos;
    private final String title;
    private final MenuCategory category;
    private final List<ResourceLocation> items;

    public SaveRestaurantMenuPacket(BlockPos signPos, String title, MenuCategory category, List<ResourceLocation> items) {
        this.signPos = signPos;
        this.title = title == null ? "" : title;
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
        this.items = List.copyOf(items);
    }

    public static void encode(SaveRestaurantMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeUtf(packet.title, 64);
        buffer.writeEnum(packet.category);
        buffer.writeInt(packet.items.size());
        for (ResourceLocation item : packet.items) {
            buffer.writeResourceLocation(item);
        }
    }

    public static SaveRestaurantMenuPacket decode(FriendlyByteBuf buffer) {
        BlockPos signPos = buffer.readBlockPos();
        String title = buffer.readUtf(64);
        MenuCategory category = buffer.readEnum(MenuCategory.class);
        int size = buffer.readInt();
        List<ResourceLocation> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(buffer.readResourceLocation());
        }
        return new SaveRestaurantMenuPacket(signPos, title, category, items);
    }

    public static void handle(SaveRestaurantMenuPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

            List<ResourceLocation> edibleItems = new ArrayList<>();
            for (ResourceLocation itemId : packet.items) {
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item != null && new ItemStack(item).isEdible() && !edibleItems.contains(itemId)) {
                    edibleItems.add(itemId);
                }
            }
            restaurant.addOrReplaceMenu(new RestaurantMenu(packet.title, packet.category, edibleItems));
            RestaurantManager.get(level).setDirty();
        });
        context.setPacketHandled(true);
    }
}
