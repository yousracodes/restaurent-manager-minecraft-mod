package com.restaurantmanager.setup;

import com.restaurantmanager.RestaurantManagerMod;
import com.restaurantmanager.entity.CustomerEntity;
import com.restaurantmanager.entity.DineInCustomerEntity;
import com.restaurantmanager.entity.TakeoutCustomerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RestaurantManagerMod.MODID);

    public static final RegistryObject<EntityType<DineInCustomerEntity>> CUSTOMER = ENTITIES.register("customer_entity", () ->
            EntityType.Builder.<DineInCustomerEntity>of(DineInCustomerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("restaurantmanager:customer_entity")
    );

    public static final RegistryObject<EntityType<DineInCustomerEntity>> DINE_IN_CUSTOMER = ENTITIES.register("dine_in_customer", () ->
            EntityType.Builder.<DineInCustomerEntity>of(DineInCustomerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("restaurantmanager:dine_in_customer")
    );

    public static final RegistryObject<EntityType<TakeoutCustomerEntity>> TAKEOUT_CUSTOMER = ENTITIES.register("takeout_customer", () ->
            EntityType.Builder.<TakeoutCustomerEntity>of(TakeoutCustomerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("restaurantmanager:takeout_customer")
    );

    public static List<EntityType<? extends CustomerEntity>> customerTypes() {
        return List.of(CUSTOMER.get(), DINE_IN_CUSTOMER.get(), TAKEOUT_CUSTOMER.get());
    }
}
