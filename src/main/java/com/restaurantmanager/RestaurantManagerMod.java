package com.restaurantmanager;

import com.restaurantmanager.food.FoodRegistryScanner;
import com.restaurantmanager.block.RestaurantSignProtectionEvents;
import com.restaurantmanager.entity.CustomerEntity;
import com.restaurantmanager.entity.CustomerSpawnEvents;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.setup.ModBlocks;
import com.restaurantmanager.setup.ModBlockEntities;
import com.restaurantmanager.setup.ModCreativeTabs;
import com.restaurantmanager.setup.ModEntities;
import com.restaurantmanager.setup.ModItems;
import com.restaurantmanager.setup.ModMenus;
import com.restaurantmanager.seating.SeatingDebugEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RestaurantManagerMod.MODID)
public class RestaurantManagerMod {
    public static final String MODID = "restaurantmanager";
    public static final Logger LOGGER = LogManager.getLogger();

    public RestaurantManagerMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModMenus.MENUS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModCreativeTabs.CREATIVE_TABS.register(bus);

        bus.addListener(this::setup);
        bus.addListener(this::onEntityAttributeCreation);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new RestaurantSignProtectionEvents());
        MinecraftForge.EVENT_BUS.register(new CustomerSpawnEvents());
        MinecraftForge.EVENT_BUS.register(new SeatingDebugEvents());
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.setup();
            FoodRegistryScanner.scanEdibleFoods();
        });
    }

    private void onEntityAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.CUSTOMER.get(), CustomerEntity.createAttributes().build());
        event.put(ModEntities.DINE_IN_CUSTOMER.get(), CustomerEntity.createAttributes().build());
        event.put(ModEntities.TAKEOUT_CUSTOMER.get(), CustomerEntity.createAttributes().build());
    }
}
