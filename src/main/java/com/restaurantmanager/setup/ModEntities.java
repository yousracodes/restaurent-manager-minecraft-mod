package com.restaurantmanager.setup;

import com.restaurantmanager.RestaurantManagerMod;
import com.restaurantmanager.entity.CustomerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RestaurantManagerMod.MODID);

    public static final RegistryObject<EntityType<CustomerEntity>> CUSTOMER = ENTITIES.register("customer_entity", () ->
            EntityType.Builder.<CustomerEntity>of(CustomerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("restaurantmanager:customer_entity")
    );
}
