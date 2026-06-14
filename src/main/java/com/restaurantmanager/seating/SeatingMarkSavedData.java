package com.restaurantmanager.seating;

import com.restaurantmanager.RestaurantManagerMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SeatingMarkSavedData extends SavedData {
    private static final String KEY = RestaurantManagerMod.MODID + "_seating_marks";

    private final Map<BlockPos, MarkedPoint> marks = new HashMap<>();
    private final Map<BlockPos, UUID> occupiedSeats = new HashMap<>();

    public static SeatingMarkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(tag -> load(tag, level.dimension()), SeatingMarkSavedData::new, KEY);
    }

    private static SeatingMarkSavedData load(CompoundTag compound, ResourceKey<Level> fallbackDimension) {
        SeatingMarkSavedData data = new SeatingMarkSavedData();
        ListTag list = compound.getList("marks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(tag.getCompound("pos"));
            MarkType type = MarkType.valueOf(tag.getString("type"));
            ResourceKey<Level> dimension = readDimension(tag, fallbackDimension);
            Vec3 interactionPosition = new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
            data.marks.put(pos, new MarkedPoint(pos, dimension, type, interactionPosition));
            if (tag.hasUUID("occupiedBy") && type == MarkType.SEAT) {
                data.occupiedSeats.put(pos, tag.getUUID("occupiedBy"));
            }
        }
        return data;
    }

    private static ResourceKey<Level> readDimension(CompoundTag tag, ResourceKey<Level> fallbackDimension) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("dimension"));
        if (id == null) {
            return fallbackDimension;
        }
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
    }

    public void mark(MarkedPoint point) {
        marks.put(point.getPos(), point);
        if (point.getType() != MarkType.SEAT) {
            occupiedSeats.remove(point.getPos());
        }
        setDirty();
    }

    public void unmark(BlockPos pos) {
        if (marks.remove(pos) != null) {
            occupiedSeats.remove(pos);
            setDirty();
        }
    }

    public boolean isSeat(BlockPos pos) {
        return isType(pos, MarkType.SEAT);
    }

    public boolean isTable(BlockPos pos) {
        return isType(pos, MarkType.TABLE);
    }

    public boolean isType(BlockPos pos, MarkType type) {
        MarkedPoint point = marks.get(pos);
        return point != null && point.getType() == type;
    }

    public Optional<MarkedPoint> getPoint(BlockPos pos) {
        return Optional.ofNullable(marks.get(pos));
    }

    public Collection<MarkedPoint> getAllPoints() {
        return List.copyOf(marks.values());
    }

    public List<MarkedPoint> getNearbySeats(BlockPos restaurantCenter, double radius) {
        double radiusSqr = radius * radius;
        return marks.values().stream()
                .filter(point -> point.getType() == MarkType.SEAT)
                .filter(point -> point.getPos().distSqr(restaurantCenter) <= radiusSqr)
                .sorted(Comparator.comparingDouble(point -> point.getPos().distSqr(restaurantCenter)))
                .toList();
    }

    public Optional<MarkedPoint> findNearestAvailableSeat(BlockPos center, double radius) {
        return getNearbySeats(center, radius).stream()
                .filter(point -> !isSeatOccupied(point.getPos()))
                .findFirst();
    }

    public boolean occupySeat(BlockPos pos, UUID entityId) {
        if (!isSeat(pos) || isSeatOccupied(pos)) {
            return false;
        }
        occupiedSeats.put(pos, entityId);
        setDirty();
        return true;
    }

    public void releaseSeat(BlockPos pos, UUID entityId) {
        UUID occupant = occupiedSeats.get(pos);
        if (entityId.equals(occupant)) {
            occupiedSeats.remove(pos);
            setDirty();
        }
    }

    public boolean isSeatOccupied(BlockPos pos) {
        return occupiedSeats.containsKey(pos);
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag list = new ListTag();
        for (MarkedPoint point : new ArrayList<>(marks.values())) {
            CompoundTag tag = new CompoundTag();
            tag.put("pos", NbtUtils.writeBlockPos(point.getPos()));
            tag.putString("dimension", point.getDimension().location().toString());
            tag.putString("type", point.getType().name());
            tag.putDouble("x", point.getInteractionPosition().x);
            tag.putDouble("y", point.getInteractionPosition().y);
            tag.putDouble("z", point.getInteractionPosition().z);
            UUID occupant = occupiedSeats.get(point.getPos());
            if (occupant != null && point.getType() == MarkType.SEAT) {
                tag.putUUID("occupiedBy", occupant);
            }
            list.add(tag);
        }
        compound.put("marks", list);
        return compound;
    }
}
