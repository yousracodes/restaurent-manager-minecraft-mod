package com.restaurantmanager.seating;

import com.restaurantmanager.network.packet.SeatingOutlinesPacket;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("restaurantmanager")
@PrefixGameTestTemplate(false)
public class SeatingOutlineGameTests {
    @GameTest(template = "seat_room")
    public static void outlineMarkersAndRange(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(5, 1, 5));
        BlockPos chair = origin.west();
        BlockPos table = origin.east();
        BlockPos queue = origin.north();
        BlockPos far = origin.above(100);
        RestaurantSeatingApi.markSeat(chair, helper.getLevel());
        RestaurantSeatingApi.markTable(table, helper.getLevel());
        RestaurantSeatingApi.markQueueSpot(queue, helper.getLevel());
        RestaurantSeatingApi.markSeat(far, helper.getLevel());
        var snapshot = SeatingDebugEvents.snapshot(helper.getLevel(), origin);
        helper.assertTrue(snapshot.chairs().contains(chair) && !snapshot.tables().contains(chair),
                "Chair must be sent as a chair outline");
        helper.assertTrue(snapshot.tables().contains(table) && !snapshot.chairs().contains(table),
                "Table must be sent as a table outline");
        helper.assertTrue(!snapshot.chairs().contains(queue) && !snapshot.tables().contains(queue),
                "Queue marker must not appear as a chair or table");
        helper.assertTrue(!snapshot.chairs().contains(far), "Distant markers must not be sent");
        helper.assertTrue(snapshot.dimension().equals(helper.getLevel().dimension().location()),
                "Snapshot must identify its dimension");
        SeatingMarkSavedData.get(helper.getLevel()).unmark(far);
        helper.succeed();
    }

    @GameTest(template = "seat_room")
    public static void outlinePacketRoundTrip(GameTestHelper helper) {
        var chair = new BlockPos(-123, 64, 456);
        var table = chair.east();
        var original = new SeatingOutlinesPacket(new ResourceLocation("minecraft", "overworld"),
                List.of(chair), List.of(table), List.of(new SeatingOutlinesPacket.Link(chair, table)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SeatingOutlinesPacket.encode(original, buffer);
            helper.assertTrue(original.equals(SeatingOutlinesPacket.decode(buffer)),
                    "Client must receive the same chair/table markers and connections");
            helper.assertTrue(buffer.readableBytes() == 0, "Packet must consume its entire payload");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "seat_room")
    public static void removedLinkUpdatesOutlines(GameTestHelper helper) {
        BlockPos chair = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos table = chair.east();
        BlockPos sign = helper.absolutePos(BlockPos.ZERO);
        Restaurant restaurant = new Restaurant(sign, new AABB(sign, sign.offset(11, 5, 11)));
        restaurant.addSeatTableLink(chair, table);
        var manager = RestaurantManager.get(helper.getLevel());
        manager.addRestaurant(restaurant);
        RestaurantSeatingApi.markSeat(chair, helper.getLevel());
        RestaurantSeatingApi.markTable(table, helper.getLevel());
        var before = SeatingDebugEvents.snapshot(helper.getLevel(), chair);
        helper.assertTrue(before.links().contains(new SeatingOutlinesPacket.Link(chair, table)),
                "Linked pair must be included for connector rendering");
        manager.removeSeatTableLinkAt(chair);
        RestaurantSeatingApi.clearSeatOrTable(chair, helper.getLevel());
        var after = SeatingDebugEvents.snapshot(helper.getLevel(), chair);
        helper.assertTrue(!after.chairs().contains(chair) && after.links().stream().noneMatch(link -> link.chair().equals(chair)),
                "Removed chair must lose its outline and connector");
        helper.assertTrue(!before.equals(after), "Server must detect the overlay change");
        manager.removeRestaurantAt(sign);
        helper.succeed();
    }
}
