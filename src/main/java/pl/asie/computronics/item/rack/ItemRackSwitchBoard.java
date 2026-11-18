package pl.asie.computronics.item.rack;

import net.minecraft.ChatFormatting;
import pl.asie.computronics.item.ItemComputronics;

public class ItemRackSwitchBoard extends ItemComputronics {
    public ItemRackSwitchBoard(Properties properties) {
        super(properties,
                line("tooltip.computronics.rack_board_switch.desc1", ChatFormatting.GRAY),
                line("tooltip.computronics.rack_board_switch.desc2", ChatFormatting.AQUA));
    }
}
