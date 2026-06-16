package com.restaurantmanager.setup;

import com.restaurantmanager.RestaurantManagerMod;
import com.restaurantmanager.item.EntranceToolItem;
import com.restaurantmanager.item.ExitToolItem;
import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.item.TableLinkToolItem;
import com.restaurantmanager.item.TakeoutPackageItem;
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

    public static final RegistryObject<Item> PLATE = ITEMS.register("plate", () ->
            new BlockItem(ModBlocks.PLATE.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> CASH_REGISTER = ITEMS.register("cash_register", () ->
            new BlockItem(ModBlocks.CASH_REGISTER.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> RESTAURANT_MENU = ITEMS.register("restaurant_menu", () ->
            new RestaurantMenuItem(new Item.Properties())
    );

    public static final RegistryObject<Item> ENTRANCE_TOOL = ITEMS.register("entrance_tool", () ->
            new EntranceToolItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> EXIT_TOOL = ITEMS.register("exit_tool", () ->
            new ExitToolItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> TABLE_LINK_TOOL = ITEMS.register("table_link_tool", () ->
            new TableLinkToolItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> TAKEOUT_PACKAGE = ITEMS.register("takeout_package", () ->
            new TakeoutPackageItem(new Item.Properties().stacksTo(16))
    );
}
