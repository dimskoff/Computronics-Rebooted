package pl.asie.computronics.item.rack;

import net.minecraft.ChatFormatting;
import pl.asie.computronics.item.ItemComputronics;

public class ItemRackCoProcessor extends ItemComputronics {
    public ItemRackCoProcessor(Properties properties) {
        super(properties,
                line("tooltip.computronics.rack_board_co_processor.desc1", ChatFormatting.GRAY),
                line("tooltip.computronics.rack_board_co_processor.desc2", ChatFormatting.DARK_GRAY));
    }
}
