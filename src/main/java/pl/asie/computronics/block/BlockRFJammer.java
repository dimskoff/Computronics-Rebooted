package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileRFJammer;

public class BlockRFJammer extends BlockComputronicsHorizontal {
    public BlockRFJammer(Properties properties) {
        super(properties, TileRFJammer::new);
    }
}