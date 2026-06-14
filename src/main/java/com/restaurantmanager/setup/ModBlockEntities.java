package com.restaurantmanager.setup;

import com.restaurantmanager.RestaurantManagerMod;
import com.restaurantmanager.block.RestaurantSignBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RestaurantManagerMod.MODID);

    public static final RegistryObject<BlockEntityType<RestaurantSignBlockEntity>> RESTAURANT_SIGN =
            BLOCK_ENTITIES.register("restaurant_sign", () ->
                    BlockEntityType.Builder.of(RestaurantSignBlockEntity::new, ModBlocks.RESTAURANT_SIGN.get()).build(null)
            );
}
