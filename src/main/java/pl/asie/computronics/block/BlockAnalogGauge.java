package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileAnalogGauge;

public class BlockAnalogGauge extends BlockComputronicsHorizontal {
    public BlockAnalogGauge(Properties properties) {
        super(properties, TileAnalogGauge::new);
    }
}