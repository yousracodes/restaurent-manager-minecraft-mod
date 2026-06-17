package com.restaurantmanager.block;

import com.restaurantmanager.item.PlateItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class PlateBlock extends BaseEntityBlock {
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);

    public PlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FILLED, false));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof PlateBlockEntity plate)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (plate.hasMeals()) {
                ItemStack removed = plate.removeLastMeal();
                if (!removed.isEmpty() && player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.getInventory().placeItemBackInInventory(removed);
                }
                player.displayClientMessage(Component.literal("Removed food from plate."), true);
            } else {
                player.displayClientMessage(Component.literal("Plate is empty."), true);
            }
            return InteractionResult.CONSUME;
        }

        if (held.isEdible()) {
            if (!plate.addMeal(held)) {
                player.displayClientMessage(Component.literal("Plate is full."), true);
                return InteractionResult.CONSUME;
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            player.displayClientMessage(Component.literal("Added food to plate."), true);
            return InteractionResult.CONSUME;
        }

        if (plate.hasMeals()) {
            player.displayClientMessage(Component.literal("Plate contains " + plate.getMeals().stream().filter(stack -> !stack.isEmpty()).count() + "/3 item(s)."), true);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.literal("Put edible food on the plate."), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlateBlockEntity plate) {
            plate.setMeals(PlateItem.getFoods(stack));
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof PlateBlockEntity plate) {
            ItemStack drop = new ItemStack(this);
            PlateItem.setFoods(drop, plate.getMeals());
            popResource(level, pos, drop);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlateBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FILLED);
    }
}
