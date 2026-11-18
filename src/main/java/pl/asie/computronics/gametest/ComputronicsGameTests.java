package pl.asie.computronics.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.entity.TileConnector;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.block.entity.TileTapeDrive;
import pl.asie.computronics.init.ModBlocks;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.item.ItemTape;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelComponentType;
import pl.asie.computronics.signal.ISignalNode;
import pl.asie.computronics.signal.Signals;
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

        drive.play(null, null);
        h.assertTrue(drive.getState() == TileTapeDrive.State.PLAYING, "drive not playing");

        h.runAfterDelay(40, () -> {
            int pos = (Integer) drive.getPosition(null, null)[0];
            h.assertTrue(pos >= 7000 && pos <= 12500, "unexpected head position after 2s: " + pos);
            drive.stop(null, null);
            int stopped = pos;
            h.runAfterDelay(10, () -> {
                int after = (Integer) drive.getPosition(null, null)[0];
                h.assertTrue(after <= stopped + 1100, "head kept moving after stop");
                h.assertTrue(!drive.removeTape().isEmpty(), "eject returned nothing");
                h.succeed();
            });
        });
    }

    private static TileControlPanel panel(GameTestHelper h, BlockPos rel) {
        BlockState state = ModBlocks.CONTROL_PANEL.get().defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        h.setBlock(rel, state);
        return (TileControlPanel) h.getBlockEntity(rel);
    }

    @GameTest(template = EMPTY)
    public static void controlPanelComponentsWork(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 1, 2);
        TileControlPanel panel = panel(h, rel);
        panel.setGeometry(3, 1);
        h.assertTrue(panel.offset() == 3 && panel.tilt() == 1, "geometry not applied");

        PanelComponent button = panel.mount(PanelComponentType.BUTTON, 1, 1, 2);
        h.assertTrue(button != null && button.width == 2, "button was not mounted");
        h.assertTrue(panel.mount(PanelComponentType.LAMP, 2, 2, 1) == null, "overlap allowed");

        panel.activate(button, "tester");
        h.assertTrue(h.getLevel().getGameTime() < button.pressedUntil, "button did not press");

        PanelComponent sw = panel.mount(PanelComponentType.SWITCH, 5, 5, 1);
        panel.activate(sw, "tester");
        h.assertTrue(sw.state, "switch did not toggle on");

        PanelComponent hold = panel.mount(PanelComponentType.HOLD_BUTTON, 5, 0, 1);
        panel.hold(hold, "tester");
        h.assertTrue(panel.getOutput(hold.id()) == Signals.ON, "hold button not active while held");
        h.runAfterDelay(TileControlPanel.HOLD_GRACE_TICKS + 2, () -> {
            h.assertTrue(panel.getOutput(hold.id()) == 0, "hold button stuck after release");
            h.destroyBlock(rel);
            h.assertItemEntityPresent(ModItems.MINI_BUTTON.get(), rel, 2.0);
            h.assertItemEntityPresent(ModItems.TOGGLE_SWITCH.get(), rel, 2.0);
            h.assertItemEntityPresent(ModItems.HOLD_BUTTON.get(), rel, 2.0);
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wiresCarrySignalsThroughConnector(GameTestHelper h) {
        BlockPos panelRel = new BlockPos(1, 1, 1), conRel = new BlockPos(3, 1, 1);
        TileControlPanel panel = panel(h, panelRel);
        h.setBlock(conRel, ModBlocks.CONNECTOR.get());
        TileConnector con = (TileConnector) h.getBlockEntity(conRel);
        BlockPos pAbs = h.absolutePos(panelRel), cAbs = h.absolutePos(conRel);

        PanelComponent sw = panel.mount(PanelComponentType.SWITCH, 0, 0, 1);
        PanelComponent lamp = panel.mount(PanelComponentType.LAMP, 4, 4, 1);

        // switch output -> connector input 3 (given in reverse order on purpose)
        Signals.Result r1 = Signals.connect(h.getLevel(), cAbs, new ISignalNode.PortRef(3, false), pAbs, new ISignalNode.PortRef(sw.id(), true));
        h.assertTrue(r1 == Signals.Result.OK, "connect switch->connector: " + r1);
        // connector output 5 -> lamp input
        Signals.Result r2 = Signals.connect(h.getLevel(), cAbs, new ISignalNode.PortRef(5, true), pAbs, new ISignalNode.PortRef(lamp.id(), false));
        h.assertTrue(r2 == Signals.Result.OK, "connect connector->lamp: " + r2);
        // input to input must fail
        h.assertTrue(Signals.connect(h.getLevel(), cAbs, new ISignalNode.PortRef(1, false), pAbs,
                new ISignalNode.PortRef(lamp.id(), false)) == Signals.Result.SAME_DIRECTION, "input-input allowed");

        panel.activate(sw, "tester");
        con.setOutput(null, new FakeArgs(5, 15));
        h.runAfterDelay(3, () -> {
            h.assertTrue(con.input(3) == Signals.ON, "connector input 3 = " + con.input(3));
            h.assertTrue(lamp.input == 15 && lamp.lit(), "lamp not lit by connector output, input=" + lamp.input);

            // unplug the lamp wire from the lamp side: lamp goes dark, link removed at the source
            h.assertTrue(Signals.disconnectInput(h.getLevel(), pAbs, lamp.id()), "nothing to unplug");
            h.assertTrue(lamp.input == 0 && !lamp.lit(), "lamp still lit after unplug");
            h.assertTrue(con.links().isEmpty(), "link left on connector");

            // breaking the panel unplugs the remaining wire and drops it
            h.destroyBlock(panelRel);
            h.assertTrue(con.input(3) == 0 && !con.isWired(3), "connector still wired after panel broke");
            h.assertItemEntityPresent(ModItems.SIGNAL_WIRE.get(), panelRel, 2.0);
            h.succeed();
        });
    }

    /** Minimal OpenComputers Arguments for calling a callback from a test. */
    private record FakeArgs(Object... values) implements li.cil.oc.api.machine.Arguments {
        public int count() { return values.length; }
        public Object checkAny(int i) { return values[i]; }
        public boolean checkBoolean(int i) { return (Boolean) values[i]; }
        public int checkInteger(int i) { return ((Number) values[i]).intValue(); }
        public long checkLong(int i) { return ((Number) values[i]).longValue(); }
        public double checkDouble(int i) { return ((Number) values[i]).doubleValue(); }
        public String checkString(int i) { return (String) values[i]; }
        public byte[] checkByteArray(int i) { return (byte[]) values[i]; }
        public java.util.Map checkTable(int i) { return (java.util.Map) values[i]; }
        public ItemStack checkItemStack(int i) { return (ItemStack) values[i]; }
        public Object optAny(int i, Object d) { return i < values.length ? values[i] : d; }
        public boolean optBoolean(int i, boolean d) { return i < values.length ? checkBoolean(i) : d; }
        public int optInteger(int i, int d) { return i < values.length ? checkInteger(i) : d; }
        public long optLong(int i, long d) { return i < values.length ? checkLong(i) : d; }
        public double optDouble(int i, double d) { return i < values.length ? checkDouble(i) : d; }
        public String optString(int i, String d) { return i < values.length ? checkString(i) : d; }
        public byte[] optByteArray(int i, byte[] d) { return i < values.length ? checkByteArray(i) : d; }
        public java.util.Map optTable(int i, java.util.Map d) { return i < values.length ? checkTable(i) : d; }
        public ItemStack optItemStack(int i, ItemStack d) { return i < values.length ? checkItemStack(i) : d; }
        public boolean isBoolean(int i) { return i < values.length && values[i] instanceof Boolean; }
        public boolean isInteger(int i) { return i < values.length && values[i] instanceof Integer; }
        public boolean isLong(int i) { return i < values.length && values[i] instanceof Long; }
        public boolean isDouble(int i) { return i < values.length && values[i] instanceof Double; }
        public boolean isString(int i) { return i < values.length && values[i] instanceof String; }
        public boolean isByteArray(int i) { return i < values.length && values[i] instanceof byte[]; }
        public boolean isTable(int i) { return i < values.length && values[i] instanceof java.util.Map; }
        public boolean isItemStack(int i) { return i < values.length && values[i] instanceof ItemStack; }
        public Object[] toArray() { return values; }
        public java.util.Iterator<Object> iterator() { return java.util.Arrays.asList(values).iterator(); }
    }
}
