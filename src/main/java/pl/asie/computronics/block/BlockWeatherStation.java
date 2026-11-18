package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileWeatherStation;

public class BlockWeatherStation extends BlockComputronicsHorizontal {
    public BlockWeatherStation(Properties properties) {
        super(properties, TileWeatherStation::new);
    }
}