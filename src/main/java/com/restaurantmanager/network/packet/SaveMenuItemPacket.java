package com.restaurantmanager.network.packet;

import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.restaurant.MenuCategory;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SaveMenuItemPacket {
    private final InteractionHand hand;
    private final String title;
    private final MenuCategory category;
    private final List<ResourceLocation> items;

    public SaveMenuItemPacket(InteractionHand hand, String title, MenuCategory category, List<ResourceLocation> items) {
        this.hand = hand;
        this.title = title;
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
        this.items = List.copyOf(items);
    }

    public static void encode(SaveMenuItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeUtf(packet.title, 64);
        buffer.writeEnum(packet.category);
        buffer.writeInt(packet.items.size());
        for (ResourceLocation item : packet.items) {
            buffer.writeResourceLocation(item);
        }
    }

    public static SaveMenuItemPacket decode(FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        String title = buffer.readUtf(64);
        MenuCategory category = buffer.readEnum(MenuCategory.class);
        int size = buffer.readInt();
        List<ResourceLocation> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(buffer.readResourceLocation());
        }
        return new SaveMenuItemPacket(hand, title, category, items);
    }

    public static void handle(SaveMenuItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ItemStack stack = player.getItemInHand(packet.hand);
            if (!stack.is(ModItems.RESTAURANT_MENU.get())) {
                return;
            }

            List<ResourceLocation> edibleItems = new ArrayList<>();
            for (ResourceLocation itemId : packet.items) {
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item != null && new ItemStack(item).isEdible() && !edibleItems.contains(itemId)) {
                    edibleItems.add(itemId);
                }
            }
            RestaurantMenuItem.saveMenu(stack, packet.title, packet.category, edibleItems);
        });
        context.setPacketHandled(true);
    }
}
