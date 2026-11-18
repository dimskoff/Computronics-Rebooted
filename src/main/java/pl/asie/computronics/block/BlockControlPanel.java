package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.item.ItemPanelComponent;
import pl.asie.computronics.item.ItemScrewdriver;
import pl.asie.computronics.item.ItemSignalWire;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelComponentType;
import pl.asie.computronics.panel.PanelOrientation;
import pl.asie.computronics.util.SidedHooks;

/**
 * A 3 px plate on a floor, ceiling or wall. Like a slab it can sit at different heights inside the
 * block, and it can be tilted (screwdriver on an empty spot of the panel). The plate itself is drawn
 * by the block entity renderer so it can follow those settings.
 * <ul>
 *   <li>right-click with a component: mount it</li>
 *   <li>right-click: press button / flip switch (hold buttons: keep holding)</li>
 *   <li>screwdriver right-click: component settings, or panel settings on an empty spot</li>
 *   <li>screwdriver sneak + right-click: resize component</li>
 *   <li>screwdriver left-click: remove component</li>
 *   <li>signal wire on the back: connect a button/switch output or lamp/display input</li>
 * </ul>
 */
public class BlockControlPanel extends BlockComputronics {

    public BlockControlPanel(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int offset = 0, tilt = 0;
        if (level.getBlockEntity(pos) instanceof TileControlPanel panel) {
            offset = panel.offset();
            tilt = panel.tilt();
        } else if (state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.FLOOR) {
            offset = defaultOffset(AttachFace.FLOOR);
        }
        return PanelOrientation.shape(state.getValue(BlockStateProperties.ATTACH_FACE),
                state.getValue(BlockStateProperties.HORIZONTAL_FACING), offset, tilt);
    }

    /** Desk height on the floor, flush on walls and ceilings. */
    public static int defaultOffset(AttachFace face) {
        return face == AttachFace.FLOOR ? PanelOrientation.OFFSETS - 1 : 0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        BlockState state = defaultBlockState();
        if (clicked == Direction.UP) {
            state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
        } else if (clicked == Direction.DOWN) {
            state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
        } else {
            state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, clicked);
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TileControlPanel panel) {
            int offset = defaultOffset(state.getValue(BlockStateProperties.ATTACH_FACE));
            // sneak-placing puts the plate on the other side, like placing a slab upside down
            if (placer != null && placer.isShiftKeyDown()) offset = PanelOrientation.OFFSETS - 1 - offset;
            panel.setGeometry(offset, 0);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileControlPanel(pos, state);
    }

    // ------------------------------------------------------------------ hit testing

    /** Grid cell on the front face under the player's crosshair, or null. */
    @Nullable
    public static int[] cellLookedAt(TileControlPanel panel, Player player) {
        BlockPos p = panel.getBlockPos();
        Vec3 eye = player.getEyePosition().subtract(p.getX(), p.getY(), p.getZ());
        double[] uv = panel.orientation().raycast(eye, player.getViewVector(1f), false);
        return uv == null ? null : PanelOrientation.cellOf(uv[0], uv[1]);
    }

    @Nullable
    public static PanelComponent componentLookedAt(TileControlPanel panel, Player player) {
        int[] cell = cellLookedAt(panel, player);
        return cell == null ? null : panel.layout().at(cell[0], cell[1]);
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileControlPanel panel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof ItemSignalWire) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION; // handled by the wire's useOn
        }
        int[] cell = cellLookedAt(panel, player);

        if (stack.getItem() instanceof ItemPanelComponent item) {
            if (cell == null) return ItemInteractionResult.FAIL;
            if (!level.isClientSide) {
                PanelComponent c = panel.mount(item.type(), cell[0], cell[1], ItemPanelComponent.getPlacementSize(stack));
                if (c == null) {
                    player.displayClientMessage(Component.translatable("message.computronics.panel.no_room"), true);
                    return ItemInteractionResult.FAIL;
                }
                stack.consume(1, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.getItem() instanceof ItemScrewdriver) {
            PanelComponent c = cell == null ? null : panel.layout().at(cell[0], cell[1]);
            if (c == null) {
                if (cell != null && level.isClientSide) SidedHooks.openPanelGeometry(pos);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide) cycleSize(panel, c, player);
            } else if (level.isClientSide) {
                SidedHooks.openPanelConfig(pos, c.id());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof TileControlPanel panel)) {
            return InteractionResult.PASS;
        }
        PanelComponent c = componentLookedAt(panel, player);
        if (c == null) return InteractionResult.PASS;
        boolean interactive = c.type.isButton() || c.type == PanelComponentType.SWITCH;
        if (level.isClientSide) return interactive ? InteractionResult.SUCCESS : InteractionResult.PASS;
        return panel.activate(c, player.getGameProfile().getName()) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    private static void cycleSize(TileControlPanel panel, PanelComponent c, Player player) {
        int current = c.type.square ? c.width : (c.type == PanelComponentType.SEGMENT ? c.digits() : c.height);
        for (int step = 1; step <= 4; step++) {
            int size = (current - 1 + step) % 4 + 1;
            int[] fp = c.type.footprint(size);
            if (panel.layout().resize(c, fp[0], fp[1])) {
                panel.markDirtyAndSync();
                player.displayClientMessage(Component.translatable("message.computronics.panel.resized", fp[0], fp[1]), true);
                return;
            }
        }
    }

    /** Screwdriver left-click: take one component off. Called once per click on the server. */
    public static boolean dismantle(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileControlPanel panel)) return false;
        PanelComponent c = componentLookedAt(panel, player);
        if (c == null) return false;
        for (ItemStack drop : panel.dismount(c)) {
            if (!player.getAbilities().instabuild && !player.getInventory().add(drop)) player.drop(drop, false);
        }
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TileControlPanel panel) {
            int incoming = panel.unplugIncoming();
            for (ItemStack drop : panel.dismountAll()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            }
            if (incoming > 0) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new ItemStack(pl.asie.computronics.init.ModItems.SIGNAL_WIRE.get(), incoming));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
