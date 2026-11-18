package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.init.ModSounds;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelComponentType;
import pl.asie.computronics.panel.PanelLayout;
import pl.asie.computronics.panel.PanelOrientation;
import pl.asie.computronics.signal.ISignalNode;
import pl.asie.computronics.signal.SignalLink;
import pl.asie.computronics.signal.SignalSource;
import pl.asie.computronics.signal.Signals;

import java.util.*;

/**
 * Control panel block entity.
 * <ul>
 *   <li>Owns a {@link PanelLayout} and the plate geometry (height offset + tilt).</li>
 *   <li>OpenComputers component {@code control_panel}; fires {@code panel_button}, {@code panel_hold}, {@code panel_switch}.</li>
 *   <li>Signal ports on the back of the plate: buttons/switches are outputs, lamps/displays are inputs.
 *       Port number = component id.</li>
 * </ul>
 * Changes only mark the panel dirty; the server tick pushes at most one update per tick to clients.
 */
public class TileControlPanel extends TileEntityComputronics implements ISignalNode {
    public static final int PRESS_TICKS = 6;
    public static final int HOLD_GRACE_TICKS = 5;

    private final PanelLayout layout = new PanelLayout();
    private final List<SignalLink> links = new ArrayList<>();
    private final Map<Integer, SignalSource> sources = new HashMap<>();
    /** last value sent on each output port, to only propagate changes */
    private final Map<Integer, Integer> sentOutputs = new HashMap<>();
    private final Map<Integer, String> holders = new HashMap<>();
    private int offset;
    private int tilt;
    private volatile boolean syncPending;
    private PanelOrientation orientationCache;

