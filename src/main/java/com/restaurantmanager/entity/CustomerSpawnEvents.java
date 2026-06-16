package com.restaurantmanager.entity;

import com.restaurantmanager.block.RestaurantSignBlock;
import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.setup.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class CustomerSpawnEvents {
    private static final Random RANDOM = new Random();
    private static final int SPAWN_CHECK_INTERVAL_TICKS = 20 * 15;
    private static final double ACTIVE_PLAYER_RADIUS = 128.0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % SPAWN_CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            RestaurantManager manager = RestaurantManager.get(level);
            for (Restaurant restaurant : manager.getRestaurants()) {
                trySpawnCustomer(level, restaurant);
            }
        }
    }

    private void trySpawnCustomer(ServerLevel level, Restaurant restaurant) {
        if (!restaurant.isActive() || !restaurant.isOpen()) {
            return;
        }
        BlockPos signPos = restaurant.getSignPos();
        if (!level.hasNearbyAlivePlayer(signPos.getX() + 0.5, signPos.getY() + 0.5, signPos.getZ() + 0.5, ACTIVE_PLAYER_RADIUS)) {
            return;
        }
        int stars = restaurant.getStarRating();
        if (countCustomersForRestaurant(level, restaurant) >= getCustomerCap(stars)) {
            return;
        }
        if (RANDOM.nextFloat() > getSpawnChance(stars)) {
            return;
        }

        BlockState signState = level.getBlockState(signPos);
        Direction facing = signState.hasProperty(RestaurantSignBlock.FACING) ? signState.getValue(RestaurantSignBlock.FACING) : Direction.NORTH;
        Optional<BlockPos> spawnPos = findSpawnPos(level, signPos, facing);
        if (spawnPos.isEmpty()) {
            return;
        }
        CustomerEntity customer = ModEntities.CUSTOMER.get().spawn(level, spawnPos.get(), MobSpawnType.TRIGGERED);
        if (customer != null) {
            customer.assignRestaurant(restaurant);
        }
    }

    private Optional<BlockPos> findSpawnPos(ServerLevel level, BlockPos signPos, Direction facing) {
        List<BlockPos> candidates = List.of(
                signPos.relative(facing).above(),
                signPos.relative(facing),
                signPos.relative(facing).relative(facing.getClockWise()).above(),
                signPos.relative(facing).relative(facing.getCounterClockWise()).above(),
                signPos.relative(facing, 2).above(),
                signPos.above()
        );
        return candidates.stream().filter(pos -> canSpawnAt(level, pos)).findFirst();
    }

    private boolean canSpawnAt(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();
    }

    private int countCustomersForRestaurant(ServerLevel level, Restaurant restaurant) {
        AABB searchBounds = restaurant.getBounds().inflate(96.0);
        return level.getEntities(
                ModEntities.CUSTOMER.get(),
                searchBounds,
                customer -> restaurant.getId().equals(customer.getRestaurantId())
        ).size();
    }

    private int getCustomerCap(int stars) {
        return switch (stars) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            default -> 6;
        };
    }

    private float getSpawnChance(int stars) {
        return switch (stars) {
            case 1 -> 0.50F;
            case 2 -> 0.60F;
            case 3 -> 0.70F;
            case 4 -> 0.80F;
            default -> 0.90F;
        };
    }
}
