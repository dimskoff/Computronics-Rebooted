package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.init.ModSounds;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelComponentType;
import pl.asie.computronics.panel.PanelLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Control panel block entity: owns a {@link PanelLayout}, exposes it to OpenComputers as the
 * {@code control_panel} component and fires {@code panel_button} / {@code panel_switch} signals.
 * <p>
 * Changes (from players or Lua) only mark the panel dirty; the server tick pushes at most one
 * block-entity update per tick to clients.
 */
public class TileControlPanel extends TileEntityComputronics {
    public static final int PRESS_TICKS = 6;

    private final PanelLayout layout = new PanelLayout();
    private volatile boolean syncPending;

    public TileControlPanel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROL_PANEL.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("control_panel", Visibility.Network)
                .create();
    }

    public PanelLayout layout() {
        return layout;
    }

    // ------------------------------------------------------------------ server-side operations

    /** Mounts a component from an item. Returns the component or null if it doesn't fit. */
    public PanelComponent mount(PanelComponentType type, int cx, int cy, int size) {
        int[] fp = type.footprint(size);
        PanelComponent c = layout.place(type, cx, cy, fp[0], fp[1]);
        if (c != null) {
            playSound(SoundEvents.STONE_PLACE, 1.4f);
            markDirtyAndSync();
        }
        return c;
    }

    /** Removes a component and returns the item it was made from. */
    public ItemStack dismount(PanelComponent c) {
        if (!layout.remove(c)) return ItemStack.EMPTY;
        playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.1f);
        markDirtyAndSync();
        return new ItemStack(c.type.item());
    }

    public List<ItemStack> dismountAll() {
        List<ItemStack> drops = new ArrayList<>();
        for (PanelComponent c : layout.clear()) drops.add(new ItemStack(c.type.item()));
        markDirtyAndSync();
        return drops;
    }

    /** A player (or Lua via press()) activates a component. */
    public boolean activate(PanelComponent c, String who) {
        switch (c.type) {
            case BUTTON -> {
                c.pressedUntil = gameTime() + PRESS_TICKS;
                playSound(ModSounds.BUTTON_PRESS.get(), 1.0f);
                signal("panel_button", c.id(), who, c.x + 1, c.y + 1);
            }
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

    /** Applies settings from the screwdriver GUI (already validated by the caller). */
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

    private void playSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
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
        if (syncPending) {
            syncPending = false;
            setChanged();
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
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

    @Callback(direct = true, doc = "function():table -- Returns a list of all components: {id, type, x, y, width, height, color, label, state, value, pressed}.")
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
        if (c.type == PanelComponentType.BUTTON) return new Object[]{gameTime() < c.pressedUntil};
        return new Object[]{require(c, PanelComponentType.LAMP, PanelComponentType.SWITCH).state};
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
        return new Object[]{require(component(args, 0), PanelComponentType.BUTTON, PanelComponentType.SWITCH, PanelComponentType.LAMP, PanelComponentType.LABEL).label};
    }

    @Callback(direct = true, limit = 32, doc = "function(id:number, text:string) -- Sets the label text (use \\n for line breaks).")
    public Object[] setLabel(Context context, Arguments args) {
        PanelComponent c = require(component(args, 0), PanelComponentType.BUTTON, PanelComponentType.SWITCH, PanelComponentType.LAMP, PanelComponentType.LABEL);
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
        return new Object[]{require(component(args, 0), PanelComponentType.SEGMENT).value};
    }

    @Callback(direct = true, limit = 64, doc = "function(id:number, value:number or string) -- Shows a value on a seven-segment display (0-9, A-F, -, space, '.').")
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
        tag.put("components", layout.save());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readLayout(tag);
    }

    private void readLayout(CompoundTag tag) {
        if (tag.contains("components", Tag.TAG_LIST)) {
            layout.load(tag.getList("components", Tag.TAG_COMPOUND));
        } else if (tag.contains("elements", Tag.TAG_LIST)) {
            layout.loadLegacy(tag.getList("elements", Tag.TAG_COMPOUND));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("components", layout.save());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readLayout(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        readLayout(pkt.getTag());
    }
}
