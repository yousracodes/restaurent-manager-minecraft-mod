package com.restaurantmanager.seating;

import com.restaurantmanager.block.CashRegisterBlock;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.SeatTableLink;
import com.restaurantmanager.setup.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class RestaurantSeatingApi {
    private RestaurantSeatingApi() {
    }

    public static void markSeat(BlockPos pos, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            SeatingMarkSavedData.get(serverLevel).mark(new MarkedPoint(
                    pos,
                    level.dimension(),
                    MarkType.SEAT,
                    computeSeatPosition(pos, level)
            ));
        }
    }

    public static void unmarkSeat(BlockPos pos, Level level) {
        if (level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isSeat(pos)) {
            SeatingMarkSavedData.get(serverLevel).unmark(pos);
        }
    }

    public static void markTable(BlockPos pos, Level level) {
        markPoint(pos, level, MarkType.TABLE, Vec3.atCenterOf(pos));
    }

    public static void unmarkTable(BlockPos pos, Level level) {
        if (level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isTable(pos)) {
            SeatingMarkSavedData.get(serverLevel).unmark(pos);
        }
    }

    public static void clearSeatTableLink(BlockPos chairPos, BlockPos tablePos, Level level) {
        unmarkSeat(chairPos, level);
        unmarkTable(tablePos, level);
    }

    public static void clearRestaurantSeatTables(Restaurant restaurant, Level level) {
        if (!(level instanceof ServerLevel) || restaurant == null) {
            return;
        }
        for (SeatTableLink link : List.copyOf(restaurant.getSeatTableLinks())) {
            clearSeatTableLink(link.getChairPos(), link.getTablePos(), level);
        }
    }

    public static void clearSeatOrTable(BlockPos pos, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        SeatingMarkSavedData data = SeatingMarkSavedData.get(serverLevel);
        if (data.isSeat(pos)) {
            data.unlinkSeatFromTable(pos);
            data.unmark(pos);
        } else if (data.isTable(pos)) {
            data.unlinkTable(pos);
            data.unmark(pos);
        }
    }

    public static void markQueueSpot(BlockPos pos, Level level) {
        markPoint(pos, level, MarkType.QUEUE, Vec3.atBottomCenterOf(pos));
    }

    public static void unmarkQueueSpot(BlockPos pos, Level level) {
        if (level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isQueue(pos)) {
            SeatingMarkSavedData.get(serverLevel).unmark(pos);
        }
    }

    public static void markCounter(BlockPos pos, Level level) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.CASH_REGISTER.get()) && state.hasProperty(CashRegisterBlock.FACING)) {
            markCounter(pos, state.getValue(CashRegisterBlock.FACING), level);
            return;
        }
        markPoint(pos, level, MarkType.COUNTER, Vec3.atBottomCenterOf(pos.relative(Direction.UP)));
    }

    public static void markCounter(BlockPos pos, Direction facing, Level level) {
        markPoint(pos, level, MarkType.COUNTER, getCounterTarget(pos, facing, level));
    }

    public static void unmarkCounter(BlockPos pos, Level level) {
        if (level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isCounter(pos)) {
            SeatingMarkSavedData.get(serverLevel).unmark(pos);
        }
    }

    public static void markEntrance(BlockPos pos, Level level) {
        markPoint(pos, level, MarkType.ENTRANCE, Vec3.atBottomCenterOf(pos));
    }

    public static void unmarkEntrance(BlockPos pos, Level level) {
        if (level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isEntrance(pos)) {
            SeatingMarkSavedData.get(serverLevel).unmark(pos);
        }
    }

    public static void markExit(BlockPos pos, Level level) {
        markPoint(pos, level, MarkType.EXIT, Vec3.atBottomCenterOf(pos));
    }

    public static void unmarkExit(BlockPos pos, Level level) {
        if (level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isExit(pos)) {
            SeatingMarkSavedData.get(serverLevel).unmark(pos);
        }
    }

    public static void markDoorBell(BlockPos pos, Direction facing, Level level) {
        markPoint(pos, level, MarkType.DOOR_BELL, getDoorBellTarget(pos, facing, level));
    }

    public static Vec3 getCounterInteractionPosition(BlockPos pos, Direction facing, Level level) {
        return getCounterTarget(pos, facing, level);
    }

    public static void unmarkDoorBell(BlockPos pos, Level level) {
        if (level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isDoorBell(pos)) {
            SeatingMarkSavedData.get(serverLevel).unmark(pos);
        }
    }

    public static boolean isSeat(BlockPos pos, Level level) {
        return level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isSeat(pos);
    }

    public static boolean isTable(BlockPos pos, Level level) {
        return level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isTable(pos);
    }

    public static boolean isQueueSpot(BlockPos pos, Level level) {
        return level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isQueue(pos);
    }

    public static boolean isCounter(BlockPos pos, Level level) {
        return level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isCounter(pos);
    }

    public static boolean isEntrance(BlockPos pos, Level level) {
        return level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isEntrance(pos);
    }

    public static boolean isExit(BlockPos pos, Level level) {
        return level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isExit(pos);
    }

    public static boolean isDoorBell(BlockPos pos, Level level) {
        return level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isDoorBell(pos);
    }

    public static List<MarkedPoint> getNearbySeats(BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).getNearbySeats(restaurantCenter, radius);
    }

    public static Optional<MarkedPoint> findNearestAvailableSeat(BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).findNearestAvailableSeat(restaurantCenter, radius);
    }

    public static Optional<MarkedPoint> findNearestTable(BlockPos center, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).findNearestTable(center, radius);
    }

    public static Optional<MarkedPoint> findNearestAvailableQueueSpot(BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).findNearestAvailableQueueSpot(restaurantCenter, radius);
    }

    public static Optional<MarkedPoint> findNearestAvailableCounter(BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).findNearestAvailableCounter(restaurantCenter, radius);
    }

    public static Optional<MarkedPoint> findNearestEntrance(BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).findNearestEntrance(restaurantCenter, radius);
    }

    public static Optional<MarkedPoint> findNearestExit(BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).findNearestExit(restaurantCenter, radius);
    }

    public static Optional<MarkedPoint> getPoint(BlockPos pos, ServerLevel level) {
        return SeatingMarkSavedData.get(level).getPoint(pos);
    }

    public static boolean occupySeat(BlockPos pos, UUID entityId, ServerLevel level) {
        return SeatingMarkSavedData.get(level).occupySeat(pos, entityId);
    }

    public static boolean occupyQueueSpot(BlockPos pos, UUID entityId, ServerLevel level) {
        return SeatingMarkSavedData.get(level).occupyQueueSpot(pos, entityId);
    }

    public static boolean occupyCounter(BlockPos pos, UUID entityId, ServerLevel level) {
        return SeatingMarkSavedData.get(level).occupyCounter(pos, entityId);
    }

    public static void releaseSeat(BlockPos pos, UUID entityId, ServerLevel level) {
        SeatingMarkSavedData.get(level).releaseSeat(pos, entityId);
    }

    public static void releaseQueueSpot(BlockPos pos, UUID entityId, ServerLevel level) {
        SeatingMarkSavedData.get(level).releaseQueueSpot(pos, entityId);
    }

    public static void releaseCounter(BlockPos pos, UUID entityId, ServerLevel level) {
        SeatingMarkSavedData.get(level).releaseCounter(pos, entityId);
    }

    public static boolean isFrontOccupiedQueueSpot(BlockPos pos, BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).isFrontOccupiedQueueSpot(pos, restaurantCenter, radius);
    }

    public static Optional<MarkedPoint> findEarlierAvailableQueueSpot(BlockPos currentPos, BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).findEarlierAvailableQueueSpot(currentPos, restaurantCenter, radius);
    }

    public static Optional<BlockPos> getLinkedTable(BlockPos seatPos, ServerLevel level) {
        return SeatingMarkSavedData.get(level).getLinkedTable(seatPos);
    }

    public static void linkSeatToTable(BlockPos seatPos, BlockPos tablePos, ServerLevel level) {
        SeatingMarkSavedData.get(level).linkSeatToTable(seatPos, tablePos);
    }

    private static void markPoint(BlockPos pos, Level level, MarkType type, Vec3 interactionPosition) {
        if (level instanceof ServerLevel serverLevel) {
            SeatingMarkSavedData.get(serverLevel).mark(new MarkedPoint(
                    pos,
                    level.dimension(),
                    type,
                    interactionPosition
            ));
        }
    }

    private static Vec3 getDoorBellTarget(BlockPos pos, Direction facing, Level level) {
        BlockPos target = pos.relative(facing);
        for (int i = 0; i < 3 && target.getY() > level.getMinBuildHeight(); i++) {
            BlockPos below = target.below();
            if (!level.getBlockState(below).isAir()) {
                return Vec3.atBottomCenterOf(target);
            }
            target = below;
        }
        return Vec3.atBottomCenterOf(pos);
    }

    private static Vec3 getCounterTarget(BlockPos pos, Direction facing, Level level) {
        BlockPos target = pos.relative(facing.getOpposite());
        for (int i = 0; i < 3 && target.getY() > level.getMinBuildHeight(); i++) {
            BlockPos below = target.below();
            if (!level.getBlockState(below).isAir()) {
                return Vec3.atBottomCenterOf(target);
            }
            target = below;
        }
        return Vec3.atBottomCenterOf(pos.relative(Direction.UP));
    }

    private static Vec3 computeSeatPosition(BlockPos pos, Level level) {
        return new Vec3(pos.getX() + 0.5, pos.getY() - 0.1, pos.getZ() + 0.5);
    }
}