    public TileControlPanel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROL_PANEL.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("control_panel", Visibility.Network)
                .create();
    }

    public PanelLayout layout() {
        return layout;
    }

    public int offset() {
        return offset;
    }

    public int tilt() {
        return tilt;
    }

    public PanelOrientation orientation() {
        PanelOrientation o = orientationCache;
        if (o == null) orientationCache = o = PanelOrientation.of(getBlockState(), offset, tilt);
        return o;
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        orientationCache = null;
    }

    public void setGeometry(int offset, int tilt) {
        this.offset = Math.max(0, Math.min(PanelOrientation.OFFSETS - 1, offset));
        this.tilt = Math.max(0, Math.min(PanelOrientation.TILTS.length - 1, tilt));
        orientationCache = null;
        markDirtyAndSync();
    }

    public boolean isWired(int inPort) {
        return sources.containsKey(inPort);
    }

    // ------------------------------------------------------------------ server-side operations

    public PanelComponent mount(PanelComponentType type, int cx, int cy, int size) {
        int[] fp = type.footprint(size);
        PanelComponent c = layout.place(type, cx, cy, fp[0], fp[1]);
        if (c != null) {
            playSound(SoundEvents.STONE_PLACE, 1.4f);
            markDirtyAndSync();
        }
        return c;
    }

    /** Removes a component, unplugs its wire and returns the items to give back. */
    public List<ItemStack> dismount(PanelComponent c) {
        List<ItemStack> drops = new ArrayList<>();
        if (!layout.remove(c)) return drops;
        drops.add(new ItemStack(c.type.item()));
        int wires = unplug(c);
        if (wires > 0) drops.add(new ItemStack(ModItems.SIGNAL_WIRE.get(), wires));
        playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.1f);
        markDirtyAndSync();
        return drops;
    }

    private int unplug(PanelComponent c) {
        if (level == null || level.isClientSide) return 0;
        int n = 0;
        if (c.type.hasOutputPort()) n += Signals.disconnectOutput(level, getBlockPos(), c.id());
        if (c.type.hasInputPort() && Signals.disconnectInput(level, getBlockPos(), c.id())) n++;
        sentOutputs.remove(c.id());
        return n;
    }

    /** Everything that should drop when the panel is broken: components + unplugged wires. */
    public List<ItemStack> dismountAll() {
        List<ItemStack> drops = new ArrayList<>();
        int wires = 0;
        for (PanelComponent c : layout.snapshot()) {
            wires += unplug(c);
        }
        for (PanelComponent c : layout.clear()) drops.add(new ItemStack(c.type.item()));
        if (wires > 0) drops.add(new ItemStack(ModItems.SIGNAL_WIRE.get(), wires));
        markDirtyAndSync();
        return drops;
    }

    /** A player (or Lua via press()) clicks a component. */
    public boolean activate(PanelComponent c, String who) {
        switch (c.type) {
            case BUTTON -> {
                c.pressedUntil = gameTime() + PRESS_TICKS;
                playSound(ModSounds.BUTTON_PRESS.get(), 1.0f);
                signal("panel_button", c.id(), who, c.x + 1, c.y + 1);
            }
            case HOLD_BUTTON -> hold(c, who);
            case SWITCH -> {
                c.state = !c.state;
                playSound(SoundEvents.LEVER_CLICK, c.state ? 0.75f : 0.6f);
                signal("panel_switch", c.id(), c.state, who, c.x + 1, c.y + 1);
            }
            default -> {
                return false;
            }
        }
        markDirtyAndSync();
        return true;
    }

    /** Hold button: keeps it pressed while the client keeps sending hold packets. */
    public void hold(PanelComponent c, String who) {
        if (c.type != PanelComponentType.HOLD_BUTTON) return;
        boolean wasDown = gameTime() < c.pressedUntil;
        c.pressedUntil = gameTime() + HOLD_GRACE_TICKS;
        if (!wasDown) {
            holders.put(c.id(), who);
            playSound(ModSounds.BUTTON_PRESS.get(), 0.9f);
            signal("panel_hold", c.id(), true, who, c.x + 1, c.y + 1);
            markDirtyAndSync();
        }
    }

    public void configure(PanelComponent c, int color, String label, int width, int height) {
        c.color = color & 0xFFFFFF;
        if (c.type.hasLabel()) c.label = label;
        if (width != c.width || height != c.height) layout.resize(c, width, height);
        markDirtyAndSync();
    }

    private void signal(String name, Object... args) {
        if (node != null && node.network() != null) {
            Object[] full = new Object[args.length + 1];
            full[0] = name;
            System.arraycopy(args, 0, full, 1, args.length);
            node.sendToReachable("computer.signal", full);
        }
    }

    private void playSound(SoundEvent sound, float pitch) {
        if (level != null && !level.isClientSide) {
            level.playSound(null, getBlockPos(), sound, SoundSource.BLOCKS, 0.6f, pitch);
        }
    }

    private long gameTime() {
        return level != null ? level.getGameTime() : 0;
    }

    public void markDirtyAndSync() {
        syncPending = true;
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        long now = level.getGameTime();

        // button releases + output propagation (only on change, full refresh every second)
        boolean refresh = now % 20 == (pos.hashCode() & 15);
        for (PanelComponent c : layout.snapshot()) {
            if (!c.type.hasOutputPort()) continue;
            int value = outputOf(c, now);
            Integer last = sentOutputs.get(c.id());
            if (last == null || last != value || refresh) {
                if (c.type == PanelComponentType.HOLD_BUTTON && last != null && last > 0 && value == 0) {
                    String who = holders.remove(c.id());
                    signal("panel_hold", c.id(), false, who == null ? "" : who, c.x + 1, c.y + 1);
                    playSound(ModSounds.BUTTON_RELEASE.get(), 1.0f);
                    syncPending = true;
                } else if (c.type == PanelComponentType.BUTTON && last != null && last > 0 && value == 0) {
                    syncPending = true;
                }
                sentOutputs.put(c.id(), value);
                Signals.propagate(level, this, c.id());
            }
        }
        if (refresh && Signals.pruneDeadLinks(level, this)) syncPending = true;

        if (syncPending) {
            syncPending = false;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private int outputOf(PanelComponent c, long now) {
        return switch (c.type) {
            case BUTTON, HOLD_BUTTON -> now < c.pressedUntil ? Signals.ON : 0;
            case SWITCH -> c.state ? Signals.ON : 0;
            default -> 0;
        };
    }

    // ------------------------------------------------------------------ ISignalNode

    @Override
    public @Nullable PortRef pickPort(Player player, Vec3 hitLocation) {
        BlockPos p = getBlockPos();
        Vec3 eye = player.getEyePosition().subtract(p.getX(), p.getY(), p.getZ());
        double[] uv = orientation().raycast(eye, player.getViewVector(1f), true);
        if (uv == null) return null;
        int[] cell = PanelOrientation.cellOf(uv[0], uv[1]);
        if (cell == null) return null;
        PanelComponent c = layout.at(cell[0], cell[1]);
        if (c == null) return null;
        if (c.type.hasOutputPort()) return new PortRef(c.id(), true);
        if (c.type.hasInputPort()) return new PortRef(c.id(), false);
        return null;
    }

    @Override
    public boolean hasPort(int port, boolean output) {
        PanelComponent c = layout.byId(port);
        return c != null && (output ? c.type.hasOutputPort() : c.type.hasInputPort());
    }

    @Override
    public Vec3 portPosition(int port, boolean output) {
        PanelComponent c = layout.byId(port);
        BlockPos p = getBlockPos();
        double cu = 0.5, cv = 0.5;
        if (c != null) {
            cu = PanelOrientation.MARGIN + (c.x + c.width / 2.0) * PanelOrientation.CELL;
            cv = PanelOrientation.MARGIN + (c.y + c.height / 2.0) * PanelOrientation.CELL;
        }
        return orientation().toBlock(cu, cv, -PanelOrientation.THICKNESS - 0.02).add(p.getX(), p.getY(), p.getZ());
    }

    @Override
    public int getOutput(int port) {
        PanelComponent c = layout.byId(port);
        return c == null ? 0 : outputOf(c, gameTime());
    }

    @Override
    public void setInput(int port, int value) {
        PanelComponent c = layout.byId(port);
        if (c != null && c.type.hasInputPort() && c.input != value) {
            c.input = value;
            markDirtyAndSync();
        }
    }

    @Override
    public List<SignalLink> links() {
        return links;
    }

    @Override
    public @Nullable SignalSource sourceOf(int inPort) {
        return sources.get(inPort);
    }

    @Override
    public void setSource(int inPort, @Nullable SignalSource source) {
        if (source == null) sources.remove(inPort);
        else sources.put(inPort, source);
    }

    @Override
    public void markSignalDirty() {
        markDirtyAndSync();
    }

    @Override
    public String describePort(PortRef ref) {
        return (ref.output() ? "OUT #" : "IN #") + ref.port();
    }

    /** Called by the block when it is broken: unplug wires that end here. */
    public int unplugIncoming() {
        if (level == null) return 0;
        int n = 0;
        for (Integer port : new ArrayList<>(sources.keySet())) {
            if (Signals.disconnectInput(level, getBlockPos(), port)) n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ Lua API

    private PanelComponent component(Arguments args, int index) {
        PanelComponent c = layout.byId(args.checkInteger(index));
        if (c == null) throw new IllegalArgumentException("no such component");
        return c;
    }

    private static PanelComponent require(PanelComponent c, PanelComponentType... types) {
        for (PanelComponentType t : types) if (c.type == t) return c;
        throw new IllegalArgumentException("not supported by " + c.type.id());
    }

    @Callback(direct = true, doc = "function():table -- Returns a list of all components: {id, type, x, y, width, height, color, label, state, value, input, pressed}.")
    public Object[] getComponents(Context context, Arguments args) {
        long t = gameTime();
        List<Map<String, Object>> list = new ArrayList<>();
        for (PanelComponent c : layout.snapshot()) list.add(c.describe(t));
        return new Object[]{list};
    }

    @Callback(direct = true, doc = "function(id:number):table -- Returns one component.")
    public Object[] getComponent(Context context, Arguments args) {
        PanelComponent c = layout.byId(args.checkInteger(0));
        return c == null ? new Object[]{null, "no such component"} : new Object[]{c.describe(gameTime())};
    }

    @Callback(direct = true, doc = "function(x:number, y:number):number -- Returns the id of the component covering grid cell (x, y), 1-based.")
    public Object[] findComponent(Context context, Arguments args) {
        PanelComponent c = layout.at(args.checkInteger(0) - 1, args.checkInteger(1) - 1);
        return c == null ? new Object[]{null} : new Object[]{c.id()};
    }

    @Callback(direct = true, doc = "function(id:number):boolean -- Lamp lit / switch on / button currently pressed.")
    public Object[] getState(Context context, Arguments args) {
        PanelComponent c = component(args, 0);
        if (c.type.isButton()) return new Object[]{gameTime() < c.pressedUntil};
        if (c.type == PanelComponentType.LAMP) return new Object[]{c.lit()};
        return new Object[]{require(c, PanelComponentType.SWITCH).state};
    }

    @Callback(direct = true, limit = 64, doc = "function(id:number, state:boolean) -- Turns a lamp on/off or flips a switch.")
    public Object[] setState(Context context, Arguments args) {
        PanelComponent c = require(component(args, 0), PanelComponentType.LAMP, PanelComponentType.SWITCH);
        boolean s = args.checkBoolean(1);
        if (c.state != s) {
            c.state = s;
            markDirtyAndSync();
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(id:number):number -- Returns the RGB color.")
    public Object[] getColor(Context context, Arguments args) {
        return new Object[]{component(args, 0).color};
    }

    @Callback(direct = true, limit = 64, doc = "function(id:number, color:number) -- Sets the RGB color (0xRRGGBB).")
    public Object[] setColor(Context context, Arguments args) {
        PanelComponent c = component(args, 0);
        int color = args.checkInteger(1) & 0xFFFFFF;
        if (c.color != color) {
            c.color = color;
            markDirtyAndSync();
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(id:number):string -- Returns the label text.")
    public Object[] getLabel(Context context, Arguments args) {
        PanelComponent c = component(args, 0);
        if (!c.type.hasLabel()) throw new IllegalArgumentException("not supported by " + c.type.id());
        return new Object[]{c.label};
    }

    @Callback(direct = true, limit = 32, doc = "function(id:number, text:string) -- Sets the label text (use \\n for line breaks).")
    public Object[] setLabel(Context context, Arguments args) {
        PanelComponent c = component(args, 0);
        if (!c.type.hasLabel()) throw new IllegalArgumentException("not supported by " + c.type.id());
        String s = args.checkString(1).replace("§", "");
        if (s.length() > PanelComponent.MAX_LABEL) s = s.substring(0, PanelComponent.MAX_LABEL);
        if (!s.equals(c.label)) {
            c.label = s;
            markDirtyAndSync();
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(id:number):string -- Returns the text shown on a seven-segment display.")
    public Object[] getValue(Context context, Arguments args) {
        PanelComponent c = require(component(args, 0), PanelComponentType.SEGMENT);
        return new Object[]{c.shownValue(isWired(c.id()))};
    }

    @Callback(direct = true, limit = 64, doc = "function(id:number, value:number or string) -- Shows a value on a seven-segment display (0-9, A-F, -, space, '.'). Ignored while a wire drives the display.")
    public Object[] setValue(Context context, Arguments args) {
        PanelComponent c = require(component(args, 0), PanelComponentType.SEGMENT);
        String s;
        if (args.isInteger(1) || args.isDouble(1)) {
            double d = args.checkDouble(1);
            s = d == Math.rint(d) && Math.abs(d) < 1e15 ? Long.toString((long) d) : Double.toString(d);
        } else {
            s = args.checkString(1);
        }
        if (s.length() > PanelComponent.MAX_VALUE) s = s.substring(0, PanelComponent.MAX_VALUE);
        if (!s.equals(c.value)) {
            c.value = s;
            markDirtyAndSync();
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(id:number):number -- Signal arriving on a lamp/display input port (0 if not wired).")
    public Object[] getInput(Context context, Arguments args) {
        return new Object[]{require(component(args, 0), PanelComponentType.LAMP, PanelComponentType.SEGMENT).input};
    }

    @Callback(doc = "function(id:number):boolean -- Presses a button or flips a switch as if a player did it (fires the signal).")
    public Object[] press(Context context, Arguments args) {
        return new Object[]{activate(component(args, 0), "")};
    }

    @Callback(direct = true, doc = "function() -- Turns off all lamps and blanks all displays.")
    public Object[] reset(Context context, Arguments args) {
        for (PanelComponent c : layout.snapshot()) {
            if (c.type == PanelComponentType.LAMP) c.state = false;
            if (c.type == PanelComponentType.SEGMENT) c.value = "";
        }
        markDirtyAndSync();
        return new Object[]{true};
    }

    // Aliases for scripts written against 2.0.0-beta.1 (slot numbers == component ids).
    @Callback(direct = true, doc = "function(id):boolean -- Deprecated alias of getState.")
    public Object[] getSwitch(Context c, Arguments a) { return getState(c, a); }

    @Callback(direct = true, limit = 64, doc = "function(id, state) -- Deprecated alias of setState.")
    public Object[] setSwitch(Context c, Arguments a) { return setState(c, a); }

    @Callback(direct = true, doc = "function(id):string -- Deprecated alias of getValue.")
    public Object[] getSegment(Context c, Arguments a) { return getValue(c, a); }

    @Callback(direct = true, limit = 64, doc = "function(id, value) -- Deprecated alias of setValue.")
    public Object[] setSegment(Context c, Arguments a) { return setValue(c, a); }

    @Callback(direct = true, doc = "function(id):string -- Deprecated alias of getLabel.")
    public Object[] getText(Context c, Arguments a) { return getLabel(c, a); }

    @Callback(direct = true, limit = 32, doc = "function(id, text) -- Deprecated alias of setLabel.")
    public Object[] setText(Context c, Arguments a) { return setLabel(c, a); }

    @Callback(direct = true, doc = "function():table -- Deprecated alias of getComponents.")
    public Object[] getElements(Context c, Arguments a) { return getComponents(c, a); }

    // ------------------------------------------------------------------ persistence & sync

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeShared(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readShared(tag);
    }

    private void writeShared(CompoundTag tag) {
        tag.put("components", layout.save());
        tag.putByte("offset", (byte) offset);
        tag.putByte("tilt", (byte) tilt);
        ListTag l = new ListTag();
        for (SignalLink link : links) l.add(link.save());
        tag.put("links", l);
        ListTag s = new ListTag();
        for (Map.Entry<Integer, SignalSource> e : sources.entrySet()) {
            CompoundTag t = e.getValue().save();
            t.putInt("port", e.getKey());
            s.add(t);
        }
        tag.put("sources", s);
    }

    private void readShared(CompoundTag tag) {
        if (tag.contains("components", Tag.TAG_LIST)) {
            layout.load(tag.getList("components", Tag.TAG_COMPOUND));
        } else if (tag.contains("elements", Tag.TAG_LIST)) {
            layout.loadLegacy(tag.getList("elements", Tag.TAG_COMPOUND));
        }
        offset = tag.getByte("offset");
        tilt = tag.getByte("tilt");
        orientationCache = null;
        links.clear();
        for (Tag t : tag.getList("links", Tag.TAG_COMPOUND)) links.add(SignalLink.load((CompoundTag) t));
        sources.clear();
        for (Tag t : tag.getList("sources", Tag.TAG_COMPOUND)) {
            sources.put(((CompoundTag) t).getInt("port"), SignalSource.load((CompoundTag) t));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeShared(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readShared(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        readShared(pkt.getTag());
    }
}
