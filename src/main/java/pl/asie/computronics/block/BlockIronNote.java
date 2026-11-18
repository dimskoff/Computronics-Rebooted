package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileIronNote;

public class BlockIronNote extends BlockComputronicsHorizontal {
    public BlockIronNote(Properties properties) {
        super(properties, TileIronNote::new);
    }
}