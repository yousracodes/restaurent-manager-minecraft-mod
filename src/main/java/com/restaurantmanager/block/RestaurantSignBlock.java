package com.restaurantmanager.block;

import com.restaurantmanager.restaurant.Restaurant;
import com.restaurantmanager.restaurant.RestaurantDetector;
import com.restaurantmanager.restaurant.RestaurantManager;
import com.restaurantmanager.item.RestaurantMenuItem;
import com.restaurantmanager.network.ModNetwork;
import com.restaurantmanager.network.packet.OpenRestaurantManagementPacket;
import com.restaurantmanager.setup.ModEntities;
import com.restaurantmanager.setup.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.UUID;

public class RestaurantSignBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public RestaurantSignBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && placer instanceof ServerPlayer player) {
            RestaurantManager manager = RestaurantManager.get(serverLevel);
            Optional<Restaurant> detected = RestaurantDetector.detect(serverLevel, pos);
            if (detected.isPresent()) {
                UUID itemRestaurantId = getRestaurantId(stack).orElse(null);
                Restaurant detectedRestaurant = detected.get();
                Optional<Restaurant> savedRestaurant = itemRestaurantId == null ? Optional.empty() : manager.findById(itemRestaurantId);
                if (manager.overlaps(detectedRestaurant.getBounds(), itemRestaurantId)) {
                    player.sendSystemMessage(Component.literal("Restaurant overlaps another active restaurant. Move the sign or resize the room."));
                    setSignRestaurantId(level, pos, itemRestaurantId);
                    return;
                }

                Restaurant value = savedRestaurant.orElse(detectedRestaurant);
                if (savedRestaurant.isPresent()) {
                    value.updateAnchor(pos, detectedRestaurant.getBounds());
                    manager.setDirty();
                } else {
                    manager.addRestaurant(value);
                }
                setSignRestaurantId(level, pos, value.getId());
                if (!value.hasOwner()) {
                    value.setIdentity(player.getName().getString() + "'s Restaurant", player.getUUID(), player.getName().getString());
                    manager.setDirty();
                }
                player.sendSystemMessage(Component.literal(createDetectionMessage(value)));
                spawnTestCustomer(serverLevel, value);
            } else {
                getRestaurantId(stack).ifPresent(id -> setSignRestaurantId(level, pos, id));
                player.sendSystemMessage(Component.literal("No restaurant detected. Place the sign facing away from a closed room with at least a 3x3x3 air interior behind it."));
                for (String line : RestaurantDetector.debugDetection(serverLevel, pos)) {
                    player.sendSystemMessage(Component.literal(line));
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && held.is(ModItems.RESTAURANT_MENU.get())) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                RestaurantManager manager = RestaurantManager.get(serverLevel);
                Optional<Restaurant> restaurant = manager.findBySignPos(pos);
                if (restaurant.isPresent()) {
                    Restaurant value = restaurant.get();
                    value.getMenu().setTitle(RestaurantMenuItem.getMenuTitle(held));
                    value.getMenu().replaceItems(RestaurantMenuItem.getMenuItems(held));
                    manager.setDirty();
                    serverPlayer.sendSystemMessage(Component.literal("Restaurant menu applied: "
                            + value.getMenu().getItems().size() + " item(s)."));
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("No registered restaurant found for this sign."));
                }
            }
            return InteractionResult.CONSUME;
        }

        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            RestaurantManager manager = RestaurantManager.get(serverLevel);
            Optional<Restaurant> restaurant = manager.findBySignPos(pos);
            if (restaurant.isPresent()) {
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new OpenRestaurantManagementPacket(pos, restaurant.get())
                );
            } else {
                serverPlayer.sendSystemMessage(Component.literal("No registered restaurant found for this sign."));
            }
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RestaurantSignBlockEntity(pos, state);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            getAnchoredRestaurant(level, pos).ifPresent(restaurant -> dropLinkedSign(level, pos, restaurant));
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            RestaurantManager.get(serverLevel).detachRestaurantAt(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static String createDetectionMessage(Restaurant restaurant) {
        BlockPos signPos = restaurant.getSignPos();
        AABB bounds = restaurant.getBounds();
        return "Restaurant detected at sign "
                + signPos.getX() + ", " + signPos.getY() + ", " + signPos.getZ()
                + " with interior from "
                + (int) bounds.minX + ", " + (int) bounds.minY + ", " + (int) bounds.minZ
                + " to "
                + ((int) bounds.maxX - 1) + ", " + ((int) bounds.maxY - 1) + ", " + ((int) bounds.maxZ - 1)
                + ".";
    }

    private static void spawnTestCustomer(ServerLevel level, Restaurant restaurant) {
        if (!restaurant.isOpen()) {
            return;
        }
        BlockPos signPos = restaurant.getSignPos();
        BlockState signState = level.getBlockState(signPos);
        Direction facing = signState.hasProperty(FACING) ? signState.getValue(FACING) : Direction.NORTH;
        ModEntities.CUSTOMER.get().spawn(
                level,
                signPos.relative(facing).above(),
                MobSpawnType.TRIGGERED
        );
    }

    private static Optional<UUID> getRestaurantId(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().hasUUID(RestaurantSignBlockEntity.RESTAURANT_ID_KEY)) {
            return Optional.empty();
        }
        return Optional.of(stack.getTag().getUUID(RestaurantSignBlockEntity.RESTAURANT_ID_KEY));
    }

    private static void setRestaurantId(ItemStack stack, UUID restaurantId) {
        stack.getOrCreateTag().putUUID(RestaurantSignBlockEntity.RESTAURANT_ID_KEY, restaurantId);
    }

    private static void setSignRestaurantId(Level level, BlockPos pos, @Nullable UUID restaurantId) {
        if (restaurantId == null) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RestaurantSignBlockEntity signBlockEntity) {
            signBlockEntity.setRestaurantId(restaurantId);
        }
    }

    private static Optional<Restaurant> getAnchoredRestaurant(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        RestaurantManager manager = RestaurantManager.get(serverLevel);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RestaurantSignBlockEntity signBlockEntity) {
            Optional<UUID> restaurantId = signBlockEntity.getRestaurantId();
            if (restaurantId.isPresent()) {
                return manager.findById(restaurantId.get());
            }
        }
        return manager.findBySignPos(pos);
    }

    private static void dropLinkedSign(Level level, BlockPos pos, Restaurant restaurant) {
        ItemStack linkedSign = new ItemStack(ModItems.RESTAURANT_SIGN.get());
        setRestaurantId(linkedSign, restaurant.getId());
        ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, linkedSign);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }
}
