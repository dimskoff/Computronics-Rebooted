package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.block.entity.TileInductiveCharger;

public class BlockInductiveCharger extends BlockComputronics {
    public BlockInductiveCharger(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileInductiveCharger(pos, state);
    }
}
