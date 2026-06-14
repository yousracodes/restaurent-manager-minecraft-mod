package com.restaurantmanager.seating;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

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
        if (level instanceof ServerLevel serverLevel) {
            SeatingMarkSavedData.get(serverLevel).mark(new MarkedPoint(
                    pos,
                    level.dimension(),
                    MarkType.TABLE,
                    Vec3.atCenterOf(pos)
            ));
        }
    }

    public static void unmarkTable(BlockPos pos, Level level) {
        if (level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isTable(pos)) {
            SeatingMarkSavedData.get(serverLevel).unmark(pos);
        }
    }

    public static boolean isSeat(BlockPos pos, Level level) {
        return level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isSeat(pos);
    }

    public static boolean isTable(BlockPos pos, Level level) {
        return level instanceof ServerLevel serverLevel && SeatingMarkSavedData.get(serverLevel).isTable(pos);
    }

    public static List<MarkedPoint> getNearbySeats(BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).getNearbySeats(restaurantCenter, radius);
    }

    public static Optional<MarkedPoint> findNearestAvailableSeat(BlockPos restaurantCenter, double radius, ServerLevel level) {
        return SeatingMarkSavedData.get(level).findNearestAvailableSeat(restaurantCenter, radius);
    }

    public static boolean occupySeat(BlockPos pos, UUID entityId, ServerLevel level) {
        return SeatingMarkSavedData.get(level).occupySeat(pos, entityId);
    }

    public static void releaseSeat(BlockPos pos, UUID entityId, ServerLevel level) {
        SeatingMarkSavedData.get(level).releaseSeat(pos, entityId);
    }

    private static Vec3 computeSeatPosition(BlockPos pos, Level level) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getShape(level, pos);
        double yOffset = 0.75;
        if (!shape.isEmpty()) {
            double maxY = shape.max(Direction.Axis.Y);
            if (maxY <= 0.625) {
                yOffset = 0.5;
            } else if (maxY >= 0.95) {
                yOffset = 1.0;
            }
        }
        return new Vec3(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5);
    }
}
