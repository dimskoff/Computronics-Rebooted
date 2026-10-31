package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.block.entity.TileChatBox;

public class BlockChatBox extends BlockComputronics {
    public BlockChatBox(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileChatBox(pos, state);
    }
}
