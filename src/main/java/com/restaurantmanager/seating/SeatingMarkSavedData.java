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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SeatingMarkSavedData extends SavedData {
    private static final String KEY = RestaurantManagerMod.MODID + "_seating_marks";

    private final Map<BlockPos, MarkedPoint> marks = new HashMap<>();
    private final Map<MarkType, List<MarkedPoint>> marksByType = new EnumMap<>(MarkType.class);
    private final Map<BlockPos, UUID> occupiedSeats = new HashMap<>();
    private final Map<BlockPos, UUID> occupiedQueueSpots = new HashMap<>();
    private final Map<BlockPos, UUID> occupiedCounters = new HashMap<>();
    private final Map<BlockPos, BlockPos> seatTables = new HashMap<>();

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
            data.putMark(new MarkedPoint(pos, dimension, type, interactionPosition));
            if (tag.hasUUID("occupiedBy")) {
                UUID occupant = tag.getUUID("occupiedBy");
                if (type == MarkType.SEAT) {
                    data.occupiedSeats.put(pos, occupant);
                } else if (type == MarkType.QUEUE) {
                    data.occupiedQueueSpots.put(pos, occupant);
                } else if (type == MarkType.COUNTER) {
                    data.occupiedCounters.put(pos, occupant);
                }
            }
            if (tag.contains("tablePos") && type == MarkType.SEAT) {
                data.seatTables.put(pos, NbtUtils.readBlockPos(tag.getCompound("tablePos")));
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
        putMark(point);
        if (point.getType() != MarkType.SEAT) {
            occupiedSeats.remove(point.getPos());
        }
        if (point.getType() != MarkType.QUEUE) {
            occupiedQueueSpots.remove(point.getPos());
        }
        if (point.getType() != MarkType.COUNTER) {
            occupiedCounters.remove(point.getPos());
        }
        if (point.getType() != MarkType.SEAT) {
            seatTables.remove(point.getPos());
        }
        setDirty();
    }

    public void unmark(BlockPos pos) {
        MarkedPoint removed = marks.remove(pos);
        if (removed != null) {
            removeFromTypeIndex(removed);
            occupiedSeats.remove(pos);
            occupiedQueueSpots.remove(pos);
            occupiedCounters.remove(pos);
            seatTables.remove(pos);
            setDirty();
        }
    }

    public boolean isSeat(BlockPos pos) {
        return isType(pos, MarkType.SEAT);
    }

    public boolean isTable(BlockPos pos) {
        return isType(pos, MarkType.TABLE);
    }

    public boolean isQueue(BlockPos pos) {
        return isType(pos, MarkType.QUEUE);
    }

    public boolean isCounter(BlockPos pos) {
        return isType(pos, MarkType.COUNTER);
    }

    public boolean isEntrance(BlockPos pos) {
        return isType(pos, MarkType.ENTRANCE) || isType(pos, MarkType.DOOR_BELL);
    }

    public boolean isExit(BlockPos pos) {
        return isType(pos, MarkType.EXIT) || isType(pos, MarkType.DOOR_BELL);
    }

    public boolean isDoorBell(BlockPos pos) {
        return isType(pos, MarkType.DOOR_BELL);
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
        return getNearbyPoints(restaurantCenter, radius, MarkType.SEAT);
    }

    public List<MarkedPoint> getNearbyTables(BlockPos restaurantCenter, double radius) {
        return getNearbyPoints(restaurantCenter, radius, MarkType.TABLE);
    }

    public List<MarkedPoint> getNearbyQueueSpots(BlockPos restaurantCenter, double radius) {
        return getNearbyPoints(restaurantCenter, radius, MarkType.QUEUE);
    }

    public List<MarkedPoint> getNearbyCounters(BlockPos restaurantCenter, double radius) {
        return getNearbyPoints(restaurantCenter, radius, MarkType.COUNTER);
    }

    public List<MarkedPoint> getNearbyEntrances(BlockPos restaurantCenter, double radius) {
        return getNearbyPoints(restaurantCenter, radius, MarkType.ENTRANCE, MarkType.DOOR_BELL);
    }

    public List<MarkedPoint> getNearbyExits(BlockPos restaurantCenter, double radius) {
        return getNearbyPoints(restaurantCenter, radius, MarkType.EXIT, MarkType.DOOR_BELL);
    }

    private List<MarkedPoint> getNearbyPoints(BlockPos restaurantCenter, double radius, MarkType... types) {
        double radiusSqr = radius * radius;
        List<MarkedPoint> nearbyPoints = new ArrayList<>();
        for (MarkType type : types) {
            for (MarkedPoint point : getPointsByType(type)) {
                if (point.getPos().distSqr(restaurantCenter) <= radiusSqr) {
                    nearbyPoints.add(point);
                }
            }
        }
        nearbyPoints.sort(Comparator.comparingDouble(point -> point.getPos().distSqr(restaurantCenter)));
        return List.copyOf(nearbyPoints);
    }

    public Optional<MarkedPoint> findNearestAvailableSeat(BlockPos center, double radius) {
        return findNearest(center, radius, point -> !isSeatOccupied(point.getPos()), MarkType.SEAT);
    }

    public Optional<MarkedPoint> findNearestAvailableQueueSpot(BlockPos center, double radius) {
        return findNearest(center, radius, point -> !isQueueSpotOccupied(point.getPos()), MarkType.QUEUE);
    }

    public Optional<MarkedPoint> findNearestAvailableCounter(BlockPos center, double radius) {
        return findNearest(center, radius, point -> !isCounterOccupied(point.getPos()), MarkType.COUNTER);
    }

    public Optional<MarkedPoint> findNearestTable(BlockPos center, double radius) {
        return findNearest(center, radius, point -> true, MarkType.TABLE);
    }

    public Optional<MarkedPoint> findNearestEntrance(BlockPos center, double radius) {
        return findNearest(center, radius, point -> true, MarkType.ENTRANCE, MarkType.DOOR_BELL);
    }

    public Optional<MarkedPoint> findNearestExit(BlockPos center, double radius) {
        return findNearest(center, radius, point -> true, MarkType.EXIT, MarkType.DOOR_BELL);
    }

    public Optional<BlockPos> getLinkedTable(BlockPos seatPos) {
        return Optional.ofNullable(seatTables.get(seatPos));
    }

    public void linkSeatToTable(BlockPos seatPos, BlockPos tablePos) {
        if (!isSeat(seatPos) || !isTable(tablePos)) {
            return;
        }
        seatTables.put(seatPos, tablePos.immutable());
        setDirty();
    }

    public boolean occupySeat(BlockPos pos, UUID entityId) {
        if (!isSeat(pos) || isSeatOccupied(pos)) {
            return false;
        }
        occupiedSeats.put(pos, entityId);
        setDirty();
        return true;
    }

    public boolean occupyQueueSpot(BlockPos pos, UUID entityId) {
        if (!isQueue(pos) || isQueueSpotOccupied(pos)) {
            return false;
        }
        occupiedQueueSpots.put(pos, entityId);
        setDirty();
        return true;
    }

    public boolean occupyCounter(BlockPos pos, UUID entityId) {
        if (!isCounter(pos) || isCounterOccupied(pos)) {
            return false;
        }
        occupiedCounters.put(pos, entityId);
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

    public void releaseQueueSpot(BlockPos pos, UUID entityId) {
        UUID occupant = occupiedQueueSpots.get(pos);
        if (entityId.equals(occupant)) {
            occupiedQueueSpots.remove(pos);
            setDirty();
        }
    }

    public void releaseCounter(BlockPos pos, UUID entityId) {
        UUID occupant = occupiedCounters.get(pos);
        if (entityId.equals(occupant)) {
            occupiedCounters.remove(pos);
            setDirty();
        }
    }

    public boolean isSeatOccupied(BlockPos pos) {
        return occupiedSeats.containsKey(pos);
    }

    public boolean isQueueSpotOccupied(BlockPos pos) {
        return occupiedQueueSpots.containsKey(pos);
    }

    public boolean isCounterOccupied(BlockPos pos) {
        return occupiedCounters.containsKey(pos);
    }

    public boolean isFrontOccupiedQueueSpot(BlockPos pos, BlockPos restaurantCenter, double radius) {
        if (!isQueue(pos)) {
            return false;
        }
        return getNearbyQueueSpots(restaurantCenter, radius).stream()
                .filter(point -> occupiedQueueSpots.containsKey(point.getPos()))
                .findFirst()
                .map(point -> point.getPos().equals(pos))
                .orElse(false);
    }

    public Optional<MarkedPoint> findEarlierAvailableQueueSpot(BlockPos currentPos, BlockPos restaurantCenter, double radius) {
        List<MarkedPoint> queue = getNearbyQueueSpots(restaurantCenter, radius);
        for (MarkedPoint point : queue) {
            if (point.getPos().equals(currentPos)) {
                return Optional.empty();
            }
            if (!isQueueSpotOccupied(point.getPos())) {
                return Optional.of(point);
            }
        }
        return Optional.empty();
    }

    private void putMark(MarkedPoint point) {
        MarkedPoint replaced = marks.put(point.getPos(), point);
        if (replaced != null) {
            removeFromTypeIndex(replaced);
        }
        marksByType.computeIfAbsent(point.getType(), type -> new ArrayList<>()).add(point);
    }

    private void removeFromTypeIndex(MarkedPoint point) {
        List<MarkedPoint> points = marksByType.get(point.getType());
        if (points != null) {
            points.removeIf(indexedPoint -> indexedPoint.getPos().equals(point.getPos()));
            if (points.isEmpty()) {
                marksByType.remove(point.getType());
            }
        }
    }

    private List<MarkedPoint> getPointsByType(MarkType type) {
        return marksByType.getOrDefault(type, List.of());
    }

    private Optional<MarkedPoint> findNearest(BlockPos center, double radius, PointFilter filter, MarkType... types) {
        double radiusSqr = radius * radius;
        MarkedPoint nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (MarkType type : types) {
            for (MarkedPoint point : getPointsByType(type)) {
                double distance = point.getPos().distSqr(center);
                if (distance <= radiusSqr && distance < nearestDistance && filter.matches(point)) {
                    nearest = point;
                    nearestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    @FunctionalInterface
    private interface PointFilter {
        boolean matches(MarkedPoint point);
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
            UUID occupant = switch (point.getType()) {
                case SEAT -> occupiedSeats.get(point.getPos());
                case QUEUE -> occupiedQueueSpots.get(point.getPos());
                case COUNTER -> occupiedCounters.get(point.getPos());
                case TABLE, ENTRANCE, EXIT, DOOR_BELL -> null;
            };
            if (occupant != null) {
                tag.putUUID("occupiedBy", occupant);
            }
            BlockPos linkedTable = seatTables.get(point.getPos());
            if (linkedTable != null && point.getType() == MarkType.SEAT) {
                tag.put("tablePos", NbtUtils.writeBlockPos(linkedTable));
            }
            list.add(tag);
        }
        compound.put("marks", list);
        return compound;
    }
}
