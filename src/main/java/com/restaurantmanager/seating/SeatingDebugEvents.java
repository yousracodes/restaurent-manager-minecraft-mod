package com.restaurantmanager.seating;

import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.SeatingOutlinesPacket;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Supplies the client overlay instead of continuously spawning marker particles. */
public class SeatingDebugEvents {
    public static final double RANGE = 24.0;
    private final Map<UUID, SeatingOutlinesPacket> lastSent = new HashMap<>();
    private final Map<UUID, ServerPlayer> viewers = new HashMap<>();

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 5 != 0) {
            return;
        }
        Set<UUID> holding = new HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!isHoldingTool(player.getMainHandItem()) && !isHoldingTool(player.getOffhandItem())) {
                continue;
            }
            holding.add(player.getUUID());
            SeatingOutlinesPacket snapshot = snapshot(player.serverLevel(), player.blockPosition());
            boolean newViewer = viewers.put(player.getUUID(), player) != player;
            if (newViewer || !snapshot.equals(lastSent.get(player.getUUID()))) {
                lastSent.put(player.getUUID(), snapshot);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
            }
        }
        lastSent.keySet().retainAll(holding);
        viewers.keySet().retainAll(holding);
    }

    public static boolean isHoldingTool(ItemStack stack) {
        return stack.is(ModItems.TABLE_LINK_TOOL.get());
    }

    public static SeatingOutlinesPacket snapshot(ServerLevel level, BlockPos playerPos) {
        Set<BlockPos> chairs = new LinkedHashSet<>();
        Set<BlockPos> tables = new LinkedHashSet<>();
        var links = new ArrayList<SeatingOutlinesPacket.Link>();
        for (MarkedPoint point : SeatingMarkSavedData.get(level).getAllPoints()) {
            if (!isNearby(playerPos, point.getPos())) {
                continue;
            }
            if (point.getType() == MarkType.SEAT) {
                chairs.add(point.getPos());
            } else if (point.getType() == MarkType.TABLE) {
                tables.add(point.getPos());
            }
        }
        for (var restaurant : RestaurantManager.get(level).getRestaurants()) {
            if (!restaurant.isActive()) {
                continue;
            }
            for (var link : restaurant.getSeatTableLinks()) {
                if (links.size() < SeatingOutlinesPacket.MAX_ENTRIES
                        && isNearby(playerPos, link.getChairPos()) && isNearby(playerPos, link.getTablePos())) {
                    chairs.add(link.getChairPos());
                    tables.add(link.getTablePos());
                    links.add(new SeatingOutlinesPacket.Link(link.getChairPos(), link.getTablePos()));
                }
            }
        }
        Comparator<BlockPos> order = Comparator.comparingLong(BlockPos::asLong);
        links.sort(Comparator.comparingLong((SeatingOutlinesPacket.Link link) -> link.chair().asLong())
                .thenComparingLong(link -> link.table().asLong()));
        return new SeatingOutlinesPacket(level.dimension().location(),
                chairs.stream().sorted(order).limit(SeatingOutlinesPacket.MAX_ENTRIES).toList(),
                tables.stream().sorted(order).limit(SeatingOutlinesPacket.MAX_ENTRIES).toList(), links);
    }

    private static boolean isNearby(BlockPos playerPos, BlockPos markPos) {
        return markPos.distSqr(playerPos) <= RANGE * RANGE;
    }
}
