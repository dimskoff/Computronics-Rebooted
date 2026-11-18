package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileCamera;

public class BlockCamera extends BlockComputronicsDirectional {
    public BlockCamera(Properties properties) {
        super(properties, TileCamera::new);
    }
}