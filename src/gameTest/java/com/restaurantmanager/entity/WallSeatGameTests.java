package com.restaurantmanager.entity;

import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.seating.RestaurantSeatingApi;
import com.restaurantmanager.setup.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("restaurantmanager")
@PrefixGameTestTemplate(false)
public class WallSeatGameTests {
    @GameTest(template = "seat_room", timeoutTicks = 240)
    public static void northWall(GameTestHelper helper) { wallCase(helper, Direction.NORTH, false, false); }
    @GameTest(template = "seat_room", timeoutTicks = 240)
    public static void southWall(GameTestHelper helper) { wallCase(helper, Direction.SOUTH, false, false); }
    @GameTest(template = "seat_room", timeoutTicks = 240)
    public static void eastWall(GameTestHelper helper) { wallCase(helper, Direction.EAST, false, false); }
    @GameTest(template = "seat_room", timeoutTicks = 240)
    public static void westWall(GameTestHelper helper) { wallCase(helper, Direction.WEST, false, false); }
    @GameTest(template = "seat_room", timeoutTicks = 240)
    public static void wallCorner(GameTestHelper helper) { wallCase(helper, Direction.NORTH, true, false); }
    @GameTest(template = "seat_room", timeoutTicks = 40)
    public static void nearbyAccessibleSide(GameTestHelper helper) { wallCase(helper, Direction.WEST, false, true); }

    private static void wallCase(GameTestHelper helper, Direction wallSide, boolean corner, boolean nearby) {
        floor(helper);
        BlockPos chair = new BlockPos(5, 1, 5);
        Direction side = wallSide.getClockWise();
        for (int offset = -3; offset <= 3; offset++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(chair.relative(wallSide).relative(side, offset).atY(y), Blocks.STONE);
                if (corner) {
                    helper.setBlock(chair.relative(side).relative(wallSide, offset).atY(y), Blocks.STONE);
                }
            }
        }
        helper.setBlock(chair, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, wallSide));
        BlockPos table = chair.relative(corner ? wallSide.getOpposite() : side);
        helper.setBlock(table, Blocks.OAK_PLANKS);
        var customer = helper.spawn(ModEntities.DINE_IN_CUSTOMER.get(),
                Vec3.atBottomCenterOf(chair.relative(wallSide.getOpposite(), nearby ? 1 : 4)));
        if (corner) {
            // The table blocks the front; the remaining side is the only entrance.
            customer.setPos(Vec3.atBottomCenterOf(helper.absolutePos(chair.relative(side.getOpposite(), 4))));
        }
        configure(helper, customer, chair, table);
        helper.succeedWhen(() -> {
            helper.assertTrue(customer.state == CustomerState.WAITING_FOR_PLATE,
                    "Customer did not sit beside wall: " + customer.state + " at " + customer.position()
                            + " chair=" + customer.reservedSeatPos + " approach=" + customer.getSeatApproachTarget()
                            + " navTarget=" + customer.getNavigation().getTargetPos()
                            + " path=" + customer.getNavigation().getPath()
                            + " done=" + customer.getNavigation().isDone()
                            + " end=" + (customer.getNavigation().getPath() == null ? null : customer.getNavigation().getPath().getEndNode()));
            helper.assertTrue(customer.position().distanceTo(customer.getSeatTarget(helper.absolutePos(chair))) < 0.01,
                    "Customer must be on reserved chair, without being pushed");
        });
    }

    @GameTest(template = "seat_room", timeoutTicks = 40)
    public static void cannotSitThroughWall(GameTestHelper helper) {
        floor(helper);
        BlockPos chair = new BlockPos(5, 1, 5);
        helper.setBlock(chair, Blocks.OAK_STAIRS);
        var customer = helper.spawn(ModEntities.DINE_IN_CUSTOMER.get(), 4, 1, 5);
        BlockPos absoluteChair = helper.absolutePos(chair);
        helper.setBlock(new BlockPos(4, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 5), Blocks.STONE);
        helper.assertTrue(!SeatAccess.canSitFrom(helper.getLevel(), customer, absoluteChair),
                "A nearby chair must not permit sitting through a wall");
        customer.discard();
        helper.succeed();
    }

    @GameTest(template = "seat_room", timeoutTicks = 40)
    public static void cannotSitAcrossTable(GameTestHelper helper) {
        floor(helper);
        BlockPos chair = new BlockPos(5, 1, 5);
        helper.setBlock(chair, Blocks.OAK_STAIRS);
        helper.setBlock(4, 1, 5, Blocks.OAK_PLANKS);
        var customer = helper.spawn(ModEntities.DINE_IN_CUSTOMER.get(), 4.1F, 1, 5.5F);
        helper.assertTrue(!SeatAccess.canSitFrom(helper.getLevel(), customer, helper.absolutePos(chair)),
                "A table must obstruct the seating transition");
        customer.discard();
        helper.succeed();
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }

    private static void configure(GameTestHelper helper, DineInCustomerEntity customer, BlockPos chair, BlockPos table) {
        BlockPos absoluteChair = helper.absolutePos(chair);
        BlockPos absoluteTable = helper.absolutePos(table);
        RestaurantSeatingApi.markSeat(absoluteChair, helper.getLevel());
        RestaurantSeatingApi.markTable(absoluteTable, helper.getLevel());
        customer.restaurant = new Restaurant(helper.absolutePos(BlockPos.ZERO),
                new AABB(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(11, 5, 11))));
        customer.reservedSeatPos = absoluteChair;
        customer.assignedTablePos = absoluteTable;
        customer.seatTarget = customer.getSeatTarget(absoluteChair);
        customer.state = CustomerState.MOVING_TO_SEAT;
        customer.setPersistenceRequired();
    }
}
