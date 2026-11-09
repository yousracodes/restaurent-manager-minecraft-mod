package com.restaurantmanager.network.packet;

import com.restaurantmanager.client.SeatingOutlines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** A dimension-specific snapshot sent only to players holding the link tool. */
public record SeatingOutlinesPacket(ResourceLocation dimension, List<BlockPos> chairs,
                                   List<BlockPos> tables, List<Link> links) {
    public static final int MAX_ENTRIES = 4096;

    public SeatingOutlinesPacket {
        chairs = List.copyOf(chairs);
        tables = List.copyOf(tables);
        links = List.copyOf(links);
    }

    public record Link(BlockPos chair, BlockPos table) {
        public Link {
            chair = chair.immutable();
            table = table.immutable();
        }
    }

    public static void encode(SeatingOutlinesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeCollection(packet.chairs, FriendlyByteBuf::writeBlockPos);
        buffer.writeCollection(packet.tables, FriendlyByteBuf::writeBlockPos);
        buffer.writeVarInt(packet.links.size());
        for (Link link : packet.links) {
            buffer.writeBlockPos(link.chair);
            buffer.writeBlockPos(link.table);
        }
    }

    public static SeatingOutlinesPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        List<BlockPos> chairs = readPositions(buffer);
        List<BlockPos> tables = readPositions(buffer);
        int count = readCount(buffer);
        List<Link> links = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            links.add(new Link(buffer.readBlockPos(), buffer.readBlockPos()));
        }
        return new SeatingOutlinesPacket(dimension, chairs, tables, links);
    }

    private static List<BlockPos> readPositions(FriendlyByteBuf buffer) {
        int count = readCount(buffer);
        List<BlockPos> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(buffer.readBlockPos());
        }
        return result;
    }

    private static int readCount(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid seating outline count: " + count);
        }
        return count;
    }

    public static void handle(SeatingOutlinesPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SeatingOutlines.accept(packet)));
        context.setPacketHandled(true);
    }
}
