package com.restaurantmanager.network.packet;

import com.restaurantmanager.client.ClientCustomerScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenCustomerSuggestionPacket {
    private final int customerId;
    private final String customerName;

    public OpenCustomerSuggestionPacket(int customerId, String customerName) {
        this.customerId = customerId;
        this.customerName = customerName;
    }

    public static void encode(OpenCustomerSuggestionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.customerId);
        buffer.writeUtf(packet.customerName, 128);
    }

    public static OpenCustomerSuggestionPacket decode(FriendlyByteBuf buffer) {
        return new OpenCustomerSuggestionPacket(buffer.readInt(), buffer.readUtf(128));
    }

    public static void handle(OpenCustomerSuggestionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientCustomerScreens.openSuggestion(packet.customerId, packet.customerName)));
        context.setPacketHandled(true);
    }
}
