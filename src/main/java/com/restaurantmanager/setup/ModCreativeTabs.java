package com.restaurantmanager.setup;

import com.restaurantmanager.RestaurantManagerMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RestaurantManagerMod.MODID);

    public static final RegistryObject<CreativeModeTab> RESTAURANT_MANAGER = CREATIVE_TABS.register("restaurant_manager", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.restaurantmanager.restaurant_manager"))
                    .icon(() -> new ItemStack(ModItems.RESTAURANT_SIGN.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RESTAURANT_SIGN.get());
                        output.accept(ModItems.PLATE.get());
                        output.accept(ModItems.CASH_REGISTER.get());
                        output.accept(ModItems.RESTAURANT_MENU.get());
                        output.accept(ModItems.ENTRANCE_TOOL.get());
                        output.accept(ModItems.EXIT_TOOL.get());
                        output.accept(ModItems.TABLE_LINK_TOOL.get());
                        output.accept(ModItems.TAKEOUT_PACKAGE.get());
                    })
                    .build()
    );
}
