package com.restaurantmanager.menu;

import com.restaurantmanager.item.PlateItem;
import com.restaurantmanager.setup.ModItems;
import com.restaurantmanager.setup.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PlateMenu extends AbstractContainerMenu {
    private static final int PLATE_SLOT_COUNT = 3;
    private final InteractionHand hand;
    private final Container plateContainer;

    public PlateMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readEnum(InteractionHand.class));
    }

    public PlateMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(ModMenus.PLATE.get(), containerId);
        this.hand = hand;
        this.plateContainer = new SimpleContainer(PLATE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                savePlate(inventory.player);
            }
        };

        PlateItem.loadFoods(inventory.player.getItemInHand(hand), plateContainer);

        addSlot(new FoodSlot(plateContainer, 0, 59, 63));
        addSlot(new FoodSlot(plateContainer, 1, 121, 63));
        addSlot(new FoodSlot(plateContainer, 2, 183, 63));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 48 + column * 18, 153 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 48 + column * 18, 211));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() == ModItems.PLATE.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < PLATE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLATE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.isEdible()) {
            if (!moveItemStackTo(stack, 0, PLATE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        savePlate(player);
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        savePlate(player);
    }

    private void savePlate(Player player) {
        PlateItem.saveFoods(player.getItemInHand(hand), plateContainer);
    }

    private static class FoodSlot extends Slot {
        public FoodSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.isEdible();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
