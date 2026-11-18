package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileEntityDetector;

public class BlockEntityDetector extends BlockComputronicsHorizontal {
    public BlockEntityDetector(Properties properties) {
        super(properties, TileEntityDetector::new);
    }
}