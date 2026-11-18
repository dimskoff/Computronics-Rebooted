package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A machine whose front faces the player horizontally ({@code horizontal_facing}).
 */
public class BlockComputronicsHorizontal extends BlockComputronics {
    private final BlockEntityType.BlockEntitySupplier<? extends BlockEntity> blockEntityFactory;

    public BlockComputronicsHorizontal(Properties properties, BlockEntityType.BlockEntitySupplier<? extends BlockEntity> blockEntityFactory) {
        super(properties);
        this.blockEntityFactory = blockEntityFactory;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityFactory.create(pos, state);
    }
}