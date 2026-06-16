package com.restaurantmanager.network.packet;

import com.restaurantmanager.client.ClientRestaurantScreens;
import com.restaurantmanager.restaurant.MenuCategory;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantMenu;
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
    private final int starRating;
    private final int customersServed;
    private final int ordersCompleted;
    private final int ordersFailed;
    private final int customersLost;
    private final int moneyEarned;
    private final String menuTitle;
    private final int menuItemCount;
    private final String[] menuTitles;
    private final int[] menuItemCounts;

    public OpenRestaurantManagementPacket(BlockPos signPos, Restaurant restaurant) {
        this(signPos, restaurant.isOpen(), restaurant.getName(), restaurant.getOwnerName(), restaurant.getReputation(),
                restaurant.getStarRating(), restaurant.getCustomersServed(), restaurant.getOrdersCompleted(), restaurant.getOrdersFailed(),
                restaurant.getCustomersLost(), restaurant.getMoneyEarned(), restaurant.getMenus().size() + " menu(s)",
                restaurant.getMenuItemCount(), readMenuTitles(restaurant), readMenuItemCounts(restaurant));
    }

    public OpenRestaurantManagementPacket(BlockPos signPos, boolean open, String name, String ownerName, int reputation, int starRating,
                                          int customersServed, int ordersCompleted, int ordersFailed,
                                          int customersLost, int moneyEarned, String menuTitle, int menuItemCount) {
        this(signPos, open, name, ownerName, reputation, starRating, customersServed, ordersCompleted, ordersFailed,
                customersLost, moneyEarned, menuTitle, menuItemCount, new String[MenuCategory.values().length],
                new int[MenuCategory.values().length]);
    }

    public OpenRestaurantManagementPacket(BlockPos signPos, boolean open, String name, String ownerName, int reputation, int starRating,
                                          int customersServed, int ordersCompleted, int ordersFailed,
                                          int customersLost, int moneyEarned, String menuTitle, int menuItemCount,
                                          String[] menuTitles, int[] menuItemCounts) {
        this.signPos = signPos;
        this.open = open;
        this.name = name;
        this.ownerName = ownerName;
        this.reputation = reputation;
        this.starRating = starRating;
        this.customersServed = customersServed;
        this.ordersCompleted = ordersCompleted;
        this.ordersFailed = ordersFailed;
        this.customersLost = customersLost;
        this.moneyEarned = moneyEarned;
        this.menuTitle = menuTitle;
        this.menuItemCount = menuItemCount;
        this.menuTitles = normalizeTitles(menuTitles);
        this.menuItemCounts = normalizeCounts(menuItemCounts);
    }

    public static void encode(OpenRestaurantManagementPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.signPos);
        buffer.writeBoolean(packet.open);
        buffer.writeUtf(packet.name, 64);
        buffer.writeUtf(packet.ownerName, 64);
        buffer.writeInt(packet.reputation);
        buffer.writeInt(packet.starRating);
        buffer.writeInt(packet.customersServed);
        buffer.writeInt(packet.ordersCompleted);
        buffer.writeInt(packet.ordersFailed);
        buffer.writeInt(packet.customersLost);
        buffer.writeInt(packet.moneyEarned);
        buffer.writeUtf(packet.menuTitle, 64);
        buffer.writeInt(packet.menuItemCount);
        for (int i = 0; i < MenuCategory.values().length; i++) {
            buffer.writeUtf(packet.menuTitles[i], 64);
            buffer.writeInt(packet.menuItemCounts[i]);
        }
    }

    public static OpenRestaurantManagementPacket decode(FriendlyByteBuf buffer) {
        BlockPos signPos = buffer.readBlockPos();
        boolean open = buffer.readBoolean();
        String name = buffer.readUtf(64);
        String ownerName = buffer.readUtf(64);
        int reputation = buffer.readInt();
        int starRating = buffer.readInt();
        int customersServed = buffer.readInt();
        int ordersCompleted = buffer.readInt();
        int ordersFailed = buffer.readInt();
        int customersLost = buffer.readInt();
        int moneyEarned = buffer.readInt();
        String menuTitle = buffer.readUtf(64);
        int menuItemCount = buffer.readInt();
        String[] menuTitles = new String[MenuCategory.values().length];
        int[] menuItemCounts = new int[MenuCategory.values().length];
        for (int i = 0; i < MenuCategory.values().length; i++) {
            menuTitles[i] = buffer.readUtf(64);
            menuItemCounts[i] = buffer.readInt();
        }
        return new OpenRestaurantManagementPacket(
                signPos,
                open,
                name,
                ownerName,
                reputation,
                starRating,
                customersServed,
                ordersCompleted,
                ordersFailed,
                customersLost,
                moneyEarned,
                menuTitle,
                menuItemCount,
                menuTitles,
                menuItemCounts
        );
    }

    public static void handle(OpenRestaurantManagementPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientRestaurantScreens.openManagement(packet.signPos, packet.open, packet.name, packet.ownerName,
                        packet.reputation, packet.starRating, packet.customersServed, packet.ordersCompleted, packet.ordersFailed,
                        packet.customersLost, packet.moneyEarned, packet.menuTitle, packet.menuItemCount,
                        packet.menuTitles, packet.menuItemCounts)));
        context.setPacketHandled(true);
    }

    private static String[] readMenuTitles(Restaurant restaurant) {
        String[] titles = new String[MenuCategory.values().length];
        for (MenuCategory category : MenuCategory.values()) {
            RestaurantMenu menu = restaurant.getMenuByCategory(category).orElse(null);
            titles[category.ordinal()] = menu == null ? "" : menu.getTitle();
        }
        return titles;
    }

    private static int[] readMenuItemCounts(Restaurant restaurant) {
        int[] counts = new int[MenuCategory.values().length];
        for (MenuCategory category : MenuCategory.values()) {
            RestaurantMenu menu = restaurant.getMenuByCategory(category).orElse(null);
            counts[category.ordinal()] = menu == null ? 0 : menu.getItems().size();
        }
        return counts;
    }

    private static String[] normalizeTitles(String[] titles) {
        String[] normalized = new String[MenuCategory.values().length];
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = titles != null && i < titles.length && titles[i] != null ? titles[i] : "";
        }
        return normalized;
    }

    private static int[] normalizeCounts(int[] counts) {
        int[] normalized = new int[MenuCategory.values().length];
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = counts != null && i < counts.length ? counts[i] : 0;
        }
        return normalized;
    }
}
