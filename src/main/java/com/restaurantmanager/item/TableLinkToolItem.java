package com.restaurantmanager.item;

import com.restaurantmanager.seating.RestaurantSeatingApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class TableLinkToolItem extends Item {
    private static final String SELECTED_SEAT = "SelectedSeat";

    public TableLinkToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (!stack.hasTag() || !stack.getTag().contains(SELECTED_SEAT)) {
            RestaurantSeatingApi.markSeat(clickedPos, level);
            stack.getOrCreateTag().put(SELECTED_SEAT, NbtUtils.writeBlockPos(clickedPos));
            context.getPlayer().displayClientMessage(Component.literal("Chair " + formatPos(clickedPos) + " selected. Now click a table."), true);
            return InteractionResult.CONSUME;
        }

        BlockPos seatPos = NbtUtils.readBlockPos(stack.getTag().getCompound(SELECTED_SEAT));
        RestaurantSeatingApi.markTable(clickedPos, level);
        RestaurantSeatingApi.linkSeatToTable(seatPos, clickedPos, level);
        stack.getTag().remove(SELECTED_SEAT);
        context.getPlayer().displayClientMessage(Component.literal("Chair " + formatPos(seatPos) + " linked to table " + formatPos(clickedPos) + "."), true);
        return InteractionResult.CONSUME;
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
