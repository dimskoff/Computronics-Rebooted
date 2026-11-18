package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileChatBox;

public class BlockChatBox extends BlockComputronicsSimple {
    public BlockChatBox(Properties properties) {
        super(properties, TileChatBox::new);
    }
}