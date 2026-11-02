package com.restaurantmanager.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

/** Checks the short transition from an accessible side onto a reserved chair. */
final class SeatAccess {
    private SeatAccess() {
    }

    static boolean canSitFrom(Level level, Mob customer, BlockPos seat) {
        return canSitFrom(level, customer, seat, customer.position());
    }

    static boolean canSitFrom(Level level, Mob customer, BlockPos seat, Vec3 start) {
        double dx = seat.getX() + 0.5 - start.x;
        double dz = seat.getZ() + 0.5 - start.z;
        if (dx * dx + dz * dz > 2.25 || Math.abs(start.y - seat.getY()) > 0.6) {
            return false;
        }
        // Only the reserved chair may be crossed when mounting. Walls, other
        // chairs and tables must remain obstacles throughout the transition.
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) / 0.1));
        CollisionContext context = CollisionContext.of(customer);
        for (int step = 0; step <= steps; step++) {
            double fraction = (double) step / steps;
            AABB body = customer.getBoundingBox().move(start.subtract(customer.position())).move(dx * fraction, 0.001, dz * fraction).deflate(0.001);
            for (BlockPos pos : BlockPos.betweenClosed(
                    BlockPos.containing(body.minX - 1, body.minY - 1, body.minZ - 1),
                    BlockPos.containing(body.maxX + 1, body.maxY + 1, body.maxZ + 1))) {
                if (pos.equals(seat)) {
                    continue;
                }
                var shape = level.getBlockState(pos).getCollisionShape(level, pos, context);
                if (!shape.isEmpty() && Shapes.joinIsNotEmpty(Shapes.create(body),
                        shape.move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.AND)) {
                    return false;
                }
            }
        }
        return true;
    }
}
