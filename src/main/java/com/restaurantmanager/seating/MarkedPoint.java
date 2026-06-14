package com.restaurantmanager.seating;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class MarkedPoint {
    private final BlockPos pos;
    private final ResourceKey<Level> dimension;
    private final MarkType type;
    private final Vec3 interactionPosition;

    public MarkedPoint(BlockPos pos, ResourceKey<Level> dimension, MarkType type, Vec3 interactionPosition) {
        this.pos = pos.immutable();
        this.dimension = dimension;
        this.type = type;
        this.interactionPosition = interactionPosition;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public MarkType getType() {
        return type;
    }

    public Vec3 getInteractionPosition() {
        return interactionPosition;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MarkedPoint other)) {
            return false;
        }
        return pos.equals(other.pos) && dimension.equals(other.dimension) && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, dimension, type);
    }
}
