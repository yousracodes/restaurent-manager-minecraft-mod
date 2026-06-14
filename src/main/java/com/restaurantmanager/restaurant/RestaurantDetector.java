package com.restaurantmanager.restaurant;

import com.restaurantmanager.block.RestaurantSignBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class RestaurantDetector {
    private static final int MAX_SEARCH_RADIUS = 16;
    private static final int MAX_INTERIOR_VOLUME = 2048;

    public static Optional<Restaurant> detectAndRegister(ServerLevel level, BlockPos signPos, RestaurantManager manager) {
        Optional<Restaurant> found = detect(level, signPos);
        if (found.isPresent()) {
            Restaurant restaurant = found.get();
            if (!manager.overlaps(restaurant.getBounds())) {
                manager.addRestaurant(restaurant);
                level.sendBlockUpdated(signPos, level.getBlockState(signPos), level.getBlockState(signPos), 3);
                return Optional.of(restaurant);
            }
            return Optional.of(restaurant);
        }
        return Optional.empty();
    }

    public static Optional<Restaurant> detect(ServerLevel level, BlockPos signPos) {
        BlockState signState = level.getBlockState(signPos);
        Direction facing = signState.hasProperty(RestaurantSignBlock.FACING)
                ? signState.getValue(RestaurantSignBlock.FACING)
                : Direction.NORTH;

        for (BlockPos start : findInteriorStarts(level, signPos, facing)) {
            FloodFillResult result = floodFillInterior(level, start);
            Set<BlockPos> interior = result.interior();
            if (result.escaped() || interior.isEmpty() || interior.size() > MAX_INTERIOR_VOLUME) {
                continue;
            }

            AABB bounds = computeBounds(interior);
            if (!validateBounds(level, bounds)) {
                continue;
            }

            return Optional.of(new Restaurant(signPos, bounds));
        }

        return Optional.empty();
    }

    public static List<String> debugDetection(ServerLevel level, BlockPos signPos) {
        List<String> debug = new ArrayList<>();
        BlockState signState = level.getBlockState(signPos);
        Direction facing = signState.hasProperty(RestaurantSignBlock.FACING)
                ? signState.getValue(RestaurantSignBlock.FACING)
                : Direction.NORTH;
        Direction interiorDirection = facing.getOpposite();

        debug.add("Restaurant debug: sign=" + formatPos(signPos)
                + ", facing=" + facing
                + ", scanning=" + interiorDirection);

        int interiorCandidates = 0;
        for (int distance = 1; distance <= 3; distance++) {
            for (int yOffset = 0; yOffset <= 2; yOffset++) {
                BlockPos candidate = signPos.relative(interiorDirection, distance).above(yOffset);
                BlockState candidateState = level.getBlockState(candidate);
                if (!isInteriorSpace(level, candidate)) {
                    debug.add("Candidate " + formatPos(candidate) + " blocked by " + candidateState.getBlock().getName().getString());
                    continue;
                }

                interiorCandidates++;
                FloodFillResult result = floodFillInterior(level, candidate);
                Set<BlockPos> interior = result.interior();
                AABB bounds = interior.isEmpty() ? null : computeBounds(interior);
                boolean validBounds = bounds != null && validateBounds(level, bounds);

                debug.add("Candidate " + formatPos(candidate)
                        + " air, fill=" + interior.size()
                        + ", escaped=" + result.escaped()
                        + ", bounds=" + formatBounds(bounds)
                        + ", validSize=" + validBounds);
            }
        }

        if (interiorCandidates == 0) {
            debug.add("No air candidates found behind the sign within 3 blocks and 2 blocks up.");
        }

        return debug;
    }

    private static Set<BlockPos> findInteriorStarts(Level level, BlockPos signPos, Direction facing) {
        Direction interiorDirection = facing.getOpposite();
        Set<BlockPos> starts = new HashSet<>();
        for (int distance = 1; distance <= 3; distance++) {
            for (int yOffset = 0; yOffset <= 2; yOffset++) {
                BlockPos candidate = signPos.relative(interiorDirection, distance).above(yOffset);
                if (isInteriorSpace(level, candidate)) {
                    starts.add(candidate);
                }
            }
        }
        return starts;
    }

    private static boolean isInteriorSpace(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static FloodFillResult floodFillInterior(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> open = new ArrayDeque<>();
        open.add(start);
        visited.add(start);
        boolean escaped = false;

        while (!open.isEmpty() && visited.size() <= MAX_INTERIOR_VOLUME) {
            BlockPos current = open.remove();
            for (BlockPos offset : new BlockPos[]{
                    current.north(), current.south(), current.east(), current.west(), current.above(), current.below()
            }) {
                if (visited.contains(offset)) {
                    continue;
                }
                if (offset.distSqr(start) > MAX_SEARCH_RADIUS * MAX_SEARCH_RADIUS) {
                    escaped = true;
                    continue;
                }
                if (isInteriorSpace(level, offset)) {
                    visited.add(offset);
                    open.add(offset);
                }
            }
        }

        if (visited.size() > MAX_INTERIOR_VOLUME) {
            escaped = true;
        }

        return new FloodFillResult(visited, escaped);
    }

    private static AABB computeBounds(Set<BlockPos> interior) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : interior) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    private static boolean validateBounds(Level level, AABB bounds) {
        if (bounds.getXsize() < 3 || bounds.getYsize() < 3 || bounds.getZsize() < 3) {
            return false;
        }

        for (int x = (int) bounds.minX; x < bounds.maxX; x++) {
            for (int y = (int) bounds.minY; y < bounds.maxY; y++) {
                for (int z = (int) bounds.minZ; z < bounds.maxZ; z++) {
                    if (!isInteriorSpace(level, new BlockPos(x, y, z))) {
                        continue;
                    }
                    if (x == bounds.minX || x == bounds.maxX - 1 || y == bounds.minY || y == bounds.maxY - 1 || z == bounds.minZ || z == bounds.maxZ - 1) {
                        continue;
                    }
                }
            }
        }

        return true;
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String formatBounds(AABB bounds) {
        if (bounds == null) {
            return "none";
        }
        return (int) bounds.minX + "," + (int) bounds.minY + "," + (int) bounds.minZ
                + " -> "
                + ((int) bounds.maxX - 1) + "," + ((int) bounds.maxY - 1) + "," + ((int) bounds.maxZ - 1);
    }

    private record FloodFillResult(Set<BlockPos> interior, boolean escaped) {
    }
}
