package com.restaurantmanager.setup;

import com.restaurantmanager.RestaurantManagerMod;
import com.restaurantmanager.menu.PlateMenu;
import com.restaurantmanager.menu.TakeoutPackageMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, RestaurantManagerMod.MODID);

    public static final RegistryObject<MenuType<TakeoutPackageMenu>> TAKEOUT_PACKAGE =
            MENUS.register("takeout_package", () -> IForgeMenuType.create(TakeoutPackageMenu::new));

    public static final RegistryObject<MenuType<PlateMenu>> PLATE =
            MENUS.register("plate", () -> IForgeMenuType.create(PlateMenu::new));
}
