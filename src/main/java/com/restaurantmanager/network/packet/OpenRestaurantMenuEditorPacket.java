package com.restaurantmanager.network.packet;

import com.restaurantmanager.client.ClientRestaurantScreens;
import com.restaurantmanager.restaurant.MenuCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenRestaurantMenuEditorPacket {
    private final BlockPos signPos;
    private final String title;
    private final MenuCategory category;
    private final List<ResourceLocation> items;

    public OpenRestaurantMenuEditorPacket(BlockPos signPos, String title, MenuCategory category, List<ResourceLocation> items) {
        this.signPos = signPos;
        this.title = title == null ? "" : title;
        this.category = category == null ? MenuCategory.MAIN_DISH : category;
        this.items = List.copyOf(items);
    }

    public static void encode(OpenRestaurantMenuEditorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeUtf(packet.title, 64);
        buffer.writeEnum(packet.category);
        buffer.writeInt(packet.items.size());
        for (ResourceLocation item : packet.items) {
            buffer.writeResourceLocation(item);
        }
    }

    public static OpenRestaurantMenuEditorPacket decode(FriendlyByteBuf buffer) {
        BlockPos signPos = buffer.readBlockPos();
        String title = buffer.readUtf(64);
        MenuCategory category = buffer.readEnum(MenuCategory.class);
        int size = buffer.readInt();
        List<ResourceLocation> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(buffer.readResourceLocation());
        }
        return new OpenRestaurantMenuEditorPacket(signPos, title, category, items);
    }

    public static void handle(OpenRestaurantMenuEditorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientRestaurantScreens.openRestaurantMenuEditor(
                        packet.signPos, packet.title, packet.category, packet.items)));
        context.setPacketHandled(true);
    }
}
