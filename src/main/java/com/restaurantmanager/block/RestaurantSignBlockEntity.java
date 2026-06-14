package com.restaurantmanager.block;

import com.restaurantmanager.setup.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

public class RestaurantSignBlockEntity extends BlockEntity {
    public static final String RESTAURANT_ID_KEY = "RestaurantId";

    private UUID restaurantId;

    public RestaurantSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESTAURANT_SIGN.get(), pos, state);
    }

    public Optional<UUID> getRestaurantId() {
        return Optional.ofNullable(restaurantId);
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.restaurantId = tag.hasUUID(RESTAURANT_ID_KEY) ? tag.getUUID(RESTAURANT_ID_KEY) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (restaurantId != null) {
            tag.putUUID(RESTAURANT_ID_KEY, restaurantId);
        }
    }
}
