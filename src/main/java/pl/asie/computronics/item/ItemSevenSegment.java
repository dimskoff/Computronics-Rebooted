package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.block.BlockControlPanel;
import pl.asie.computronics.block.entity.TileControlPanel;

import java.util.List;

public class ItemSevenSegment extends Item {

    public ItemSevenSegment(Properties properties) {
        super(properties);
    }

    public static int getPlacementSize(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains("size")) {
            return Math.max(1, Math.min(4, cd.copyTag().getInt("size")));
        }
        return 1;
    }

    public static void setPlacementSize(ItemStack stack, int size) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("size", Math.max(1, Math.min(4, size)));
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int current = getPlacementSize(stack);
            int next = (current == 1) ? 2 : (current == 2 ? 3 : (current == 3 ? 4 : 1));
            setPlacementSize(stack, next);
            if (level.isClientSide()) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 1.4f);
            } else {
                player.displayClientMessage(Component.literal(String.format("§b[7-Segment Display] §fPlacement size: §6%dx%d", next, next)), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (state.getBlock() instanceof BlockControlPanel) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileControlPanel panel) {
                int[] grid = BlockControlPanel.calculateGrid(state, pos, context.getClickLocation());
                if (grid != null) {
                    if (!level.isClientSide()) {
                        int size = getPlacementSize(stack);
                        if (panel.mountElementWithSize(grid[0], grid[1], TileControlPanel.ElementType.SEVEN_SEGMENT, 0x00FFCC, size)) {
                            if (player != null && !player.isCreative()) {
                                stack.shrink(1);
                            }
                            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.4f);
                            if (player != null) {
                                player.displayClientMessage(Component.literal(String.format("§b[Control Panel] §fMounted %dx%d 7-Segment Display at (%d, %d)", size, size, grid[0], grid[1])), true);
                            }
                        } else {
                            if (player != null) {
                                player.displayClientMessage(Component.literal("§c[Control Panel] Cannot fit element here (overlap or out of bounds)!"), true);
                            }
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int size = getPlacementSize(stack);
        tooltip.add(Component.translatable("tooltip.computronics.seven_segment.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.seven_segment.desc2").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("Configured Size: " + size + "x" + size + " (Sneak+R-Click to change)").withStyle(ChatFormatting.GOLD));
    }
}
