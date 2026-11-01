package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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

public class ItemPanelText extends Item {

    private static final int[][] SIZES = {
            {1, 1},
            {2, 1},
            {4, 1},
            {8, 1}, // Full 8-cell width
            {2, 2},
            {4, 2}
    };

    public ItemPanelText(Properties properties) {
        super(properties);
    }

    public static int[] getPlacementDimensions(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains("sizeX") && cd.contains("sizeY")) {
            CompoundTag tag = cd.copyTag();
            int sx = Math.max(1, Math.min(8, tag.getInt("sizeX")));
            int sy = Math.max(1, Math.min(8, tag.getInt("sizeY")));
            return new int[]{sx, sy};
        }
        return new int[]{2, 1}; // Default 2x1 label
    }

    public static void cycleSize(ItemStack stack, Player player) {
        int[] cur = getPlacementDimensions(stack);
        int curIdx = 0;
        for (int i = 0; i < SIZES.length; i++) {
            if (SIZES[i][0] == cur[0] && SIZES[i][1] == cur[1]) {
                curIdx = i;
                break;
            }
        }
        int nextIdx = (curIdx + 1) % SIZES.length;
        int nextW = SIZES[nextIdx][0];
        int nextH = SIZES[nextIdx][1];

        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = custom.copyTag();
        tag.putInt("sizeX", nextW);
        tag.putInt("sizeY", nextH);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        player.displayClientMessage(Component.literal(String.format("§b[Panel Text] §fРазмер текстовой метки: §6%dx%d §7(%d клеток)", nextW, nextH, nextW * nextH)), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                cycleSize(stack, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
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
                    int[] dims = getPlacementDimensions(stack);
                    int w = dims[0];
                    int h = dims[1];
                    if (panel.mountElementWithSize(grid[0], grid[1], TileControlPanel.ElementType.TEXT, 0x1E2228, w, h)) {
                        int slot = panel.getSlotIndex(grid[0], grid[1]);
                        TileControlPanel.PanelElement elem = panel.getElement(slot);
                        if (elem != null) {
                            elem.label = "TEXT";
                            elem.textColor = 0x00FFCC;
                            elem.bold = true;
                            panel.markChangedAndSync();
                        }

                        if (player != null && !player.isCreative()) {
                            stack.shrink(1);
                        }
                        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.3f);
                        if (player != null) {
                            player.displayClientMessage(Component.literal(String.format("§b[Control Panel] §fУстановлена текстовая метка §6%dx%d §fв (%d, %d)", w, h, grid[0], grid[1])), true);
                        }
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    } else if (player != null && !level.isClientSide()) {
                        player.displayClientMessage(Component.literal("§c[Control Panel] Недостаточно места для метки " + w + "x" + h + "!"), true);
                    }
                    return InteractionResult.CONSUME;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int[] dims = getPlacementDimensions(stack);
        tooltip.add(Component.literal("Монтируемая текстовая метка для панели управления.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Размер: §e" + dims[0] + "x" + dims[1] + " §7(Ширина x Высота)").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("ПКМ отверткой для настройки текста, шрифта и цвета").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift + ПКМ: сменить размер (1x1, 2x1, 4x1, 8x1, 2x2, 4x2)").withStyle(ChatFormatting.YELLOW));
    }
}
