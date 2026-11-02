package com.restaurantmanager.menu;

import com.restaurantmanager.item.TakeoutPackageItem;
import com.restaurantmanager.setup.ModItems;
import com.restaurantmanager.setup.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

public class TakeoutPackageMenu extends AbstractContainerMenu {
    private static final int PACKAGE_SLOT_COUNT = 3;
    private final InteractionHand hand;
    private final Container packageContainer;
    private final ItemStack openedPackage;
    private final int lockedSlot;
    private boolean loading = true;

    public TakeoutPackageMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readEnum(InteractionHand.class));
    }

    public TakeoutPackageMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(ModMenus.TAKEOUT_PACKAGE.get(), containerId);
        this.hand = hand;
        this.openedPackage = inventory.player.getItemInHand(hand);
        this.lockedSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : 40;
        this.packageContainer = new SimpleContainer(PACKAGE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                savePackage(inventory.player);
            }
        };

        TakeoutPackageItem.loadFoods(openedPackage, packageContainer);
        loading = false;

        addSlot(new FoodSlot(packageContainer, 0, 77, 58));
        addSlot(new FoodSlot(packageContainer, 1, 120, 58));
        addSlot(new FoodSlot(packageContainer, 2, 163, 58));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(inventorySlot(inventory, column + row * 9 + 9, 48 + column * 18, 145 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(inventorySlot(inventory, column, 48 + column * 18, 203));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack held = player.getItemInHand(hand);
        return held.getItem() == ModItems.TAKEOUT_PACKAGE.get()
                && held.getCount() == 1
                && (player.level().isClientSide() || held == openedPackage);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()
                || (slots.get(index).container == player.getInventory()
                && slots.get(index).getSlotIndex() == lockedSlot)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < PACKAGE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PACKAGE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.isEdible()) {
            if (!moveItemStackTo(stack, 0, PACKAGE_SLOT_COUNT, false)) {
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
        savePackage(player);
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        savePackage(player);
    }

    private void savePackage(Player player) {
        if (!loading && !player.level().isClientSide() && stillValid(player)) {
            TakeoutPackageItem.saveFoods(openedPackage, packageContainer);
        }
    }

    private Slot inventorySlot(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return index != lockedSlot && super.mayPickup(player);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return index != lockedSlot && super.mayPlace(stack);
            }
        };
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || (type == ClickType.SWAP && button == lockedSlot)) {
            return;
        }
        super.clicked(slot, button, type, player);
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
