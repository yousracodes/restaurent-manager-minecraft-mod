package com.restaurantmanager.network.packet;

import com.restaurantmanager.client.ClientRestaurantScreens;
import com.restaurantmanager.restaurant.Restaurant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenRestaurantManagementPacket {
    private final BlockPos signPos;
    private final boolean open;
    private final String name;
    private final String ownerName;
    private final int reputation;
    private final int customersServed;
    private final int ordersCompleted;
    private final int ordersFailed;
    private final int customersLost;
    private final int moneyEarned;
    private final String menuTitle;
    private final int menuItemCount;

    public OpenRestaurantManagementPacket(BlockPos signPos, Restaurant restaurant) {
        this(signPos, restaurant.isOpen(), restaurant.getName(), restaurant.getOwnerName(), restaurant.getReputation(),
                restaurant.getCustomersServed(), restaurant.getOrdersCompleted(), restaurant.getOrdersFailed(),
                restaurant.getCustomersLost(), restaurant.getMoneyEarned(), restaurant.getMenu().getTitle(),
                restaurant.getMenu().getItems().size());
    }

    public OpenRestaurantManagementPacket(BlockPos signPos, boolean open, String name, String ownerName, int reputation,
                                          int customersServed, int ordersCompleted, int ordersFailed,
                                          int customersLost, int moneyEarned, String menuTitle, int menuItemCount) {
        this.signPos = signPos;
        this.open = open;
        this.name = name;
        this.ownerName = ownerName;
        this.reputation = reputation;
        this.customersServed = customersServed;
        this.ordersCompleted = ordersCompleted;
        this.ordersFailed = ordersFailed;
        this.customersLost = customersLost;
        this.moneyEarned = moneyEarned;
        this.menuTitle = menuTitle;
        this.menuItemCount = menuItemCount;
    }

    public static void encode(OpenRestaurantManagementPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeBoolean(packet.open);
        buffer.writeUtf(packet.name, 64);
        buffer.writeUtf(packet.ownerName, 64);
        buffer.writeInt(packet.reputation);
        buffer.writeInt(packet.customersServed);
        buffer.writeInt(packet.ordersCompleted);
        buffer.writeInt(packet.ordersFailed);
        buffer.writeInt(packet.customersLost);
        buffer.writeInt(packet.moneyEarned);
        buffer.writeUtf(packet.menuTitle, 64);
        buffer.writeInt(packet.menuItemCount);
    }

    public static OpenRestaurantManagementPacket decode(FriendlyByteBuf buffer) {
        return new OpenRestaurantManagementPacket(
                buffer.readBlockPos(),
                buffer.readBoolean(),
                buffer.readUtf(64),
                buffer.readUtf(64),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(64),
                buffer.readInt()
        );
    }

    public static void handle(OpenRestaurantManagementPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientRestaurantScreens.openManagement(packet.signPos, packet.open, packet.name, packet.ownerName,
                        packet.reputation, packet.customersServed, packet.ordersCompleted, packet.ordersFailed,
                        packet.customersLost, packet.moneyEarned, packet.menuTitle, packet.menuItemCount)));
        context.setPacketHandled(true);
    }
}
