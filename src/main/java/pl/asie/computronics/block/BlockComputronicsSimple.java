package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine without any orientation.
 */
public class BlockComputronicsSimple extends BlockComputronics {
    private final BlockEntityType.BlockEntitySupplier<? extends BlockEntity> blockEntityFactory;

    public BlockComputronicsSimple(Properties properties, BlockEntityType.BlockEntitySupplier<? extends BlockEntity> blockEntityFactory) {
        super(properties);
        this.blockEntityFactory = blockEntityFactory;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityFactory.create(pos, state);
    }
}