package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import pl.asie.computronics.block.entity.TileControlPanel;

public class BlockControlPanel extends BlockComputronics {
    // 8-voxel thick slab shapes
    private static final VoxelShape SHAPE_FLOOR   = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape SHAPE_CEILING = Block.box(0, 8, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_WALL_N  = Block.box(0, 0, 8, 16, 16, 16);
    private static final VoxelShape SHAPE_WALL_S  = Block.box(0, 0, 0, 16, 16, 8);
    private static final VoxelShape SHAPE_WALL_W  = Block.box(8, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_WALL_E  = Block.box(0, 0, 0, 8, 16, 16);

    public BlockControlPanel(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
        if (face == AttachFace.FLOOR) return SHAPE_FLOOR;
        if (face == AttachFace.CEILING) return SHAPE_CEILING;
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return switch (facing) {
            case NORTH -> SHAPE_WALL_N;
            case SOUTH -> SHAPE_WALL_S;
            case WEST -> SHAPE_WALL_W;
            case EAST -> SHAPE_WALL_E;
            default -> SHAPE_FLOOR;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        AttachFace face;
        Direction facing;

        if (clicked == Direction.UP) {
            face = AttachFace.FLOOR;
            facing = context.getHorizontalDirection().getOpposite();
        } else if (clicked == Direction.DOWN) {
            face = AttachFace.CEILING;
            facing = context.getHorizontalDirection().getOpposite();
        } else {
            face = AttachFace.WALL;
            facing = clicked;
        }

        return defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, face)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }

    public static Direction getFrontFace(BlockState state) {
        AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
        if (face == AttachFace.FLOOR) return Direction.UP;
        if (face == AttachFace.CEILING) return Direction.DOWN;
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public static int[] calculateGrid(BlockState state, BlockPos pos, BlockHitResult hit) {
        return calculateGrid(state, pos, hit.getLocation());
    }

    public static int[] calculateGrid(BlockState state, BlockPos pos, net.minecraft.world.phys.Vec3 clickLoc) {
        AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        double x = clickLoc.x - pos.getX();
        double y = clickLoc.y - pos.getY();
        double z = clickLoc.z - pos.getZ();

        double u = 0.5;
        double v = 0.5;

        if (face == AttachFace.FLOOR) {
            switch (facing) {
                case NORTH -> { u = x; v = z; }
                case SOUTH -> { u = 1.0 - x; v = 1.0 - z; }
                case WEST  -> { u = 1.0 - z; v = x; }
                case EAST  -> { u = z; v = 1.0 - x; }
                default    -> { u = x; v = z; }
            }
        } else if (face == AttachFace.CEILING) {
            switch (facing) {
                case NORTH -> { u = x; v = 1.0 - z; }
                case SOUTH -> { u = 1.0 - x; v = z; }
                case WEST  -> { u = z; v = x; }
                case EAST  -> { u = 1.0 - z; v = 1.0 - x; }
                default    -> { u = x; v = 1.0 - z; }
            }
        } else { // WALL
            v = 1.0 - y;
            switch (facing) {
                case NORTH -> u = x;
                case SOUTH -> u = 1.0 - x;
                case WEST  -> u = 1.0 - z;
                case EAST  -> u = z;
                default    -> u = x;
            }
        }

        u = Math.max(0.001, Math.min(0.999, u));
        v = Math.max(0.001, Math.min(0.999, v));
        int gx = (int) (u * 8.0);
        int gy = (int) (v * 8.0);
        return new int[]{ Math.max(0, Math.min(7, gx)), Math.max(0, Math.min(7, gy)) };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileControlPanel(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileControlPanel panel) {
            return panel.onInteractWithItem(player, hand, hitResult);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileControlPanel panel) {
            return panel.onInteractEmptyHand(player, hitResult);
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
