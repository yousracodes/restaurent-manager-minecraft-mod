package com.restaurantmanager.block;

import com.restaurantmanager.setup.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class PlateBlockEntity extends BlockEntity {
    public static final int SLOT_COUNT = 3;

    private final NonNullList<ItemStack> meals = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public PlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLATE.get(), pos, state);
    }

    public List<ItemStack> getMeals() {
        return List.copyOf(meals);
    }

    public boolean hasMeals() {
        return meals.stream().anyMatch(stack -> !stack.isEmpty());
    }

    public boolean addMeal(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible()) {
            return false;
        }

        for (int slot = 0; slot < meals.size(); slot++) {
            if (meals.get(slot).isEmpty()) {
                meals.set(slot, stack.copyWithCount(1));
                markContentsChanged();
                return true;
            }
        }
        return false;
    }

    public Optional<ItemStack> findMeal(Item item) {
        return meals.stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() == item)
                .findFirst()
                .map(ItemStack::copy);
    }

    public ItemStack removeMeal(Item item) {
        for (int slot = 0; slot < meals.size(); slot++) {
            ItemStack meal = meals.get(slot);
            if (!meal.isEmpty() && meal.getItem() == item) {
                meals.set(slot, ItemStack.EMPTY);
                markContentsChanged();
                return meal.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack removeLastMeal() {
        for (int slot = meals.size() - 1; slot >= 0; slot--) {
            ItemStack meal = meals.get(slot);
            if (!meal.isEmpty()) {
                meals.set(slot, ItemStack.EMPTY);
                markContentsChanged();
                return meal.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    public void clearMeals() {
        for (int slot = 0; slot < meals.size(); slot++) {
            meals.set(slot, ItemStack.EMPTY);
        }
        markContentsChanged();
    }

    /**
     * Kept for older call sites while the tray API migrates to multi-slot meals.
     */
    public boolean hasMeal() {
        return hasMeals();
    }

    public ItemStack getMeal() {
        return meals.stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
    }

    public ItemStack removeMeal() {
        return removeLastMeal();
    }

    public void clearMeal() {
        clearMeals();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag mealTags = new ListTag();
        for (int slot = 0; slot < meals.size(); slot++) {
            ItemStack meal = meals.get(slot);
            if (!meal.isEmpty()) {
                CompoundTag mealTag = meal.save(new CompoundTag());
                mealTag.putInt("Slot", slot);
                mealTags.add(mealTag);
            }
        }
        if (!mealTags.isEmpty()) {
            tag.put("Meals", mealTags);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int slot = 0; slot < meals.size(); slot++) {
            meals.set(slot, ItemStack.EMPTY);
        }

        ListTag mealTags = tag.getList("Meals", Tag.TAG_COMPOUND);
        for (int i = 0; i < mealTags.size(); i++) {
            CompoundTag mealTag = mealTags.getCompound(i);
            int slot = mealTag.getInt("Slot");
            if (slot >= 0 && slot < meals.size()) {
                meals.set(slot, ItemStack.of(mealTag));
            }
        }

        if (tag.contains("Meal", Tag.TAG_COMPOUND) && meals.stream().allMatch(ItemStack::isEmpty)) {
            meals.set(0, ItemStack.of(tag.getCompound("Meal")));
        }
        updateFilledState();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        load(packet.getTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    private void markContentsChanged() {
        setChanged();
        updateFilledState();
        syncToClient();
    }

    private void updateFilledState() {
        if (level != null && level.getBlockState(worldPosition).getBlock() instanceof PlateBlock) {
            BlockState state = level.getBlockState(worldPosition);
            boolean filled = hasMeals();
            if (state.getValue(PlateBlock.FILLED) != filled) {
                level.setBlock(worldPosition, state.setValue(PlateBlock.FILLED, filled), 3);
            }
        }
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
