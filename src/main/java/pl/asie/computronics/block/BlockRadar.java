package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileRadar;

public class BlockRadar extends BlockComputronicsSimple {
    public BlockRadar(Properties properties) {
        super(properties, TileRadar::new);
    }
}