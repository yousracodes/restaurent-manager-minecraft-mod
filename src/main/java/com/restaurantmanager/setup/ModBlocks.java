package com.restaurantmanager.setup;

import com.restaurantmanager.block.CashRegisterBlock;
import com.restaurantmanager.block.DoorBellBlock;
import com.restaurantmanager.block.RestaurantSignBlock;
import com.restaurantmanager.block.PlateBlock;
import com.restaurantmanager.RestaurantManagerMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RestaurantManagerMod.MODID);

    public static final RegistryObject<Block> RESTAURANT_SIGN = BLOCKS.register("restaurant_sign", () ->
            new RestaurantSignBlock(BlockBehaviour.Properties.of().strength(2.0F).noOcclusion())
    );

    public static final RegistryObject<Block> PLATE = BLOCKS.register("plate", () ->
            new PlateBlock(BlockBehaviour.Properties.of().strength(0.3F).noOcclusion())
    );

    public static final RegistryObject<Block> CASH_REGISTER = BLOCKS.register("cash_register", () ->
            new CashRegisterBlock(BlockBehaviour.Properties.of().strength(1.5F).noOcclusion())
    );

    public static final RegistryObject<Block> DOOR_BELL = BLOCKS.register("door_bell", () ->
            new DoorBellBlock(BlockBehaviour.Properties.of().strength(0.4F).noOcclusion())
    );
}
