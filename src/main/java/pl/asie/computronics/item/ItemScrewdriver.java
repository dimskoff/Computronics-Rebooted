package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.block.entity.TileControlPanel;

import java.util.List;

public class ItemScrewdriver extends Item {
    public static final int[] PRESET_COLORS = {
            0xCC2020, // Tactile Red
            0x20CC40, // Emerald Green
            0x2080FF, // Electric Blue
            0xFFCC00, // Amber Yellow
            0x00FFCC, // Cyan
            0xFF20AA, // Magenta
            0xFF8000, // Industrial Orange
            0xEEEEEE, // Bright White
            0x323842  // Dark Gunmetal
    };

    public ItemScrewdriver(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof pl.asie.computronics.block.BlockControlPanel) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileControlPanel panel) {
                Player player = context.getPlayer();
                if (player == null) return InteractionResult.PASS;

                int[] grid = pl.asie.computronics.block.BlockControlPanel.calculateGrid(state, pos, context.getClickLocation());
                if (grid == null) return InteractionResult.PASS;

                int gx = grid[0];
                int gy = grid[1];
                TileControlPanel.PanelElement elem = panel.findRootElement(gx, gy);
                if (elem == null || elem.type == TileControlPanel.ElementType.NONE) {
                    if (!level.isClientSide) {
                        player.displayClientMessage(Component.literal("§7[Screwdriver] No component at clicked position.").withStyle(ChatFormatting.GRAY), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }

                if (player.isShiftKeyDown()) {
                    if (!level.isClientSide) {
                        // Shift+Click: Cycle size 1 -> 2 -> 3 -> 4 -> 1
                        int newSize = (elem.size % 4) + 1;
                        panel.resizeElement(elem, newSize);
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f + (newSize * 0.1f));
                        player.displayClientMessage(Component.literal(String.format("§e[Screwdriver] §fResized component to §b%dx%d", newSize, newSize)), true);
                    }
                } else {
                    // Regular Right-Click opens the interactive GUI!
                    if (level.isClientSide) {
                        pl.asie.computronics.client.gui.ControlPanelConfigScreen.open(panel, elem.slot);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.computronics.screwdriver.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.screwdriver.desc2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.screwdriver.desc3").withStyle(ChatFormatting.DARK_AQUA));
    }
}
