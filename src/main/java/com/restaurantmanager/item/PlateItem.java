package com.restaurantmanager.item;

import com.restaurantmanager.menu.PlateMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PlateItem extends BlockItem {
    private static final String FOODS_TAG = "Foods";
    private static final int SLOT_COUNT = 3;

    public PlateItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack plateStack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(plateStack);
        }

        openPlateScreen(level, player, hand);
        return InteractionResultHolder.consume(plateStack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return super.useOn(context);
        }
        if (!context.getLevel().isClientSide()) {
            openPlateScreen(context.getLevel(), context.getPlayer(), context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    private void openPlateScreen(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        NetworkHooks.openScreen(
                serverPlayer,
                new SimpleMenuProvider((id, inventory, openedPlayer) -> new PlateMenu(id, inventory, hand),
                        Component.literal("Plate")),
                buffer -> buffer.writeEnum(hand)
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> foods = getFoods(stack);
        if (foods.isEmpty()) {
            tooltip.add(Component.literal("Empty plate").withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.literal("Contains:").withStyle(ChatFormatting.GRAY));
        for (ItemStack food : foods) {
            tooltip.add(Component.literal("- ")
                    .append(food.getHoverName())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    public static boolean hasFood(ItemStack stack) {
        return !getFoods(stack).isEmpty();
    }

    public static List<ItemStack> getFoods(ItemStack stack) {
        List<ItemStack> foods = new ArrayList<>();
        if (!stack.hasTag()) {
            return foods;
        }

        CompoundTag tag = stack.getTag();
        if (!tag.contains(FOODS_TAG, Tag.TAG_LIST)) {
            return foods;
        }

        ListTag list = tag.getList(FOODS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack food = ItemStack.of(list.getCompound(i));
            if (!food.isEmpty()) {
                foods.add(food);
            }
        }
        return foods;
    }

    public static void clearFood(ItemStack plateStack) {
        if (plateStack.hasTag()) {
            plateStack.getTag().remove(FOODS_TAG);
        }
    }

    public static void setFoods(ItemStack plateStack, List<ItemStack> foods) {
        CompoundTag tag = plateStack.getOrCreateTag();
        ListTag list = new ListTag();
        for (int i = 0; i < SLOT_COUNT && i < foods.size(); i++) {
            ItemStack stack = foods.get(i);
            if (!stack.isEmpty()) {
                list.add(stack.copyWithCount(1).save(new CompoundTag()));
            }
        }

        if (list.isEmpty()) {
            tag.remove(FOODS_TAG);
        } else {
            tag.put(FOODS_TAG, list);
        }
    }

    public static void loadFoods(ItemStack plateStack, Container container) {
        List<ItemStack> foods = getFoods(plateStack);
        for (int i = 0; i < SLOT_COUNT; i++) {
            container.setItem(i, i < foods.size() ? foods.get(i).copyWithCount(1) : ItemStack.EMPTY);
        }
    }

    public static void saveFoods(ItemStack plateStack, Container container) {
        CompoundTag tag = plateStack.getOrCreateTag();
        ListTag list = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                list.add(stack.copyWithCount(1).save(new CompoundTag()));
            }
        }

        if (list.isEmpty()) {
            tag.remove(FOODS_TAG);
        } else {
            tag.put(FOODS_TAG, list);
        }
    }
}
