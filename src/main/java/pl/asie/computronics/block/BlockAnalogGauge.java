package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.block.entity.TileAnalogGauge;

public class BlockAnalogGauge extends BlockComputronics {
    public BlockAnalogGauge(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileAnalogGauge(pos, state);
    }
}
