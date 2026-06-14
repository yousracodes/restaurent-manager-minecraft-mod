package com.restaurantmanager.network.packet;

import com.restaurantmanager.client.ClientRestaurantScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenRestaurantMenuPacket {
    private final BlockPos signPos;
    private final List<ResourceLocation> items;

    public OpenRestaurantMenuPacket(BlockPos signPos, List<ResourceLocation> items) {
        this.signPos = signPos;
        this.items = List.copyOf(items);
    }

    public static void encode(OpenRestaurantMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeInt(packet.items.size());
        for (ResourceLocation item : packet.items) {
            buffer.writeResourceLocation(item);
        }
    }

    public static OpenRestaurantMenuPacket decode(FriendlyByteBuf buffer) {
        BlockPos signPos = buffer.readBlockPos();
        int size = buffer.readInt();
        List<ResourceLocation> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(buffer.readResourceLocation());
        }
        return new OpenRestaurantMenuPacket(signPos, items);
    }

    public static void handle(OpenRestaurantMenuPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientRestaurantScreens.openMenu(packet.signPos, packet.items)));
        context.setPacketHandled(true);
    }
}
