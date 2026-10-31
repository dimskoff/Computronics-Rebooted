package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.block.BlockControlPanel;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.init.ModBlocks;

import java.util.List;

public class ItemMiniButton extends Item {
    public ItemMiniButton(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        BlockState state = level.getBlockState(pos);

        // Case 1: Clicked directly on an existing Control Panel face
        if (state.getBlock() instanceof BlockControlPanel) {
            Direction panelFacing = state.hasProperty(BlockStateProperties.FACING)
                    ? state.getValue(BlockStateProperties.FACING)
                    : (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH);
            if (face == panelFacing) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TileControlPanel panel) {
                    Vec3 hit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos));
                    int[] grid = ItemMiniLamp.calculateGrid(hit, face);
                    if (grid != null) {
                        if (!level.isClientSide()) {
                            if (panel.mountElementAt(grid[0], grid[1], TileControlPanel.ElementType.BUTTON, 0xFF0000)) {
                                if (player != null && !player.isCreative()) {
                                    stack.shrink(1);
                                }
                                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.2f);
                                if (player != null) {
                                    player.displayClientMessage(Component.literal(String.format("§a[Control Panel] §fMounted Mini Button at (%d, %d)", grid[0], grid[1])), true);
                                }
                            } else {
                                if (player != null) {
                                    player.displayClientMessage(Component.literal("§c[Control Panel] Slot already occupied!"), true);
                                }
                            }
                        }
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }
                }
            }
        }

        // Case 2: Clicked on a solid wall/surface -> attach/create a modular panel with the button mounted!
        BlockPos placePos = pos.relative(face);
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        if (level.getBlockState(placePos).canBeReplaced(placeContext)) {
            if (!level.isClientSide()) {
                BlockState newState = ModBlocks.CONTROL_PANEL.get().defaultBlockState();
                if (newState.hasProperty(BlockStateProperties.FACING)) {
                    newState = newState.setValue(BlockStateProperties.FACING, face);
                } else if (newState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && face.getAxis().isHorizontal()) {
                    newState = newState.setValue(BlockStateProperties.HORIZONTAL_FACING, face);
                }
                level.setBlock(placePos, newState, 3);

                BlockEntity be = level.getBlockEntity(placePos);
                if (be instanceof TileControlPanel panel) {
                    Vec3 hit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos));
                    int[] grid = ItemMiniLamp.calculateGrid(hit, face);
                    int gx = grid != null ? grid[0] : 3;
                    int gy = grid != null ? grid[1] : 3;

                    panel.mountElementAt(gx, gy, TileControlPanel.ElementType.BUTTON, 0xFF0000);
                    if (player != null && !player.isCreative()) {
                        stack.shrink(1);
                    }
                    level.playSound(null, placePos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (player != null) {
                        player.displayClientMessage(Component.literal(String.format("§a[Control Panel] §fAttached Panel & Mounted Button at (%d, %d)", gx, gy)), true);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.computronics.mini_button.desc").withStyle(ChatFormatting.GRAY));
    }
}
