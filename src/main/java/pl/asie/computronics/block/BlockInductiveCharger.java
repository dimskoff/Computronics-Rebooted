package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileInductiveCharger;

public class BlockInductiveCharger extends BlockComputronicsSimple {
    public BlockInductiveCharger(Properties properties) {
        super(properties, TileInductiveCharger::new);
    }
}