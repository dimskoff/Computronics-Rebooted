package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.block.entity.TileConnector;
import pl.asie.computronics.init.ModItems;

/**
 * Signal connector block: 8 wire inputs on top, 8 wire outputs below, OpenComputers cable on the sides.
 */
public class BlockConnector extends BlockComputronics {
    public BlockConnector(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileConnector(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TileConnector connector) {
            int wires = connector.unplugAll();
            if (wires > 0) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new ItemStack(ModItems.SIGNAL_WIRE.get(), wires));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
