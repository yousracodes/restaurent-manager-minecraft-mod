package com.restaurantmanager.item;

import com.restaurantmanager.entity.CustomerEntity;
import com.restaurantmanager.menu.TakeoutPackageMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TakeoutPackageItem extends Item {
    private static final String FOOD_TAG = "Food";
    private static final String FOODS_TAG = "Foods";
    private static final int SLOT_COUNT = 3;

    public TakeoutPackageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof CustomerEntity customer)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return customer.receiveTakeoutPackage(player, stack) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack packageStack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(packageStack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider((id, inventory, openedPlayer) -> new TakeoutPackageMenu(id, inventory, hand),
                            Component.literal("Takeout Package")),
                    buffer -> buffer.writeEnum(hand)
            );
        }
        return InteractionResultHolder.consume(packageStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> foods = getFoods(stack);
        if (foods.isEmpty()) {
            tooltip.add(Component.literal("Empty package").withStyle(ChatFormatting.GRAY));
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

    public static ItemStack getFood(ItemStack stack) {
        List<ItemStack> foods = getFoods(stack);
        return foods.isEmpty() ? ItemStack.EMPTY : foods.get(0);
    }

    public static List<ItemStack> getFoods(ItemStack stack) {
        List<ItemStack> foods = new ArrayList<>();
        if (!stack.hasTag()) {
            return foods;
        }
        CompoundTag tag = stack.getTag();
        if (tag.contains(FOODS_TAG, Tag.TAG_LIST)) {
            ListTag list = tag.getList(FOODS_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack food = ItemStack.of(list.getCompound(i));
                if (!food.isEmpty()) {
                    foods.add(food);
                }
            }
            return foods;
        }
        if (tag.contains(FOOD_TAG, Tag.TAG_COMPOUND)) {
            ItemStack legacyFood = ItemStack.of(tag.getCompound(FOOD_TAG));
            if (!legacyFood.isEmpty()) {
                foods.add(legacyFood);
            }
        }
        return foods;
    }

    public static void setFood(ItemStack packageStack, ItemStack foodStack) {
        CompoundTag tag = packageStack.getOrCreateTag();
        tag.put(FOOD_TAG, foodStack.copyWithCount(1).save(new CompoundTag()));
    }

    public static void clearFood(ItemStack packageStack) {
        if (packageStack.hasTag()) {
            packageStack.getTag().remove(FOOD_TAG);
            packageStack.getTag().remove(FOODS_TAG);
        }
    }

    public static void loadFoods(ItemStack packageStack, Container container) {
        List<ItemStack> foods = getFoods(packageStack);
        for (int i = 0; i < SLOT_COUNT; i++) {
            container.setItem(i, i < foods.size() ? foods.get(i).copyWithCount(1) : ItemStack.EMPTY);
        }
    }

    public static void saveFoods(ItemStack packageStack, Container container) {
        CompoundTag tag = packageStack.getOrCreateTag();
        ListTag list = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                list.add(stack.copyWithCount(1).save(new CompoundTag()));
            }
        }
        tag.remove(FOOD_TAG);
        if (list.isEmpty()) {
            tag.remove(FOODS_TAG);
        } else {
            tag.put(FOODS_TAG, list);
        }
    }
}
