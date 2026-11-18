package pl.asie.computronics.item.rack;

import net.minecraft.ChatFormatting;
import pl.asie.computronics.item.ItemComputronics;

public class ItemRackBoomBoard extends ItemComputronics {
    public ItemRackBoomBoard(Properties properties) {
        super(properties,
                line("tooltip.computronics.rack_board_boom.desc1", ChatFormatting.GRAY),
                line("tooltip.computronics.rack_board_boom.desc2", ChatFormatting.RED));
    }
}
