package com.restaurantmanager.item;

import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.seating.RestaurantSeatingApi;
import com.restaurantmanager.seating.SeatingMarkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
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
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
                                java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.literal("Chairs: blue outlines").withStyle(style -> style.withColor(0x0072B2)));
        tooltip.add(Component.literal("Tables: orange outlines").withStyle(style -> style.withColor(0xD55E00)));
        tooltip.add(Component.literal("Click a chair, then a table. Sneak-click to unlink.")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        RestaurantManager manager = RestaurantManager.get(level);

        if (context.getPlayer().isShiftKeyDown()) {
            SeatingMarkSavedData data = SeatingMarkSavedData.get(level);
            if (!data.isSeat(clickedPos) && !data.isTable(clickedPos)) {
                context.getPlayer().displayClientMessage(Component.literal("No chair or table marker found here."), true);
                return InteractionResult.CONSUME;
            }
            boolean wasSeat = data.isSeat(clickedPos);
            manager.removeSeatTableLinkAt(clickedPos);
            RestaurantSeatingApi.clearSeatOrTable(clickedPos, level);
            if (stack.hasTag()) {
                stack.getTag().remove(SELECTED_SEAT);
            }
            context.getPlayer().displayClientMessage(Component.literal((wasSeat ? "Chair" : "Table") + " marker removed."), true);
            level.sendParticles(ParticleTypes.SMOKE, clickedPos.getX() + 0.5,
                    clickedPos.getY() + 1.0, clickedPos.getZ() + 0.5, 8,
                    0.2, 0.2, 0.2, 0.0);
            return InteractionResult.CONSUME;
        }

        if (!stack.hasTag() || !stack.getTag().contains(SELECTED_SEAT)) {
            if (manager.findForLayoutBlock(clickedPos).isEmpty()) {
                context.getPlayer().displayClientMessage(Component.literal("Chair must be inside or near a restaurant."), true);
                return InteractionResult.CONSUME;
            }
            RestaurantSeatingApi.markSeat(clickedPos, level);
            stack.getOrCreateTag().put(SELECTED_SEAT, NbtUtils.writeBlockPos(clickedPos));
            context.getPlayer().displayClientMessage(Component.literal("Chair " + formatPos(clickedPos) + " selected. Now click a table."), true);
            return InteractionResult.CONSUME;
        }

        BlockPos seatPos = NbtUtils.readBlockPos(stack.getTag().getCompound(SELECTED_SEAT));
        if (manager.linkSeatToTable(seatPos, clickedPos).isEmpty()) {
            RestaurantSeatingApi.unmarkSeat(seatPos, level);
            stack.getTag().remove(SELECTED_SEAT);
            context.getPlayer().displayClientMessage(Component.literal("Chair and table must belong to the same restaurant."), true);
            return InteractionResult.CONSUME;
        }
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
