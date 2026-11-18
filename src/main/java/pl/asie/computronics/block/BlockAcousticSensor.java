package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileAcousticSensor;

public class BlockAcousticSensor extends BlockComputronicsSimple {
    public BlockAcousticSensor(Properties properties) {
        super(properties, TileAcousticSensor::new);
    }
}