package com.restaurantmanager.network.packet;

import com.restaurantmanager.client.ClientCustomerScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenTakeoutNegotiationPacket {
    private final int customerId;
    private final String customerName;
    private final int requestIndex;
    private final String unavailableItemName;

    public OpenTakeoutNegotiationPacket(int customerId, String customerName, int requestIndex, String unavailableItemName) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.requestIndex = requestIndex;
        this.unavailableItemName = unavailableItemName;
    }

    public static void encode(OpenTakeoutNegotiationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.customerId);
        buffer.writeUtf(packet.customerName, 128);
        buffer.writeInt(packet.requestIndex);
        buffer.writeUtf(packet.unavailableItemName, 128);
    }

    public static OpenTakeoutNegotiationPacket decode(FriendlyByteBuf buffer) {
        return new OpenTakeoutNegotiationPacket(buffer.readInt(), buffer.readUtf(128), buffer.readInt(), buffer.readUtf(128));
    }

    public static void handle(OpenTakeoutNegotiationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientCustomerScreens.openTakeoutNegotiation(
                        packet.customerId,
                        packet.customerName,
                        packet.requestIndex,
                        packet.unavailableItemName
                )));
        context.setPacketHandled(true);
    }
}
