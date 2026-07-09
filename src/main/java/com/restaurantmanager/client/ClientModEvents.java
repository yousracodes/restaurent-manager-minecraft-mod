package com.restaurantmanager.client;

import com.restaurantmanager.RestaurantManagerMod;
import com.restaurantmanager.item.FoodListTooltipComponent;
import com.restaurantmanager.setup.ModBlockEntities;
import com.restaurantmanager.setup.ModEntities;
import com.restaurantmanager.setup.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = RestaurantManagerMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUSTOMER.get(), CustomerRenderer::new);
        event.registerEntityRenderer(ModEntities.DINE_IN_CUSTOMER.get(), CustomerRenderer::new);
        event.registerEntityRenderer(ModEntities.TAKEOUT_CUSTOMER.get(), CustomerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PLATE.get(), PlateBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.TAKEOUT_PACKAGE.get(), TakeoutPackageScreen::new);
            MenuScreens.register(ModMenus.PLATE.get(), PlateScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(FoodListTooltipComponent.class, FoodListClientTooltip::new);
    }
}
