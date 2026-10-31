package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.block.entity.TileCamera;

public class BlockCamera extends BlockComputronics {
    public BlockCamera(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileCamera(pos, state);
    }
}
