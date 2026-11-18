package pl.asie.computronics.block;

import pl.asie.computronics.block.entity.TileSpeechBox;

public class BlockSpeechBox extends BlockComputronicsHorizontal {
    public BlockSpeechBox(Properties properties) {
        super(properties, TileSpeechBox::new);
    }
}