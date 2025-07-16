package dev.khloeleclair.skulkmuffler.common.blocks;

import com.mojang.serialization.MapCodec;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.network.CustomPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MufflerBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<MufflerBlock> CODEC = simpleCodec(MufflerBlock::new);
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    protected static final VoxelShape FLOOR_SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
            Block.box(2.0, 4.0, 2.0, 14.0, 8.0, 14.0)
            );

    protected static final VoxelShape CEILING_SHAPE = Shapes.or(
            Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(2.0, 8.0, 2.0, 14.0, 12.0, 14.0)
    );

    protected static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(0,0,12,16,16,16),
            Block.box(2, 2, 8, 14, 14, 12)
    );

    protected static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 16, 4),
            Block.box(2, 2, 4, 14, 14, 8)
    );

    protected static final VoxelShape EAST_SHAPE = Shapes.or(
            Block.box(0, 0, 0, 4, 16, 16),
            Block.box(4, 2, 2, 8, 14, 14)
    );

    protected static final VoxelShape WEST_SHAPE = Shapes.or(
            Block.box(12, 0, 0, 16, 16, 16),
            Block.box(8, 2, 2, 12, 14, 14)
    );

    //region Construction and State / Rotation

    public MufflerBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(ENABLED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, ENABLED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        if (state == null)
            state = defaultBlockState();

        return state.setValue(ENABLED, ! isPowered(state, context.getLevel(), context.getClickedPos()));
    }

    public boolean isPowered(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        Direction banned = switch (state.getValue(FACE)) {
            case WALL -> state.getValue(FACING);
            case FLOOR -> Direction.UP;
            default -> Direction.DOWN;
        };

        for(Direction dir : SignalGetter.DIRECTIONS) {
            if (dir != banned && level.getSignal(pos.relative(dir), dir) > 0)
                return true;
        }

        return false;
    }


    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        switch(state.getValue(FACE)) {
            case WALL:
                switch(state.getValue(FACING)) {
                    case EAST:
                        return EAST_SHAPE;
                    case WEST:
                        return WEST_SHAPE;
                    case SOUTH:
                        return SOUTH_SHAPE;
                    case NORTH:
                    default:
                        return NORTH_SHAPE;
                }
            case FLOOR:
                return FLOOR_SHAPE;
            case CEILING:
            default:
                return CEILING_SHAPE;
        }
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return true;
    }

    //endregion

    //region Entity Block Stuff

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (player.isSteppingCarefully())
            return InteractionResult.PASS;

        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            CustomPackets.CHANNEL.serverHandle(sp).send(new CustomPackets.OpenMufflerMenu(pos));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull MapCodec<? extends MufflerBlock> codec() { return CODEC; }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean triggerEvent(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
    }

    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MufflerBlockEntity(pos, state);
    }

    //endregion

    @Override
    protected void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block neighborBlock, @NotNull BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            boolean enabled = state.getValue(ENABLED);
            boolean should_enable = ! isPowered(state, level, pos);
            if (should_enable != enabled) {
                level.setBlock(pos, state.setValue(ENABLED, should_enable), 3);
            }
        }
    }

}
