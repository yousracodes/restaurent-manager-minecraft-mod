package com.restaurantmanager.item;

import com.restaurantmanager.restaurant.MenuCategory;
import com.restaurantmanager.client.ClientRestaurantScreens;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RestaurantMenuItem extends Item {
    private static final String TITLE_KEY = "RestaurantMenuTitle";
    private static final String CATEGORY_KEY = "RestaurantMenuCategory";
    private static final String ITEMS_KEY = "RestaurantMenuItems";

    public RestaurantMenuItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientRestaurantScreens.openMenuItemEditor(hand, stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String title = getMenuTitle(stack);
        List<ResourceLocation> items = getMenuItems(stack);
        if (!title.isBlank()) {
            tooltip.add(Component.literal(title).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.literal(getMenuCategory(stack).getDisplayName()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(items.size() + " menu item(s)").withStyle(ChatFormatting.GRAY));
    }

    public static String getMenuTitle(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TITLE_KEY);
    }

    public static MenuCategory getMenuCategory(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? MenuCategory.MAIN_DISH : MenuCategory.byName(tag.getString(CATEGORY_KEY));
    }

    public static List<ResourceLocation> getMenuItems(ItemStack stack) {
        List<ResourceLocation> items = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return items;
        }
        ListTag list = tag.getList(ITEMS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                items.add(id);
            }
        }
        return items;
    }

    public static void saveMenu(ItemStack stack, String title, MenuCategory category, List<ResourceLocation> items) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TITLE_KEY, title == null ? "" : title.trim());
        tag.putString(CATEGORY_KEY, (category == null ? MenuCategory.MAIN_DISH : category).name());
        ListTag list = new ListTag();
        for (ResourceLocation item : items) {
            list.add(StringTag.valueOf(item.toString()));
        }
        tag.put(ITEMS_KEY, list);
    }
}
