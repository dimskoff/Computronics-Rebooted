package pl.asie.computronics.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.block.entity.TileTapeDrive;
import pl.asie.computronics.init.ModBlocks;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.item.ItemPanelComponent;
import pl.asie.computronics.item.ItemTape;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelComponentType;
import pl.asie.computronics.panel.PanelOrientation;
import pl.asie.computronics.tape.TapeStorage;

/**
 * In-world tests. Run with {@code gradlew runGameTestServer}.
 */
@GameTestHolder(Computronics.MODID)
@PrefixGameTestTemplate(false)
public class ComputronicsGameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void tapeDrivePlaysAndAdvances(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 1, 2);
        h.setBlock(rel, ModBlocks.TAPE_DRIVE.get());
        TileTapeDrive drive = (TileTapeDrive) h.getBlockEntity(rel);

        ItemStack tape = new ItemStack(ModItems.TAPE_IRON.get());
        String id = ItemTape.getOrCreateStorageId(tape);
        TapeStorage storage = TapeStorage.get(id, ItemTape.CAPACITY_IRON);
        h.assertTrue(storage != null, "tape storage unavailable (world dir not set?)");
        storage.write(0, new byte[]{0x55, 0x55, 0x55, 0x55});

        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, tape);
        drive.onInteract(player, InteractionHand.MAIN_HAND);
        h.assertTrue(!drive.getTape().isEmpty(), "tape was not inserted");
        h.assertTrue(player.getMainHandItem().isEmpty(), "tape not taken from hand");

        drive.play(null, null);
        h.assertTrue(drive.getState() == TileTapeDrive.State.PLAYING, "drive not playing");

        h.runAfterDelay(40, () -> {
            int pos = (Integer) drive.getPosition(null, null)[0];
            // 2 s at 4096 B/s (+ 0.5 s prebuffer) = ~10 KB, allow scheduling slack
            h.assertTrue(pos >= 7000 && pos <= 12500, "unexpected head position after 2s: " + pos);
            drive.stop(null, null);
            h.assertTrue(drive.getState() == TileTapeDrive.State.STOPPED, "drive did not stop");
            int stopped = pos;
            h.runAfterDelay(10, () -> {
                int after = (Integer) drive.getPosition(null, null)[0];
                h.assertTrue(after <= stopped + 1100, "head kept moving after stop");
                ItemStack out = drive.removeTape();
                h.assertTrue(!out.isEmpty(), "eject returned nothing");
                h.succeed();
            });
        });
    }

    @GameTest(template = EMPTY)
    public static void controlPanelComponentsWork(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockState state = ModBlocks.CONTROL_PANEL.get().defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        h.setBlock(rel, state);
        TileControlPanel panel = (TileControlPanel) h.getBlockEntity(rel);
        BlockPos abs = h.absolutePos(rel);

        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack buttons = new ItemStack(ModItems.MINI_BUTTON.get(), 2);
        ItemPanelComponent.setPlacementSize(buttons, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, buttons);

        // click the centre of cell (1, 1) on the front face
        Vec3 hit = cellCenter(state, abs, 1, 1);
        BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, abs, false);
        state.useItemOn(buttons, h.getLevel(), player, InteractionHand.MAIN_HAND, bhr);
        PanelComponent button = panel.layout().at(1, 1);
        h.assertTrue(button != null && button.type == PanelComponentType.BUTTON, "button was not mounted");
        h.assertTrue(button.width == 2 && button.height == 2, "wrong button size");
        h.assertTrue(buttons.getCount() == 1, "item not consumed");

        // overlapping placement is refused and keeps the item
        state.useItemOn(buttons, h.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(cellCenter(state, abs, 2, 2), Direction.UP, abs, false));
        h.assertTrue(buttons.getCount() == 1, "overlapping placement consumed an item");

        // empty hand press
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        state.useWithoutItem(h.getLevel(), player, new BlockHitResult(cellCenter(state, abs, 2, 2), Direction.UP, abs, false));
        h.assertTrue(h.getLevel().getGameTime() < button.pressedUntil, "button did not press");

        // a switch toggles
        PanelComponent sw = panel.mount(PanelComponentType.SWITCH, 5, 5, 1);
        h.assertTrue(sw != null, "switch did not mount");
        state.useWithoutItem(h.getLevel(), player, new BlockHitResult(cellCenter(state, abs, 5, 5), Direction.UP, abs, false));
        h.assertTrue(sw.state, "switch did not toggle on");

        // clicks on the side of the plate are ignored
        h.assertTrue(pl.asie.computronics.block.BlockControlPanel.cellAt(state, abs, hit, Direction.NORTH) == null,
                "side click hit a cell");

        // breaking the panel drops the mounted components
        h.destroyBlock(rel);
        h.assertItemEntityPresent(ModItems.MINI_BUTTON.get(), rel, 2.0);
        h.assertItemEntityPresent(ModItems.TOGGLE_SWITCH.get(), rel, 2.0);
        h.succeed();
    }

    private static Vec3 cellCenter(BlockState state, BlockPos abs, int cx, int cy) {
        PanelOrientation o = PanelOrientation.of(state);
        Vec3 local = o.origin
                .add(o.u.scale(PanelOrientation.MARGIN + (cx + 0.5) * PanelOrientation.CELL))
                .add(o.v.scale(PanelOrientation.MARGIN + (cy + 0.5) * PanelOrientation.CELL));
        return local.add(abs.getX(), abs.getY(), abs.getZ());
    }
}
