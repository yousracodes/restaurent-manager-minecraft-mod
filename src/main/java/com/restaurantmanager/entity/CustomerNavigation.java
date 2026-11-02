package com.restaurantmanager.entity;

import com.restaurantmanager.seating.SeatingMarkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Ground navigation that treats restaurant furniture as obstacles. */
public class CustomerNavigation extends GroundPathNavigation {
    private Vec3 exactTarget;
    public CustomerNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    public boolean moveToExact(Vec3 target, double speed) {
        BlockPos destination = BlockPos.containing(target);
        // Preserve progress instead of rebuilding the same route every tick.
        if (this.path != null && !this.path.isDone() && this.path.canReach() && !this.isStuck()) {
            var end = this.path.getEndNode();
            if (end != null && end.x == destination.getX() && end.z == destination.getZ()
                    && Math.abs(end.y - target.y) < 1.0D) {
                this.exactTarget = target;
                return true;
            }
        }
        // The coordinate moveTo overload uses accuracy=1, which can stop one
        // whole block short of the chair's approach block. Use accuracy=0.
        this.stop();
        var exactPath = this.createPath(destination, 0);
        if (exactPath == null || !exactPath.canReach()) {
            return false;
        }
        this.exactTarget = target;
        return this.moveTo(exactPath, speed);
    }

    @Override
    protected void followThePath() {
        super.followThePath();
        if (exactTarget != null && this.path != null && this.path.isDone()) {
            // Vanilla may finish the last waypoint before reaching its center.
            // Keep walking the final approach rather than waiting for a push.
            if (Math.abs(this.mob.getX() - exactTarget.x) > 0.2
                    || Math.abs(this.mob.getZ() - exactTarget.z) > 0.2
                    || Math.abs(this.mob.getY() - exactTarget.y) > 0.6) {
                this.path.setNextNodeIndex(this.path.getNodeCount() - 1);
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        exactTarget = null;
    }

    public boolean hasArrivedAt(Vec3 target) {
        if (this.path == null || !this.path.isDone() || !this.path.canReach()) {
            return false;
        }
        var end = this.path.getEndNode();
        BlockPos destination = BlockPos.containing(target);
        return end != null && end.x == destination.getX() && end.z == destination.getZ()
                && Math.abs(end.y - target.y) < 1.0D
                && CustomerWaypoint.hasArrived(this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        end.x, end.y, end.z, this.mob.getBbWidth());
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new WalkNodeEvaluator() {
            @Override
            public BlockPathTypes getBlockPathType(BlockGetter blocks, int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                if (mob.level() instanceof ServerLevel serverLevel) {
                    SeatingMarkSavedData marks = SeatingMarkSavedData.get(serverLevel);
                    if (marks.isSeat(pos) || marks.isTable(pos)
                            || marks.isSeat(pos.below()) || marks.isTable(pos.below())) {
                        return BlockPathTypes.BLOCKED;
                    }
                }
                BlockPathTypes type = super.getBlockPathType(blocks, x, y, z);
                if (type == BlockPathTypes.WALKABLE) {
                    double floor = WalkNodeEvaluator.getFloorLevel(blocks, pos);
                    double halfWidth = mob.getBbWidth() / 2.0;
                    AABB bounds = new AABB(x + 0.5 - halfWidth, floor + 0.001,
                            z + 0.5 - halfWidth, x + 0.5 + halfWidth,
                            floor + mob.getBbHeight(), z + 0.5 + halfWidth);
                    if (!mob.level().noCollision(mob, bounds)) {
                        return BlockPathTypes.BLOCKED;
                    }
                }
                return type;
            }
        };
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }
}
