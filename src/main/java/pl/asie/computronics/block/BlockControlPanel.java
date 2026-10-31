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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import pl.asie.computronics.block.entity.TileControlPanel;

public class BlockControlPanel extends BlockComputronics {
    private static final VoxelShape SHAPE_BOTTOM = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape SHAPE_TOP    = Block.box(0, 8, 0, 16, 16, 16);

    public BlockControlPanel(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Half half = state.hasProperty(BlockStateProperties.HALF) ? state.getValue(BlockStateProperties.HALF) : Half.BOTTOM;
        return half == Half.TOP ? SHAPE_TOP : SHAPE_BOTTOM;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Half half;
        if (clicked == Direction.DOWN || (clicked != Direction.UP && (context.getClickLocation().y - context.getClickedPos().getY()) > 0.5)) {
            half = Half.TOP;
        } else {
            half = Half.BOTTOM;
        }
        Direction facing = context.getHorizontalDirection().getOpposite();
        return defaultBlockState()
                .setValue(BlockStateProperties.HALF, half)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
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
