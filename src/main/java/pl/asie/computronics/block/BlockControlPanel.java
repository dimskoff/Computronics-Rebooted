package pl.asie.computronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
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
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.item.ItemPanelComponent;
import pl.asie.computronics.item.ItemScrewdriver;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelComponentType;
import pl.asie.computronics.panel.PanelOrientation;
import pl.asie.computronics.util.SidedHooks;

/**
 * A thin plate (4 px) mounted flush on a floor, ceiling or wall. Components are placed with
 * right-click, used with right-click, configured with the screwdriver (right-click: settings,
 * sneak + right-click: resize) and removed with screwdriver left-click.
 */
public class BlockControlPanel extends BlockComputronics {
    private static final int T = 4;
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, T, 16);
    private static final VoxelShape CEILING = Block.box(0, 16 - T, 0, 16, 16, 16);
    private static final VoxelShape WALL_NORTH = Block.box(0, 0, 16 - T, 16, 16, 16);
    private static final VoxelShape WALL_SOUTH = Block.box(0, 0, 0, 16, 16, T);
    private static final VoxelShape WALL_WEST = Block.box(16 - T, 0, 0, 16, 16, 16);
    private static final VoxelShape WALL_EAST = Block.box(0, 0, 0, T, 16, 16);

    public BlockControlPanel(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.ATTACH_FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                case NORTH -> WALL_NORTH;
                case SOUTH -> WALL_SOUTH;
                case WEST -> WALL_WEST;
                default -> WALL_EAST;
            };
        };
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

    /** Grid cell under the hit, or null if the hit isn't on the front face's usable area. */
    public static int[] cellAt(BlockState state, BlockPos pos, Vec3 hitLocation, Direction hitFace) {
        PanelOrientation o = PanelOrientation.of(state);
        if (hitFace != null && hitFace != Direction.getNearest(o.w.x, o.w.y, o.w.z)) return null;
        return o.toCell(hitLocation.subtract(pos.getX(), pos.getY(), pos.getZ()));
    }

    public static PanelComponent componentAt(TileControlPanel panel, BlockHitResult hit) {
        int[] cell = cellAt(panel.getBlockState(), panel.getBlockPos(), hit.getLocation(), hit.getDirection());
        return cell == null ? null : panel.layout().at(cell[0], cell[1]);
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileControlPanel panel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int[] cell = cellAt(state, pos, hit.getLocation(), hit.getDirection());

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
            if (c == null) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
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
        PanelComponent c = componentAt(panel, hit);
        if (c == null) return InteractionResult.PASS;
        if (level.isClientSide) {
            // predict: buttons/switches respond, lamps & displays don't consume the click
            return c.type == PanelComponentType.BUTTON || c.type == PanelComponentType.SWITCH
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return panel.activate(c, player.getGameProfile().getName()) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    private static void cycleSize(TileControlPanel panel, PanelComponent c, Player player) {
        // try the next size up, wrapping to 1 when it doesn't fit or exceeds 4
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

    /** Screwdriver left-click: take one component off. Called once per click from the server. */
    public static boolean dismantle(Level level, BlockPos pos, Player player, Vec3 hitLocation, Direction face) {
        if (!(level.getBlockEntity(pos) instanceof TileControlPanel panel)) return false;
        int[] cell = cellAt(panel.getBlockState(), pos, hitLocation, face);
        PanelComponent c = cell == null ? null : panel.layout().at(cell[0], cell[1]);
        if (c == null) return false;
        ItemStack drop = panel.dismount(c);
        if (!player.getAbilities().instabuild && !player.getInventory().add(drop)) {
            player.drop(drop, false);
        }
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TileControlPanel panel) {
            for (ItemStack drop : panel.dismountAll()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }
}
