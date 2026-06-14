package com.restaurantmanager.setup;

import com.restaurantmanager.RestaurantManagerMod;
import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.item.SeatToolItem;
import com.restaurantmanager.item.TableToolItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RestaurantManagerMod.MODID);

    public static final RegistryObject<Item> RESTAURANT_SIGN = ITEMS.register("restaurant_sign", () ->
            new BlockItem(ModBlocks.RESTAURANT_SIGN.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> RESTAURANT_MENU = ITEMS.register("restaurant_menu", () ->
            new RestaurantMenuItem(new Item.Properties())
    );

    public static final RegistryObject<Item> SEAT_TOOL = ITEMS.register("seat_tool", () ->
            new SeatToolItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> TABLE_TOOL = ITEMS.register("table_tool", () ->
            new TableToolItem(new Item.Properties().stacksTo(1))
    );
}
