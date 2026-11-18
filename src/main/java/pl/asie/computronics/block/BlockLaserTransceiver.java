package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileLaserTransceiver;

public class BlockLaserTransceiver extends BlockComputronicsDirectional {
    public BlockLaserTransceiver(Properties properties) {
        super(properties, TileLaserTransceiver::new);
    }
}